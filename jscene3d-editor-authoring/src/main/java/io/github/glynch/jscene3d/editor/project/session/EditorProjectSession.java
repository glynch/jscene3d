/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.configuration.definition.SettingKey;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.session.internal.AuthoringChangeSource;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProjector;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Central facade for one loaded JScene3D authoring project and its per-definition working copies. */
public final class EditorProjectSession implements AutoCloseable {
    private final GameProject project;
    private final ProjectConfiguration configuration;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final DefinitionResolver definitions;
    private final List<ProjectAsset> assets;
    private final Collection<ProjectDiagnostic> diagnostics;
    private final EditorHierarchyProjector hierarchyProjector;
    private final AssetId startupWorldId;
    private final Path startupWorldSource;
    private final AuthoringChangeSource<AuthoringSessionChange> changes = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<EditorHierarchyProjection> hierarchyChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<AuthoringDefinitionChange> definitionChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<AuthoringConfigurationChange> configurationChanges =
            new AuthoringChangeSource<>();
    private final Map<AssetId, AuthoredDefinitionWorkingCopy> workingCopies = new LinkedHashMap<>();
    private final List<AuthoringSubscription> workingCopySubscriptions = new ArrayList<>();
    private final Map<AssetId, RetainedState> retainedDefinitions = new LinkedHashMap<>();

    private EditorHierarchyProjection hierarchy;
    private long configurationRevision;
    private boolean closed;

    /**
     * Stores validated project data and opens the authoritative startup-world working copy.
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
        this.hierarchyProjector = Objects.requireNonNull(hierarchyProjector, "hierarchyProjector");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        startupWorldId = validSource.startupWorld().id();
        startupWorldSource = validSource.startupWorldSource().toAbsolutePath().normalize();
        AuthoredDefinitionDocument startupDocument = loadAuthoredDocument(startupWorldId);
        openWorkingCopy(startupDocument);
        hierarchy = projectHierarchy();
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

    /** Returns the current startup-world content.
     *
     * @return current world definition
     */
    public WorldDefinition startupWorld() {
        ensureOpen();
        return ((EditorRetainedDefinition.Content.World)
                        startupWorkingCopy().state().content())
                .definition();
    }

    /** Returns the normalized authored startup-world source.
     *
     * @return authored world source
     */
    public Path startupWorldSource() {
        ensureOpen();
        return startupWorldSource;
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
        AssetId validId = Objects.requireNonNull(id, "id");
        RetainedState existing = retainedDefinitions.get(validId);
        if (existing != null) {
            return new DefinitionRetentionResult(Optional.of(snapshot(existing)), List.of());
        }

        Optional<AssetMetadata> authored = authoredAssets.find(validId);
        DefinitionRetentionResult result =
                authored.isPresent() ? retainAuthored(authored.orElseThrow()) : retainGeneratedEntity(validId);
        diagnostics.addAll(result.diagnostics());
        return result;
    }

    /**
     * Returns retained identities in first-retention order.
     *
     * @return immutable retained definition identities
     */
    public List<AssetId> retainedDefinitionIds() {
        ensureOpen();
        return List.copyOf(retainedDefinitions.keySet());
    }

    /** Returns the session-owned source-preserving state after an authored definition has been retained. */
    Optional<AuthoredDefinitionDocument> retainedAuthoredDocument(AssetId id) {
        ensureOpen();
        RetainedState retained = retainedDefinitions.get(Objects.requireNonNull(id, "id"));
        return retained instanceof AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy)
                ? Optional.of(workingCopy.current())
                : Optional.empty();
    }

    /** Returns the retained authoritative working-copy instance for focused session tests. */
    Optional<AuthoredDefinitionWorkingCopy> retainedAuthoredWorkingCopy(AssetId id) {
        ensureOpen();
        RetainedState retained = retainedDefinitions.get(Objects.requireNonNull(id, "id"));
        return retained instanceof AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy)
                ? Optional.of(workingCopy)
                : Optional.empty();
    }

    /** Returns current lifecycle state for one open authored definition.
     *
     * @param id authored definition identity
     * @return current state, or empty for an unopened or generated definition
     */
    public Optional<AuthoringDefinitionState> definitionState(AssetId id) {
        ensureOpen();
        AuthoredDefinitionWorkingCopy workingCopy = workingCopies.get(Objects.requireNonNull(id, "id"));
        return workingCopy == null ? Optional.empty() : Optional.of(workingCopy.state());
    }

    /** Returns whether any authored definition differs from its persisted semantic baseline.
     *
     * @return whether the project session contains dirty authored state
     */
    public boolean isDirty() {
        ensureOpen();
        return workingCopies.values().stream().anyMatch(AuthoredDefinitionWorkingCopy::isDirty);
    }

    /** Returns metadata for the next undo operation of one retained authored definition.
     *
     * @param id authored definition identity
     * @return next undo operation when available
     */
    public Optional<AuthoringOperation> undoOperation(AssetId id) {
        ensureOpen();
        return authoredWorkingCopy(id).flatMap(AuthoredDefinitionWorkingCopy::undoOperation);
    }

    /** Restores the previous accepted state of one authored definition.
     *
     * @param id retained definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return accepted, stale, non-editable, invalid, or unavailable outcome
     */
    public AuthoringMutationResult undo(AssetId id, long expectedRevision) {
        ensureOpen();
        return editableWorkingCopyResult(id, expectedRevision, HistoryOperation.UNDO);
    }

    /** Reapplies the next previously undone state of one authored definition.
     *
     * @param id retained definition identity
     * @param expectedRevision definition revision observed by the caller
     * @return accepted, stale, non-editable, invalid, or unavailable outcome
     */
    public AuthoringMutationResult redo(AssetId id, long expectedRevision) {
        ensureOpen();
        return editableWorkingCopyResult(id, expectedRevision, HistoryOperation.REDO);
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
        AssetId validId = Objects.requireNonNull(id, "id");
        return authoredWorkingCopy(validId)
                .map(workingCopy -> workingCopy.save(expectedRevision))
                .orElseGet(() -> persistenceFailure(validId, expectedRevision));
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
        AssetId validId = Objects.requireNonNull(id, "id");
        return authoredWorkingCopy(validId)
                .map(workingCopy -> workingCopy.revert(expectedRevision))
                .orElseGet(() -> persistenceFailure(validId, expectedRevision));
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
        AssetId validId = Objects.requireNonNull(id, "id");
        return authoredWorkingCopy(validId)
                .map(workingCopy -> workingCopy.backup(project.identity().id(), expectedRevision))
                .orElseGet(() -> backupFailure(validId, expectedRevision));
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
        AssetId validId = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(encodedBackup, "encodedBackup");
        return authoredWorkingCopy(validId)
                .map(workingCopy -> workingCopy.restore(project.identity().id(), encodedBackup, expectedRevision))
                .orElseGet(() -> restoreFailure(validId, expectedRevision));
    }

    /** Establishes a future successfully persisted authored document as the in-memory baseline. */
    AuthoringMutationResult markPersisted(
            AssetId id, AuthoredDefinitionDocument baselinedDocument, long expectedRevision) {
        ensureOpen();
        return authoredWorkingCopy(id)
                .map(workingCopy -> workingCopy.markPersisted(baselinedDocument, expectedRevision))
                .orElseGet(() -> retainedFailure(id, expectedRevision));
    }

    /** Returns the hierarchy projection for the current startup world.
     *
     * @return current definition context and hierarchy roots
     */
    public EditorHierarchyProjection hierarchy() {
        ensureOpen();
        return hierarchy;
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
        InspectorTarget validTarget = Objects.requireNonNull(target, "target");
        HierarchyOccurrenceId occurrence = validTarget
                .occurrence()
                .orElseThrow(() -> new IllegalArgumentException("Inspector target must have a hierarchy occurrence"));
        RetainedState retained = Optional.ofNullable(retainedDefinitions.get(occurrence.definition()))
                .orElseThrow(() ->
                        new IllegalStateException("Inspector definition is not retained: " + occurrence.definition()));
        EditorRetainedDefinition current = snapshot(retained);
        requireRevision(current.revision(), expectedRevision);
        EditorHierarchyNode node = current.hierarchy().roots().stream()
                .map(root -> findNode(root, occurrence))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("hierarchy occurrence is not current: " + occurrence));
        if (!node.inspectorTarget().equals(validTarget)) {
            throw new IllegalArgumentException("Inspector target does not match current hierarchy state");
        }
        List<EntityEntry> roots = scopeRoots(current.content());
        ResolvedInspectorEntry resolved = resolveEntry(
                roots,
                occurrence.entityPath(),
                false,
                roots,
                new HierarchyOccurrenceId(occurrence.definition(), List.of()));
        InspectorProjection.Provenance provenance =
                resolved.generated() ? InspectorProjection.Provenance.GENERATED : InspectorProjection.Provenance.LOCAL;
        InspectorProjection.DefinitionOrigin definitionOrigin =
                current.origin() == EditorRetainedDefinition.Origin.AUTHORED
                        ? InspectorProjection.DefinitionOrigin.AUTHORED
                        : InspectorProjection.DefinitionOrigin.GENERATED;
        EditorInspectorProjector.Context context = new EditorInspectorProjector.Context(
                validTarget,
                definitionOrigin,
                provenance,
                node.isEditable(),
                types,
                resolved.scopeRoots(),
                resolved.scopeOccurrence(),
                assets);
        return switch (resolved.entry()) {
            case LocalEntity entity -> EditorInspectorProjector.entity(entity, context);
            case EntityPlacement placement ->
                EditorInspectorProjector.placement(placement, resolvePlacementDefinition(placement), context);
        };
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
        AssetId validDefinition = Objects.requireNonNull(definition, "definition");
        InspectorMutationTarget validTarget = Objects.requireNonNull(target, "target");
        AuthoringMutation validMutation = Objects.requireNonNull(mutation, "mutation");
        Optional<AuthoredDefinitionWorkingCopy> authored = authoredWorkingCopy(validDefinition);
        if (authored.isEmpty()) {
            return retainedFailure(validDefinition, expectedRevision);
        }
        AuthoredDefinitionWorkingCopy workingCopy = authored.orElseThrow();
        if (workingCopy.state().revision() != expectedRevision) {
            return result(workingCopy, AuthoringMutationResult.Outcome.STALE_REVISION, List.of());
        }
        if (!validTarget.occurrence().definition().equals(validDefinition)) {
            return result(workingCopy, AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
        }
        Optional<EditorHierarchyNode> node = editableNode(validDefinition, validTarget.occurrence());
        if (node.isEmpty() || !matchesEntity(node.orElseThrow(), validTarget)) {
            return result(workingCopy, AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
        }
        return switch (validTarget) {
            case InspectorMutationTarget.EntityEnabled enabled ->
                mutateEnabled(workingCopy, enabled, validMutation, expectedRevision);
            case InspectorMutationTarget.ComponentProperty property ->
                mutateProperty(workingCopy, property, validMutation, expectedRevision);
        };
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
        workingCopySubscriptions.forEach(AuthoringSubscription::close);
        workingCopies.values().forEach(AuthoredDefinitionWorkingCopy::close);
        workingCopySubscriptions.clear();
        workingCopies.clear();
        retainedDefinitions.clear();
        changes.close();
        hierarchyChanges.close();
        definitionChanges.close();
        configurationChanges.close();
    }

    /** Refreshes current projections and forwards one coherent working-copy notification. */
    private void workingCopyChanged(AuthoringDefinitionChange change) {
        if (change.definition().equals(startupWorldId)) {
            hierarchy = projectHierarchy();
            hierarchyChanges.emit(hierarchy);
        }
        definitionChanges.emit(change);
    }

    /** Projects the current startup-world working-copy state. */
    private EditorHierarchyProjection projectHierarchy() {
        AuthoredDefinitionWorkingCopy workingCopy = startupWorkingCopy();
        EditorRetainedDefinition.Content.World world =
                (EditorRetainedDefinition.Content.World) workingCopy.state().content();
        return hierarchyProjector.project(
                world.definition(), startupWorldSource.toUri(), true, true, workingCopy.modifiedEntityIds());
    }

    /** Loads one definition whose kind and filesystem source were established by the authored catalog. */
    private DefinitionRetentionResult retainAuthored(AssetMetadata metadata) {
        AuthoredDefinitionWorkingCopy workingCopy = workingCopies.get(metadata.id());
        List<ProjectDiagnostic> loadingDiagnostics = List.of();
        if (workingCopy == null) {
            AuthoredDefinitionDocument.LoadResult result =
                    AuthoredDefinitionDocument.load(authoredAssets, definitions, types, metadata.id());
            if (result.document().isEmpty()) {
                return new DefinitionRetentionResult(Optional.empty(), result.diagnostics());
            }
            workingCopy = openWorkingCopy(result.document().orElseThrow());
            loadingDiagnostics = result.diagnostics();
        }
        RetainedState state = new AuthoredRetainedState(workingCopy);
        retainedDefinitions.put(metadata.id(), state);
        return new DefinitionRetentionResult(Optional.of(snapshot(state)), loadingDiagnostics);
    }

    /** Resolves the only currently publishable generated structural kind. */
    private DefinitionRetentionResult retainGeneratedEntity(AssetId id) {
        DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(id), types);
        return retainLoaded(
                result,
                AssetKind.ENTITY_DEFINITION,
                result.definition().map(EditorRetainedDefinition.Content.Entity::new));
    }

    /** Retains successfully loaded content while preserving resolver diagnostics on failure. */
    private DefinitionRetentionResult retainLoaded(
            DefinitionLoadResult<?> result, AssetKind kind, Optional<EditorRetainedDefinition.Content> content) {
        if (content.isEmpty()) {
            return new DefinitionRetentionResult(Optional.empty(), result.diagnostics());
        }
        EditorRetainedDefinition.Content loaded = content.orElseThrow();
        RetainedState state = new GeneratedRetainedState(loaded.id(), kind, result.source(), loaded);
        retainedDefinitions.put(loaded.id(), state);
        return new DefinitionRetentionResult(Optional.of(snapshot(state)), result.diagnostics());
    }

    /** Creates a current immutable snapshot from authored working-copy or generated retained state. */
    private EditorRetainedDefinition snapshot(RetainedState state) {
        EditorRetainedDefinition.Content content;
        long definitionRevision;
        boolean editable;
        Set<EntityId> modifiedEntities;
        if (state instanceof AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy)) {
            AuthoringDefinitionState authoredState = workingCopy.state();
            content = authoredState.content();
            definitionRevision = authoredState.revision();
            editable = true;
            modifiedEntities = workingCopy.modifiedEntityIds();
        } else {
            GeneratedRetainedState generated = (GeneratedRetainedState) state;
            content = generated.content();
            definitionRevision = 0L;
            editable = false;
            modifiedEntities = Set.of();
        }
        EditorHierarchyProjection projection =
                switch (content) {
                    case EditorRetainedDefinition.Content.World world ->
                        hierarchyProjector.project(
                                world.definition(), state.source(), editable, editable, modifiedEntities);
                    case EditorRetainedDefinition.Content.Entity entity ->
                        hierarchyProjector.project(
                                entity.definition(), state.source(), editable, editable, modifiedEntities);
                };
        return new EditorRetainedDefinition(
                state.id(),
                state.kind(),
                state.origin(),
                editable,
                state.source(),
                definitionRevision,
                content,
                projection);
    }

    /** Rejects a stale inspector read against one retained definition revision. */
    private static void requireRevision(long currentRevision, long expectedRevision) {
        if (expectedRevision != currentRevision) {
            throw new IllegalStateException("stale authoring revision: expected " + expectedRevision
                    + " but current revision is " + currentRevision);
        }
    }

    /** Returns an editable occurrence from the current retained-definition projection. */
    private Optional<EditorHierarchyNode> editableNode(AssetId definition, HierarchyOccurrenceId occurrence) {
        RetainedState retained = retainedDefinitions.get(definition);
        EditorHierarchyProjection currentHierarchy;
        if (retained != null) {
            currentHierarchy = snapshot(retained).hierarchy();
        } else if (definition.equals(startupWorldId)) {
            currentHierarchy = hierarchy;
        } else {
            return Optional.empty();
        }
        return currentHierarchy.roots().stream()
                .map(root -> findNode(root, occurrence))
                .flatMap(Optional::stream)
                .findFirst()
                .filter(EditorHierarchyNode::isEditable);
    }

    private static Optional<EditorHierarchyNode> findNode(EditorHierarchyNode node, HierarchyOccurrenceId occurrence) {
        if (node.occurrence().equals(occurrence)) {
            return Optional.of(node);
        }
        return node.children().stream()
                .map(child -> findNode(child, occurrence))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Returns the directly authored roots of one retained structural definition. */
    private static List<EntityEntry> scopeRoots(EditorRetainedDefinition.Content content) {
        return switch (content) {
            case EditorRetainedDefinition.Content.World world ->
                world.definition().roots();
            case EditorRetainedDefinition.Content.Entity entity ->
                List.of(entity.definition().root());
        };
    }

    /** Resolves one occurrence path, traversing placed definitions without adding a wrapper entity. */
    private ResolvedInspectorEntry resolveEntry(
            List<EntityEntry> roots,
            List<EntityId> path,
            boolean generated,
            List<EntityEntry> scopeRoots,
            HierarchyOccurrenceId scopeOccurrence) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Inspector occurrence must identify an entity or placement");
        }
        EntityEntry entry = roots.stream()
                .filter(candidate -> candidate.id().equals(path.getFirst()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Inspector occurrence path is not current"));
        if (path.size() == 1) {
            return new ResolvedInspectorEntry(entry, generated, scopeRoots, scopeOccurrence);
        }
        List<EntityId> remainder = path.subList(1, path.size());
        return switch (entry) {
            case LocalEntity entity ->
                resolveEntry(entity.children(), remainder, generated, scopeRoots, scopeOccurrence);
            case EntityPlacement placement -> {
                EntityDefinition definition = resolvePlacementDefinition(placement)
                        .orElseThrow(() -> new IllegalStateException("placed definition is unavailable"));
                yield resolveEntry(
                        definition.root().children(),
                        remainder,
                        true,
                        definition.root().children(),
                        scopeOccurrence.child(placement.id()));
            }
        };
    }

    /** Resolves a placement through the session's authoritative definition graph. */
    private Optional<EntityDefinition> resolvePlacementDefinition(EntityPlacement placement) {
        return definitions.loadEntity(placement.definition(), types).definition();
    }

    /** Checks that one Java-issued mutation target still names the projected entity occurrence. */
    private static boolean matchesEntity(EditorHierarchyNode node, InspectorMutationTarget target) {
        EntityId targetEntity =
                switch (target) {
                    case InspectorMutationTarget.EntityEnabled enabled -> enabled.entity();
                    case InspectorMutationTarget.ComponentProperty property -> property.entity();
                };
        return node.entityId().filter(targetEntity::equals).isPresent();
    }

    /** Applies the built-in entity-enabled SET operation or returns a structured rejection. */
    private AuthoringMutationResult mutateEnabled(
            AuthoredDefinitionWorkingCopy workingCopy,
            InspectorMutationTarget.EntityEnabled target,
            AuthoringMutation mutation,
            long expectedRevision) {
        if (mutation instanceof AuthoringMutation.Remove) {
            return result(workingCopy, AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
        }
        ProjectValue value = ((AuthoringMutation.Set) mutation).value();
        if (!(value instanceof ProjectValue.BooleanValue booleanValue)) {
            return rejectedEnabled(workingCopy, value);
        }
        return workingCopy.setEntityEnabled(target.entity(), booleanValue.value(), expectedRevision);
    }

    /** Applies one source-preserving component-property SET or REMOVE operation. */
    private static AuthoringMutationResult mutateProperty(
            AuthoredDefinitionWorkingCopy workingCopy,
            InspectorMutationTarget.ComponentProperty target,
            AuthoringMutation mutation,
            long expectedRevision) {
        return switch (mutation) {
            case AuthoringMutation.Set set ->
                workingCopy.set(target.entity(), target.component(), target.property(), set.value(), expectedRevision);
            case AuthoringMutation.Remove ignored ->
                workingCopy.remove(target.entity(), target.component(), target.property(), expectedRevision);
        };
    }

    /** Creates a structured rejection for a non-boolean built-in entity-enabled value. */
    private static AuthoringMutationResult rejectedEnabled(
            AuthoredDefinitionWorkingCopy workingCopy, ProjectValue value) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                PropertyValidationDiagnosticCode.KIND,
                workingCopy.current().source().toUri(),
                "/enabled",
                List.of("enabled", "BOOLEAN", ProjectValueKind.of(value)),
                Map.of("technicalDetail", "enabled replacement must be a boolean value"));
        return result(workingCopy, AuthoringMutationResult.Outcome.VALIDATION_REJECTED, List.of(diagnostic));
    }

    /** Opens or reuses exactly one authoritative working copy per authored definition identity. */
    private AuthoredDefinitionWorkingCopy openWorkingCopy(AuthoredDefinitionDocument document) {
        AuthoredDefinitionWorkingCopy existing = workingCopies.get(document.id());
        if (existing != null) {
            return existing;
        }
        AuthoredDefinitionWorkingCopy workingCopy = new AuthoredDefinitionWorkingCopy(document);
        workingCopies.put(document.id(), workingCopy);
        workingCopySubscriptions.add(workingCopy.onDidChange().subscribe(this::workingCopyChanged));
        return workingCopy;
    }

    /** Loads the source-preserving authored startup document established by the project source. */
    private AuthoredDefinitionDocument loadAuthoredDocument(AssetId id) {
        AuthoredDefinitionDocument.LoadResult result =
                AuthoredDefinitionDocument.load(authoredAssets, definitions, types, id);
        diagnostics.addAll(result.diagnostics());
        if (result.document().isEmpty()) {
            throw new IllegalStateException("Authored startup definition could not be loaded: " + id);
        }
        return result.document().orElseThrow();
    }

    /** Returns the always-open startup-world working copy. */
    private AuthoredDefinitionWorkingCopy startupWorkingCopy() {
        AuthoredDefinitionWorkingCopy workingCopy = workingCopies.get(startupWorldId);
        if (workingCopy == null) {
            throw new IllegalStateException("Startup-world working copy is unavailable");
        }
        return workingCopy;
    }

    /** Returns an open authored working copy, including the eagerly opened startup world. */
    private Optional<AuthoredDefinitionWorkingCopy> authoredWorkingCopy(AssetId id) {
        return Optional.ofNullable(workingCopies.get(Objects.requireNonNull(id, "id")));
    }

    /** Performs one history operation against a retained authored definition. */
    private AuthoringMutationResult editableWorkingCopyResult(
            AssetId id, long expectedRevision, HistoryOperation operation) {
        AssetId validId = Objects.requireNonNull(id, "id");
        return authoredWorkingCopy(validId)
                .map(workingCopy -> switch (operation) {
                    case UNDO -> workingCopy.undo(expectedRevision);
                    case REDO -> workingCopy.redo(expectedRevision);
                })
                .orElseGet(() -> retainedFailure(validId, expectedRevision));
    }

    /** Returns a generated-definition or unknown-definition structured failure. */
    private AuthoringMutationResult retainedFailure(AssetId id, long expectedRevision) {
        RetainedState retained = retainedDefinitions.get(id);
        AuthoringMutationResult.Outcome outcome = retained instanceof GeneratedRetainedState
                ? AuthoringMutationResult.Outcome.NON_EDITABLE
                : AuthoringMutationResult.Outcome.INVALID_TARGET;
        long currentRevision = retained == null
                ? Math.max(0L, expectedRevision)
                : snapshot(retained).revision();
        return new AuthoringMutationResult(id, outcome, currentRevision, false, false, false, List.of());
    }

    /** Returns a generated-definition or unknown-definition persistence failure. */
    private AuthoringPersistenceResult persistenceFailure(AssetId id, long expectedRevision) {
        RetainedState retained = retainedDefinitions.get(id);
        AuthoringPersistenceResult.Outcome outcome = retained instanceof GeneratedRetainedState
                ? AuthoringPersistenceResult.Outcome.NON_EDITABLE
                : AuthoringPersistenceResult.Outcome.INVALID_TARGET;
        long currentRevision = retained == null
                ? Math.max(0L, expectedRevision)
                : snapshot(retained).revision();
        return new AuthoringPersistenceResult(id, outcome, currentRevision, false, false, false, List.of());
    }

    /** Returns a generated-definition or unknown-definition backup failure. */
    private AuthoringBackupResult backupFailure(AssetId id, long expectedRevision) {
        RetainedState retained = retainedDefinitions.get(id);
        AuthoringBackupResult.Outcome outcome = retained instanceof GeneratedRetainedState
                ? AuthoringBackupResult.Outcome.NON_EDITABLE
                : AuthoringBackupResult.Outcome.INVALID_TARGET;
        long currentRevision = retained == null
                ? Math.max(0L, expectedRevision)
                : snapshot(retained).revision();
        return new AuthoringBackupResult(id, outcome, currentRevision, false, false, false, Optional.empty());
    }

    /** Returns a generated-definition or unknown-definition restore failure. */
    private AuthoringRestoreResult restoreFailure(AssetId id, long expectedRevision) {
        RetainedState retained = retainedDefinitions.get(id);
        AuthoringRestoreResult.Outcome outcome = retained instanceof GeneratedRetainedState
                ? AuthoringRestoreResult.Outcome.NON_EDITABLE
                : AuthoringRestoreResult.Outcome.INVALID_TARGET;
        long currentRevision = retained == null
                ? Math.max(0L, expectedRevision)
                : snapshot(retained).revision();
        return new AuthoringRestoreResult(id, outcome, currentRevision, false, false, false, List.of());
    }

    /** Creates one operation result from the current working-copy state. */
    private static AuthoringMutationResult result(
            AuthoredDefinitionWorkingCopy workingCopy,
            AuthoringMutationResult.Outcome outcome,
            List<ProjectDiagnostic> diagnostics) {
        AuthoringDefinitionState state = workingCopy.state();
        return new AuthoringMutationResult(
                state.definition(),
                outcome,
                state.revision(),
                state.dirty(),
                state.canUndo(),
                state.canRedo(),
                diagnostics);
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
     * @param startupWorld loaded startup-world definition
     * @param startupWorldSource authored startup-world source path
     */
    public record Source(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authoredAssets,
            RegisteredTypeCatalog types,
            DefinitionResolver definitions,
            WorldDefinition startupWorld,
            Path startupWorldSource) {
        /** Validates the complete loaded source. */
        public Source {
            Objects.requireNonNull(project, "project");
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(authoredAssets, "authoredAssets");
            Objects.requireNonNull(types, "types");
            Objects.requireNonNull(definitions, "definitions");
            Objects.requireNonNull(startupWorld, "startupWorld");
            Objects.requireNonNull(startupWorldSource, "startupWorldSource");
        }
    }

    /** Selects one per-definition history transition. */
    private enum HistoryOperation {
        /** Restore the previous accepted state. */
        UNDO,
        /** Restore the next previously undone state. */
        REDO
    }

    /** Session-owned retained definition state independent of transient protocol snapshots. */
    private sealed interface RetainedState permits AuthoredRetainedState, GeneratedRetainedState {
        /** Returns the retained asset identity. */
        AssetId id();

        /** Returns the retained structural kind. */
        AssetKind kind();

        /** Returns the definition-level origin. */
        EditorRetainedDefinition.Origin origin();

        /** Returns the absolute logical source. */
        URI source();
    }

    /** Retained authored state backed by exactly one authoritative working copy. */
    private record AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy) implements RetainedState {
        /** Validates the retained authored state. */
        private AuthoredRetainedState {
            Objects.requireNonNull(workingCopy, "workingCopy");
        }

        @Override
        public AssetId id() {
            return workingCopy.state().definition();
        }

        @Override
        public AssetKind kind() {
            return workingCopy.state().kind();
        }

        @Override
        public EditorRetainedDefinition.Origin origin() {
            return EditorRetainedDefinition.Origin.AUTHORED;
        }

        @Override
        public URI source() {
            return workingCopy.current().source().toUri();
        }
    }

    /** Retained immutable generated structural state. */
    private record GeneratedRetainedState(
            AssetId id, AssetKind kind, URI source, EditorRetainedDefinition.Content content) implements RetainedState {
        /** Validates the retained generated state. */
        private GeneratedRetainedState {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(content, "content");
            if (!id.equals(content.id()) || kind != content.kind()) {
                throw new IllegalArgumentException("generated definition identity and content must agree");
            }
        }

        @Override
        public EditorRetainedDefinition.Origin origin() {
            return EditorRetainedDefinition.Origin.GENERATED;
        }
    }

    /** One resolved occurrence and whether traversal crossed a placement boundary. */
    private record ResolvedInspectorEntry(
            EntityEntry entry, boolean generated, List<EntityEntry> scopeRoots, HierarchyOccurrenceId scopeOccurrence) {
        private ResolvedInspectorEntry {
            Objects.requireNonNull(entry, "entry");
            scopeRoots = List.copyOf(scopeRoots);
            Objects.requireNonNull(scopeOccurrence, "scopeOccurrence");
        }
    }
}
