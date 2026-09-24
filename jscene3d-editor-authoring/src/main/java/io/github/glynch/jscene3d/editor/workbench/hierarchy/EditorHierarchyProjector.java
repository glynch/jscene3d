/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Projects a world and placed definition roots into an immutable semantic hierarchy. */
public final class EditorHierarchyProjector {
    private final Path worldSource;
    private final ProjectionContext context;

    /**
     * Creates a projector backed by loaded project catalogs.
     *
     * @param worldSource authored startup-world source
     * @param projectRoot opened project root
     * @param authored authored asset catalog
     * @param definitions definition resolver
     * @param types registered project types
     * @param diagnostics collection receiving definition-loading diagnostics
     */
    public EditorHierarchyProjector(
            Path worldSource,
            Path projectRoot,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            Collection<ProjectDiagnostic> diagnostics) {
        this.worldSource = Objects.requireNonNull(worldSource, "worldSource");
        context = new ProjectionContext(projectRoot, authored, definitions, types, diagnostics);
    }

    /**
     * Projects the latest authored world.
     *
     * @param world current startup-world definition
     * @param modifiedEntityIds entities modified since the saved revision
     * @return immutable hierarchy root
     */
    public EditorHierarchyNode project(WorldDefinition world, Set<EntityId> modifiedEntityIds) {
        ProjectionState state = new ProjectionState(Set.copyOf(modifiedEntityIds));
        HierarchyOccurrenceId root = new HierarchyOccurrenceId(world.id(), List.of());
        List<EditorHierarchyNode> children = world.roots().stream()
                .map(entry -> projectEntry(entry, worldSource, new HashSet<>(), false, root, state))
                .toList();
        InspectorTarget target = new InspectorTarget(
                InspectorTarget.Kind.WORLD, worldSource, world.id().toString(), Optional.of(root));
        return new EditorHierarchyNode(
                new EditorHierarchyNode.Identity(
                        root, EditorHierarchyNode.Kind.WORLD, Optional.empty(), Optional.of(world.id())),
                world.name(),
                new EditorHierarchyNode.AuthoringState(true, false, Optional.empty()),
                target,
                children);
    }

    private EditorHierarchyNode projectEntry(
            EntityEntry entry,
            Path source,
            Set<AssetId> ancestors,
            boolean generated,
            HierarchyOccurrenceId parent,
            ProjectionState state) {
        HierarchyOccurrenceId occurrence = parent.child(entry.id());
        return switch (entry) {
            case LocalEntity local -> projectLocal(local, source, ancestors, generated, occurrence, state);
            case EntityPlacement placement ->
                projectPlacement(placement, source, ancestors, generated, occurrence, state);
        };
    }

    private EditorHierarchyNode projectLocal(
            LocalEntity local,
            Path source,
            Set<AssetId> ancestors,
            boolean generated,
            HierarchyOccurrenceId occurrence,
            ProjectionState state) {
        List<EditorHierarchyNode> children = local.children().stream()
                .map(child -> projectEntry(child, source, ancestors, generated, occurrence, state))
                .toList();
        EditorHierarchyNode.Kind kind =
                generated ? EditorHierarchyNode.Kind.GENERATED_ENTITY : EditorHierarchyNode.Kind.LOCAL_ENTITY;
        InspectorTarget.Kind targetKind =
                generated ? InspectorTarget.Kind.GENERATED_ENTITY : InspectorTarget.Kind.LOCAL_ENTITY;
        return new EditorHierarchyNode(
                new EditorHierarchyNode.Identity(occurrence, kind, Optional.of(local.id()), Optional.empty()),
                local.name().orElse("Unnamed entity"),
                new EditorHierarchyNode.AuthoringState(
                        local.isEnabled(),
                        !generated && state.modified().contains(local.id()),
                        generated
                                ? Optional.empty()
                                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, local.id()))),
                new InspectorTarget(targetKind, source, local.id().toString(), Optional.of(occurrence)),
                children);
    }

    private EditorHierarchyNode projectPlacement(
            EntityPlacement placement,
            Path source,
            Set<AssetId> ancestors,
            boolean generated,
            HierarchyOccurrenceId occurrence,
            ProjectionState state) {
        DefinitionLoadResult<EntityDefinition> result =
                context.definitions().loadEntity(placement.definition(), context.types());
        context.diagnostics().addAll(result.diagnostics());
        Optional<EntityDefinition> loaded = result.definition();
        if (loaded.isEmpty() || !ancestors.add(placement.definition().id())) {
            return placementNode(
                    placement,
                    source,
                    generated,
                    occurrence,
                    state,
                    placement.name().orElse("Unavailable definition"),
                    List.of());
        }
        EntityDefinition definition = loaded.orElseThrow();
        Path definitionSource = context.authored()
                .find(definition.id())
                .map(AssetMetadata::path)
                .orElse(source);
        List<EditorHierarchyNode> children = definition.root().children().stream()
                .map(child -> projectEntry(child, definitionSource, ancestors, true, occurrence, state))
                .toList();
        ancestors.remove(placement.definition().id());
        String label = placement.name().orElseGet(() -> definition.root().name().orElse(definition.name()));
        return placementNode(placement, source, generated, occurrence, state, label, children);
    }

    private static EditorHierarchyNode placementNode(
            EntityPlacement placement,
            Path source,
            boolean generated,
            HierarchyOccurrenceId occurrence,
            ProjectionState state,
            String label,
            List<EditorHierarchyNode> children) {
        return new EditorHierarchyNode(
                new EditorHierarchyNode.Identity(
                        occurrence,
                        EditorHierarchyNode.Kind.PLACEMENT,
                        Optional.of(placement.id()),
                        Optional.of(placement.definition().id())),
                label,
                new EditorHierarchyNode.AuthoringState(
                        placement.isEnabled(),
                        !generated && state.modified().contains(placement.id()),
                        generated
                                ? Optional.empty()
                                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, placement.id()))),
                new InspectorTarget(
                        InspectorTarget.Kind.PLACEMENT, source, placement.id().toString(), Optional.of(occurrence)),
                children);
    }

    /** Values that vary for each hierarchy refresh. */
    private record ProjectionState(Set<EntityId> modified) {}

    /** Stable services used by all hierarchy refreshes. */
    private record ProjectionContext(
            Path projectRoot,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            Collection<ProjectDiagnostic> diagnostics) {
        private ProjectionContext {
            Objects.requireNonNull(projectRoot, "projectRoot");
            Objects.requireNonNull(authored, "authored");
            Objects.requireNonNull(definitions, "definitions");
            Objects.requireNonNull(types, "types");
            Objects.requireNonNull(diagnostics, "diagnostics");
        }
    }
}
