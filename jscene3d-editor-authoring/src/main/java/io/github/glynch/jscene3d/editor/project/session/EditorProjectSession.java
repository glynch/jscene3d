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
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorWorldWorkingCopy;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Central revisioned facade for one loaded JScene3D authoring project. */
public final class EditorProjectSession implements AutoCloseable {
    private final GameProject project;
    private final ProjectConfiguration configuration;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final DefinitionResolver definitions;
    private final List<ProjectAsset> assets;
    private final Collection<ProjectDiagnostic> diagnostics;
    private final EditorHierarchyProjector hierarchyProjector;
    private final EditorWorldWorkingCopy startupWorld;
    private final AuthoringChangeSource<AuthoringSessionChange> changes = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<EditorHierarchyProjection> hierarchyChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<Boolean> dirtyChanges = new AuthoringChangeSource<>();
    private final AuthoringChangeSource<AuthoringConfigurationChange> configurationChanges =
            new AuthoringChangeSource<>();
    private final AuthoringSubscription workingCopySubscription;
    private final Map<AssetId, RetainedState> retainedDefinitions = new LinkedHashMap<>();

    private EditorHierarchyProjection hierarchy;
    private long revision;
    private boolean lastDirty;
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
        startupWorld = new EditorWorldWorkingCopy(validSource.startupWorldSource(), validSource.startupWorld());
        workingCopySubscription = startupWorld.onDidChange().subscribe(this::workingCopyChanged);
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
        return startupWorld.current();
    }

    /** Returns the normalized authored startup-world source.
     *
     * @return authored world source
     */
    public Path startupWorldSource() {
        ensureOpen();
        return startupWorld.source();
    }

    /** Returns the current authoritative authoring revision.
     *
     * @return non-negative revision
     */
    public long revision() {
        ensureOpen();
        return revision;
    }

    /**
     * Resolves and retains one structural definition by authoritative asset identity.
     *
     * <p>The startup world reuses its existing working copy. Other definitions are retained as immutable loaded
     * content. Generated definitions remain read-only and retain their logical published source.
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

    /** Returns whether startup-world content differs from its saved baseline.
     *
     * @return dirty state
     */
    public boolean isDirty() {
        ensureOpen();
        return startupWorld.isDirty();
    }

    /** Returns whether an earlier in-memory revision can be restored.
     *
     * @return whether undo is available
     */
    public boolean canUndo() {
        ensureOpen();
        return undoOperation().isPresent();
    }

    /** Returns whether a previously undone revision can be restored.
     *
     * @return whether redo is available
     */
    public boolean canRedo() {
        ensureOpen();
        return redoOperation().isPresent();
    }

    /** Returns metadata for the next undo operation.
     *
     * @return next undo operation when available
     */
    public Optional<AuthoringOperation> undoOperation() {
        ensureOpen();
        return startupWorld.undoOperation();
    }

    /** Returns metadata for the next redo operation.
     *
     * @return next redo operation when available
     */
    public Optional<AuthoringOperation> redoOperation() {
        ensureOpen();
        return startupWorld.redoOperation();
    }

    /** Restores the immediately preceding authored revision when available. */
    public void undo() {
        ensureOpen();
        startupWorld.undo();
    }

    /** Reapplies the immediately following authored revision when available. */
    public void redo() {
        ensureOpen();
        startupWorld.redo();
    }

    /**
     * Saves dirty startup-world content.
     *
     * @throws IOException when the replacement cannot be persisted
     */
    public void save() throws IOException {
        ensureOpen();
        startupWorld.save();
    }

    /** Reverts dirty startup-world content and clears undo/redo history. */
    public void revert() {
        ensureOpen();
        startupWorld.revert();
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
        requireRevision(expectedRevision);
        InspectorTarget validTarget = Objects.requireNonNull(target, "target");
        HierarchyOccurrenceId occurrence = validTarget
                .occurrence()
                .orElseThrow(() -> new IllegalArgumentException("Inspector target must have a hierarchy occurrence"));
        RetainedState retained = Optional.ofNullable(retainedDefinitions.get(occurrence.definition()))
                .orElseThrow(() ->
                        new IllegalStateException("Inspector definition is not retained: " + occurrence.definition()));
        EditorRetainedDefinition current = snapshot(retained);
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

    /** Returns dirty-state transitions.
     *
     * @return dirty-state change stream
     */
    public AuthoringChange<Boolean> onDidChangeDirty() {
        ensureOpen();
        return dirtyChanges;
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
        revision++;
        configurationChanges.emit(new AuthoringConfigurationChange(key, true, revision));
        changes.emit(new AuthoringSessionChange(revision, isDirty(), AuthoringSessionChange.Kind.CONFIGURATION));
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
        revision++;
        configurationChanges.emit(new AuthoringConfigurationChange(key, false, revision));
        changes.emit(new AuthoringSessionChange(revision, isDirty(), AuthoringSessionChange.Kind.CONFIGURATION));
    }

    /**
     * Applies one typed Inspector mutation against an expected authoring revision.
     *
     * @param target stable semantic mutation target
     * @param replacement typed replacement value
     * @param expectedRevision revision observed by the caller
     * @return resulting authoritative revision
     * @throws IllegalStateException if the revision is stale or the target is not editable
     * @throws IllegalArgumentException if the replacement violates descriptor constraints
     */
    public long mutate(InspectorMutationTarget target, ProjectValue replacement, long expectedRevision) {
        ensureOpen();
        requireRevision(expectedRevision);
        InspectorMutationTarget validTarget = Objects.requireNonNull(target, "target");
        ProjectValue value = Objects.requireNonNull(replacement, "replacement");
        EditorHierarchyNode node = editableNode(validTarget.occurrence());
        switch (validTarget) {
            case InspectorMutationTarget.EntityEnabled enabled -> {
                requireEntity(node, enabled.entity());
                if (!(value instanceof ProjectValue.BooleanValue booleanValue)) {
                    throw new IllegalArgumentException("enabled replacement must be a boolean value");
                }
                startupWorld.setEntityEnabled(enabled.entity(), booleanValue.value());
            }
            case InspectorMutationTarget.ComponentProperty property -> {
                requireEntity(node, property.entity());
                PropertyDescriptor descriptor = propertyDescriptor(property);
                if (!descriptor.accepts(value)) {
                    throw new IllegalArgumentException("replacement does not satisfy property "
                            + property.property().value());
                }
                startupWorld.setComponentProperty(property.entity(), property.component(), property.property(), value);
            }
        }
        return revision;
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
        workingCopySubscription.close();
        startupWorld.close();
        retainedDefinitions.clear();
        changes.close();
        hierarchyChanges.close();
        dirtyChanges.close();
        configurationChanges.close();
    }

    private void workingCopyChanged(EditorWorldWorkingCopy.Change change) {
        boolean dirty = startupWorld.isDirty();
        if (change != EditorWorldWorkingCopy.Change.SAVED) {
            revision++;
            hierarchy = projectHierarchy();
            hierarchyChanges.emit(hierarchy);
        }
        if (dirty != lastDirty) {
            lastDirty = dirty;
            dirtyChanges.emit(dirty);
        }
        AuthoringSessionChange.Kind kind =
                switch (change) {
                    case CONTENT -> AuthoringSessionChange.Kind.CONTENT;
                    case SAVED -> AuthoringSessionChange.Kind.SAVED;
                    case REVERTED -> AuthoringSessionChange.Kind.REVERTED;
                };
        changes.emit(new AuthoringSessionChange(revision, dirty, kind));
    }

    private EditorHierarchyProjection projectHierarchy() {
        return hierarchyProjector.project(
                startupWorld.current(), startupWorld.source().toUri(), true, true, startupWorld.modifiedEntityIds());
    }

    /** Loads one definition whose kind and filesystem source were established by the authored catalog. */
    private DefinitionRetentionResult retainAuthored(AssetMetadata metadata) {
        return switch (metadata.kind()) {
            case WORLD_DEFINITION -> retainAuthoredWorld(metadata);
            case ENTITY_DEFINITION -> {
                DefinitionLoadResult<EntityDefinition> result =
                        definitions.loadEntity(AssetRef.to(metadata.id()), types);
                yield retainLoaded(
                        result,
                        AssetKind.ENTITY_DEFINITION,
                        EditorRetainedDefinition.Origin.AUTHORED,
                        result.definition().map(EditorRetainedDefinition.Content.Entity::new));
            }
        };
    }

    /** Reuses the startup working copy or loads another authored world as immutable content. */
    private DefinitionRetentionResult retainAuthoredWorld(AssetMetadata metadata) {
        if (metadata.id().equals(startupWorld.current().id())) {
            RetainedState state = new RetainedState(
                    metadata.id(),
                    AssetKind.WORLD_DEFINITION,
                    EditorRetainedDefinition.Origin.AUTHORED,
                    metadata.path().toUri(),
                    new EditorRetainedDefinition.Content.World(startupWorld.current()));
            retainedDefinitions.put(metadata.id(), state);
            return new DefinitionRetentionResult(Optional.of(snapshot(state)), List.of());
        }
        DefinitionLoadResult<WorldDefinition> result = definitions.loadWorld(AssetRef.to(metadata.id()), types);
        return retainLoaded(
                result,
                AssetKind.WORLD_DEFINITION,
                EditorRetainedDefinition.Origin.AUTHORED,
                result.definition().map(EditorRetainedDefinition.Content.World::new));
    }

    /** Resolves the only currently publishable generated structural kind. */
    private DefinitionRetentionResult retainGeneratedEntity(AssetId id) {
        DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(id), types);
        return retainLoaded(
                result,
                AssetKind.ENTITY_DEFINITION,
                EditorRetainedDefinition.Origin.GENERATED,
                result.definition().map(EditorRetainedDefinition.Content.Entity::new));
    }

    /** Retains successfully loaded content while preserving resolver diagnostics on failure. */
    private DefinitionRetentionResult retainLoaded(
            DefinitionLoadResult<?> result,
            AssetKind kind,
            EditorRetainedDefinition.Origin origin,
            Optional<EditorRetainedDefinition.Content> content) {
        if (content.isEmpty()) {
            return new DefinitionRetentionResult(Optional.empty(), result.diagnostics());
        }
        EditorRetainedDefinition.Content loaded = content.orElseThrow();
        RetainedState state = new RetainedState(loaded.id(), kind, origin, result.source(), loaded);
        retainedDefinitions.put(loaded.id(), state);
        return new DefinitionRetentionResult(Optional.of(snapshot(state)), result.diagnostics());
    }

    /** Creates a current immutable snapshot, refreshing the startup world from its authoritative working copy. */
    private EditorRetainedDefinition snapshot(RetainedState state) {
        EditorRetainedDefinition.Content content =
                state.id().equals(startupWorld.current().id())
                        ? new EditorRetainedDefinition.Content.World(startupWorld.current())
                        : state.content();
        boolean editable = state.origin() == EditorRetainedDefinition.Origin.AUTHORED
                && state.id().equals(startupWorld.current().id());
        EditorHierarchyProjection projection =
                switch (content) {
                    case EditorRetainedDefinition.Content.World world ->
                        hierarchyProjector.project(
                                world.definition(),
                                state.source(),
                                editable,
                                state.id().equals(startupWorld.current().id()),
                                startupWorld.modifiedEntityIds());
                    case EditorRetainedDefinition.Content.Entity entity ->
                        hierarchyProjector.project(entity.definition(), state.source(), editable, false, Set.of());
                };
        return new EditorRetainedDefinition(
                state.id(), state.kind(), state.origin(), editable, state.source(), revision, content, projection);
    }

    private void requireRevision(long expectedRevision) {
        if (expectedRevision != revision) {
            throw new IllegalStateException(
                    "stale authoring revision: expected " + expectedRevision + " but current revision is " + revision);
        }
    }

    private EditorHierarchyNode editableNode(HierarchyOccurrenceId occurrence) {
        EditorHierarchyNode node = hierarchy.roots().stream()
                .map(root -> findNode(root, occurrence))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("hierarchy occurrence is not current: " + occurrence));
        if (!node.isEditable()) {
            throw new IllegalStateException("hierarchy occurrence is read-only: " + occurrence);
        }
        return node;
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

    private static void requireEntity(EditorHierarchyNode node, EntityId id) {
        if (node.entityId().filter(id::equals).isEmpty()) {
            throw new IllegalStateException("mutation entity does not match hierarchy occurrence");
        }
    }

    private PropertyDescriptor propertyDescriptor(InspectorMutationTarget.ComponentProperty target) {
        LocalEntity entity = findLocal(startupWorld.current().roots(), target.entity())
                .orElseThrow(() -> new IllegalStateException("local entity is not current: " + target.entity()));
        ComponentDefinition component = entity.components().stream()
                .filter(candidate -> candidate.id().equals(target.component()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("component is not current: " + target.component()));
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        return types.findComponent(type)
                .flatMap(descriptor ->
                        Optional.ofNullable(descriptor.properties().get(target.property())))
                .orElseThrow(
                        () -> new IllegalStateException("property descriptor is unavailable: " + target.property()));
    }

    private static Optional<LocalEntity> findLocal(List<EntityEntry> entries, EntityId target) {
        for (EntityEntry entry : entries) {
            if (entry instanceof LocalEntity local) {
                if (local.id().equals(target)) {
                    return Optional.of(local);
                }
                Optional<LocalEntity> child = findLocal(local.children(), target);
                if (child.isPresent()) {
                    return child;
                }
            }
        }
        return Optional.empty();
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

    /** Session-owned retained definition data independent of transient protocol snapshots. */
    private record RetainedState(
            AssetId id,
            AssetKind kind,
            EditorRetainedDefinition.Origin origin,
            URI source,
            EditorRetainedDefinition.Content content) {
        private RetainedState {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(content, "content");
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
