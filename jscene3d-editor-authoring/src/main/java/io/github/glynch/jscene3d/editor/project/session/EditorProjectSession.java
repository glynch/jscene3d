/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.configuration.definition.SettingKey;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.session.internal.AuthoringChangeSource;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewDiagnosticCode;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjector;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.composition.CompositionPlanResult;
import io.github.glynch.jscene3d.project.composition.CompositionPlanner;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Central facade for one loaded JScene3D authoring project and its per-definition working copies. */
public final class EditorProjectSession implements AutoCloseable {
    private final GameProject project;
    private final ProjectConfiguration configuration;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final DefinitionResolver definitions;
    private final List<ProjectAsset> assets;
    private final Collection<ProjectDiagnostic> diagnostics;
    private final RetainedDefinitionRegistry retainedDefinitions;
    private final DefinitionAuthoringOperations authoringOperations;
    private final InspectorProjectionService inspectorProjection;
    private final AuthoringChangeSource<AuthoringSessionChange> changes = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<EditorHierarchyProjection> hierarchyChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<AuthoringDefinitionChange> definitionChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<AuthoringConfigurationChange> configurationChanges =
            new AuthoringChangeSource<>();
    private Optional<EditorHierarchyProjection> activeHierarchy = Optional.empty();
    private long configurationRevision;
    private boolean closed;

    /**
     * Stores validated project data without implicitly opening any Scene.
     *
     * @param source loaded project inputs
     * @param assets validated authored assets
     * @param hierarchyProjector projector used after authored edits
     * @param diagnostics live ordered diagnostic collection owned by the load/session boundary
     */
    public EditorProjectSession(
            Source source,
            List<ProjectAsset> assets,
            EditorHierarchyProjector hierarchyProjector,
            Collection<ProjectDiagnostic> diagnostics) {
        Source validSource = Objects.requireNonNull(source, "source");
        project = validSource.project();
        configuration = validSource.configuration();
        authoredAssets = validSource.authoredAssets();
        types = validSource.types();
        definitions = validSource.definitions();
        this.assets = List.copyOf(assets);
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        retainedDefinitions = new RetainedDefinitionRegistry(
                authoredAssets, definitions, types, hierarchyProjector, this::workingCopyChanged);
        MutationTargetResolver mutationTargets = new MutationTargetResolver(retainedDefinitions);
        authoringOperations =
                new DefinitionAuthoringOperations(project.identity().id(), retainedDefinitions, mutationTargets);
        inspectorProjection = new InspectorProjectionService(retainedDefinitions, definitions, types, this.assets);
    }

    /** Returns the validated project descriptor.
     *
     * @return validated project descriptor
     */
    public GameProject project() {
        ensureOpen();
        return project;
    }

    /** Returns portable settings applied to this project session.
     *
     * @return portable project settings
     */
    public ProjectSettings settings() {
        ensureOpen();
        return configuration.document();
    }

    /** Returns the effective project configuration and declarative setting registry.
     *
     * @return live project configuration
     */
    public ProjectConfiguration configuration() {
        ensureOpen();
        return configuration;
    }

    /** Returns authored definition metadata.
     *
     * @return authored asset catalog
     */
    public AssetCatalog authoredAssets() {
        ensureOpen();
        return authoredAssets;
    }

    /** Returns safe component and resource metadata.
     *
     * @return registered type catalog
     */
    public RegisteredTypeCatalog types() {
        ensureOpen();
        return types;
    }

    /** Returns the combined authored and generated definition resolver.
     *
     * @return definition resolver
     */
    public DefinitionResolver definitions() {
        ensureOpen();
        return definitions;
    }

    /**
     * Returns current structured project and authoring diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        ensureOpen();
        return List.copyOf(diagnostics);
    }

    /** Returns asset projections in deterministic order.
     *
     * @return immutable authored assets
     */
    public List<ProjectAsset> assets() {
        ensureOpen();
        return assets;
    }

    /**
     * Resolves and retains one structural definition by authoritative asset identity.
     *
     * <p>Every authored definition reuses one session-owned authoritative working copy. Generated definitions remain
     * read-only and retain their immutable logical published source.
     *
     * @param id structural definition asset identity
     * @return retained snapshot or ordered loading diagnostics
     */
    public DefinitionRetentionResult retainDefinition(AssetId id) {
        ensureOpen();
        DefinitionRetentionResult result = retainedDefinitions.retain(id);
        diagnostics.addAll(result.diagnostics());
        result.definition().ifPresent(definition -> {
            activeHierarchy = Optional.of(definition.hierarchy());
            hierarchyChanges.emit(definition.hierarchy());
        });
        return result;
    }

    /**
     * Returns retained identities in first-retention order.
     *
     * @return immutable retained definition identities
     */
    public List<AssetId> retainedDefinitionIds() {
        ensureOpen();
        return retainedDefinitions.retainedIds();
    }

    /** Returns the session-owned source-preserving state after an authored definition has been retained. */
    Optional<AuthoredDefinitionDocument> retainedAuthoredDocument(AssetId id) {
        ensureOpen();
        return retainedDefinitions.authoredDocument(id);
    }

    /** Returns the retained authoritative working-copy instance for focused session tests. */
    Optional<AuthoredDefinitionWorkingCopy> retainedAuthoredWorkingCopy(AssetId id) {
        ensureOpen();
        return retainedDefinitions.workingCopy(id);
    }

    /** Returns current lifecycle state for one open authored definition.
     *
     * @param id authored definition identity
     * @return current state, or empty for an unopened or generated definition
     */
    public Optional<AuthoringDefinitionState> definitionState(AssetId id) {
        ensureOpen();
        return retainedDefinitions.definitionState(id);
    }

    /**
     * Projects the current in-memory revision of one retained Scene for a future editor Scene View.
     *
     * <p>The root Scene comes directly from its authoritative working copy, including accepted unsaved changes. The
     * established definition resolver remains responsible for referenced authored and generated entity definitions.
     * The projection uses only safe descriptors and never creates runtime components, project classes, renderer
     * objects, or resources.
     *
     * @param scene retained Scene asset identity
     * @param expectedRevision Scene revision observed by the caller
     * @return current projection, stale/unavailable outcome, or structured planning diagnostics
     */
    public SceneViewProjectionResult projectSceneView(AssetId scene, long expectedRevision) {
        ensureOpen();
        AssetId validScene = Objects.requireNonNull(scene, "scene");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must be non-negative");
        }
        Optional<EditorRetainedDefinition> retainedDefinition = retainedDefinitions.snapshot(validScene);
        if (retainedDefinition.isEmpty() || retainedDefinition.orElseThrow().kind() != AssetKind.SCENE_DEFINITION) {
            return new SceneViewProjectionResult(
                    SceneViewProjectionResult.Outcome.SCENE_UNAVAILABLE,
                    validScene,
                    OptionalLong.empty(),
                    Optional.empty(),
                    List.of());
        }
        EditorRetainedDefinition retained = retainedDefinition.orElseThrow();
        long currentRevision = retained.revision();
        if (currentRevision != expectedRevision) {
            return new SceneViewProjectionResult(
                    SceneViewProjectionResult.Outcome.STALE_REVISION,
                    validScene,
                    OptionalLong.of(currentRevision),
                    Optional.empty(),
                    List.of());
        }
        EditorRetainedDefinition.Content.Scene content = (EditorRetainedDefinition.Content.Scene) retained.content();
        CompositionPlanResult plan =
                CompositionPlanner.plan(retained.source(), content.definition(), definitions, types);
        if (plan.plan().isEmpty()) {
            return new SceneViewProjectionResult(
                    SceneViewProjectionResult.Outcome.PLANNING_FAILED,
                    validScene,
                    OptionalLong.of(currentRevision),
                    Optional.empty(),
                    plan.diagnostics());
        }
        return projectSceneView(validScene, retained.source(), currentRevision, plan);
    }

    /** Returns whether any authored definition differs from its persisted semantic baseline.
     *
     * @return whether the project session contains dirty authored state
     */
    public boolean isDirty() {
        ensureOpen();
        return retainedDefinitions.isDirty();
    }

    /** Returns metadata for the next undo operation of one retained authored definition.
     *
     * @param id authored definition identity
     * @return next undo operation when available
     */
    public Optional<AuthoringOperation> undoOperation(AssetId id) {
        ensureOpen();
        return authoringOperations.undoOperation(id);
    }

    /** Restores the previous accepted state of one authored definition.
     *
     * @param id retained definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return accepted, stale, non-editable, invalid, or unavailable outcome
     */
    public AuthoringMutationResult undo(AssetId id, long expectedRevision) {
        ensureOpen();
        return authoringOperations.undo(id, expectedRevision);
    }

    /** Reapplies the next previously undone state of one authored definition.
     *
     * @param id retained definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return accepted, stale, non-editable, invalid, or unavailable outcome
     */
    public AuthoringMutationResult redo(AssetId id, long expectedRevision) {
        ensureOpen();
        return authoringOperations.redo(id, expectedRevision);
    }

    /**
     * Atomically saves one authored definition after persisted-fingerprint conflict verification.
     *
     * @param id authoritative definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return structured save, no-op, conflict, write, concurrency, or editability outcome
     */
    public AuthoringPersistenceResult saveDefinition(AssetId id, long expectedRevision) {
        ensureOpen();
        return authoringOperations.save(id, expectedRevision);
    }

    /**
     * Reloads and validates one authored definition from its current trusted physical source.
     *
     * @param id authoritative definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return structured revert, no-op, validation, source, concurrency, or editability outcome
     */
    public AuthoringPersistenceResult revertDefinition(AssetId id, long expectedRevision) {
        ensureOpen();
        return authoringOperations.revert(id, expectedRevision);
    }

    /**
     * Captures deterministic recovery state for one authored definition without writing its source.
     *
     * @param id authoritative definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return structured backup, concurrency, target, or editability outcome
     */
    public AuthoringBackupResult backupDefinition(AssetId id, long expectedRevision) {
        ensureOpen();
        return authoringOperations.backup(id, expectedRevision);
    }

    /**
     * Restores deterministic recovery bytes after strict parsing, identity checks, and authoritative validation.
     *
     * @param id authoritative target definition identity
     * @param expectedRevision new-session definition revision observed by the caller
     * @param encodedBackup untrusted complete backup JSON bytes
     * @return structured restore, conflict, validation, concurrency, target, or editability outcome
     */
    public AuthoringRestoreResult restoreDefinition(AssetId id, long expectedRevision, byte[] encodedBackup) {
        ensureOpen();
        return authoringOperations.restore(id, expectedRevision, encodedBackup);
    }

    /** Establishes a future successfully persisted authored document as the in-memory baseline. */
    AuthoringMutationResult markPersisted(
            AssetId id, AuthoredDefinitionDocument baselinedDocument, long expectedRevision) {
        ensureOpen();
        return authoringOperations.markPersisted(id, baselinedDocument, expectedRevision);
    }

    /** Returns the hierarchy projection for the explicitly opened active definition.
     *
     * @return current definition context and hierarchy roots, when a definition is active
     */
    public Optional<EditorHierarchyProjection> activeHierarchy() {
        ensureOpen();
        return activeHierarchy;
    }

    /**
     * Projects one Java-issued semantic target against the current retained definition revision.
     *
     * @param target semantic target previously returned by the current hierarchy
     * @param expectedRevision definition revision observed by the caller
     * @return complete immutable Inspector snapshot
     * @throws IllegalStateException if the revision, retained definition, or occurrence is stale
     * @throws IllegalArgumentException if the supplied target does not match current Java state
     */
    public InspectorProjection inspect(InspectorTarget target, long expectedRevision) {
        ensureOpen();
        return inspectorProjection.inspect(target, expectedRevision);
    }

    /** Returns all authoritative session state changes.
     *
     * @return session change stream
     */
    public AuthoringChange<AuthoringSessionChange> onDidChange() {
        ensureOpen();
        return changes;
    }

    /** Returns hierarchy projection changes.
     *
     * @return hierarchy change stream
     */
    public AuthoringChange<EditorHierarchyProjection> onDidChangeHierarchy() {
        ensureOpen();
        return hierarchyChanges;
    }

    /** Returns coherent authored-definition state changes.
     *
     * @return definition change stream
     */
    public AuthoringChange<AuthoringDefinitionChange> onDidChangeDefinition() {
        ensureOpen();
        return definitionChanges;
    }

    /** Returns persisted project-setting changes.
     *
     * @return configuration change stream
     */
    public AuthoringChange<AuthoringConfigurationChange> onDidChangeConfiguration() {
        ensureOpen();
        return configurationChanges;
    }

    /**
     * Persists one declared project setting and advances the authoring revision.
     *
     * @param <T> setting value type
     * @param key declared setting key
     * @param value replacement value
     * @throws IOException when the updated settings cannot be persisted
     */
    public <T> void updateSetting(SettingKey<T> key, T value) throws IOException {
        ensureOpen();
        configuration.update(key, value);
        configurationRevision++;
        configurationChanges.emit(new AuthoringConfigurationChange(key, true, configurationRevision));
        changes.emit(new AuthoringSessionChange(
                configurationRevision, isDirty(), AuthoringSessionChange.Kind.CONFIGURATION));
    }

    /**
     * Removes one project override and advances the authoring revision.
     *
     * @param key declared setting key
     * @throws IOException when the updated settings cannot be persisted
     */
    public void resetSetting(SettingKey<?> key) throws IOException {
        ensureOpen();
        configuration.reset(key);
        configurationRevision++;
        configurationChanges.emit(new AuthoringConfigurationChange(key, false, configurationRevision));
        changes.emit(new AuthoringSessionChange(
                configurationRevision, isDirty(), AuthoringSessionChange.Kind.CONFIGURATION));
    }

    /**
     * Applies one SET or REMOVE mutation to a retained authored definition.
     *
     * @param definition authoritative definition identity
     * @param target stable semantic mutation target
     * @param mutation discriminated SET or REMOVE operation
     * @param expectedRevision definition revision observed by the caller
     * @return structured accepted, no-op, validation, conflict, target, or editability outcome
     */
    public AuthoringMutationResult mutate(
            AssetId definition, InspectorMutationTarget target, AuthoringMutation mutation, long expectedRevision) {
        ensureOpen();
        return authoringOperations.mutate(definition, target, mutation, expectedRevision);
    }

    /**
     * Applies one phase of a Java-authoritative interactive component-property edit.
     *
     * @param definition authoritative definition identity
     * @param target stable component-property target
     * @param phase interactive lifecycle phase
     * @param editId client-generated identity retained for the complete drag
     * @param value preview value, required only for {@link AuthoringInteractivePhase#UPDATE}
     * @param expectedRevision definition revision observed by the caller
     * @return authoritative edit state
     */
    public AuthoringMutationResult interact(
            AssetId definition,
            InspectorMutationTarget.ComponentProperty target,
            AuthoringInteractivePhase phase,
            String editId,
            Optional<ProjectValue> value,
            long expectedRevision) {
        ensureOpen();
        return authoringOperations.interact(definition, target, phase, editId, value, expectedRevision);
    }

    /**
     * Rejects a syntactically invalid mutation candidate after applying the normal identity and revision checks.
     *
     * <p>Protocol adapters use this when an exact numeric literal cannot be represented as a {@link ProjectValue}.
     * The working copy remains authoritative for the unchanged revision, dirty state, and history availability.
     *
     * @param definition authoritative definition identity
     * @param target stable semantic mutation target
     * @param expectedRevision definition revision observed by the caller
     * @param diagnostics ordered candidate-validation diagnostics
     * @return validation rejection, stale revision, invalid target, or editability outcome
     */
    public AuthoringMutationResult rejectMutation(
            AssetId definition,
            InspectorMutationTarget target,
            long expectedRevision,
            List<ProjectDiagnostic> diagnostics) {
        ensureOpen();
        return authoringOperations.reject(definition, target, expectedRevision, diagnostics);
    }

    /** Returns whether this session has released its subscriptions and working copy.
     *
     * @return whether the session is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases project-scoped subscriptions without saving dirty content. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        retainedDefinitions.close();
        changes.close();
        hierarchyChanges.close();
        definitionChanges.close();
        configurationChanges.close();
    }

    /** Refreshes current projections and forwards one coherent working-copy notification. */
    private void workingCopyChanged(AuthoringDefinitionChange change) {
        Optional<EditorRetainedDefinition> retained = retainedDefinitions.snapshot(change.definition());
        if (retained.isPresent()
                && activeHierarchy
                        .map(EditorHierarchyProjection::context)
                        .map(EditorHierarchyProjection.Context::definitionId)
                        .filter(change.definition()::equals)
                        .isPresent()) {
            EditorHierarchyProjection refreshed = retained.orElseThrow().hierarchy();
            activeHierarchy = Optional.of(refreshed);
            hierarchyChanges.emit(refreshed);
        }
        definitionChanges.emit(change);
    }

    /** Converts one coherent plan into a snapshot while containing unexpected projection failures. */
    private static SceneViewProjectionResult projectSceneView(
            AssetId scene, URI source, long revision, CompositionPlanResult plan) {
        try {
            SceneViewSnapshot projected = SceneViewProjector.project(plan.plan().orElseThrow(), revision);
            return new SceneViewProjectionResult(
                    SceneViewProjectionResult.Outcome.PROJECTED,
                    scene,
                    OptionalLong.of(revision),
                    Optional.of(projected),
                    List.of());
        } catch (RuntimeException exception) {
            String detail =
                    exception.getMessage() == null ? "The visual projection was inconsistent" : exception.getMessage();
            ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                    ProjectDiagnostic.Severity.ERROR,
                    SceneViewDiagnosticCode.PROJECTION_FAILED,
                    source,
                    "",
                    Map.of("technicalDetail", detail));
            return new SceneViewProjectionResult(
                    SceneViewProjectionResult.Outcome.PROJECTION_FAILED,
                    scene,
                    OptionalLong.of(revision),
                    Optional.empty(),
                    List.of(diagnostic));
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Editor project session is closed");
        }
    }

    /**
     * Groups immutable loaded inputs from which a project session is opened.
     *
     * @param project validated project descriptor
     * @param configuration effective project configuration
     * @param authoredAssets authored asset catalog
     * @param types registered project type catalog
     * @param definitions authored and published definition resolver
     */
    public record Source(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authoredAssets,
            RegisteredTypeCatalog types,
            DefinitionResolver definitions) {
        /** Validates the complete loaded source. */
        public Source {
            Objects.requireNonNull(project, "project");
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(authoredAssets, "authoredAssets");
            Objects.requireNonNull(types, "types");
            Objects.requireNonNull(definitions, "definitions");
        }
    }
}
