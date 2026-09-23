/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.editor.workbench.inspector.EditorComponentPropertyEditor;
import io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProjector;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Projects a world and placed definition roots into an immutable Hierarchy model. */
public final class EditorHierarchyProjector {
    private final Path worldSource;
    private final ProjectionContext context;

    /**
     * Creates a projector backed by the loaded project catalogs.
     *
     * @param worldSource authored startup-world source
     * @param projectRoot opened project root
     * @param authored authored asset catalog
     * @param definitions definition resolver
     * @param types registered project types
     * @param diagnostics diagnostic collection receiving definition-loading diagnostics
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
     * Projects the latest authored world and edit commands.
     *
     * @param world current startup-world definition
     * @param modifiedEntityIds entities modified since the saved revision
     * @param enabledEditor enabled-state edit command
     * @param componentPropertyEditor component-property edit command
     * @return immutable hierarchy root
     */
    public EditorHierarchyNode project(
            WorldDefinition world,
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabledEditor,
            EditorComponentPropertyEditor componentPropertyEditor) {
        ProjectionEditors editors = new ProjectionEditors(modifiedEntityIds, enabledEditor, componentPropertyEditor);
        List<EditorHierarchyNode> children = world.roots().stream()
                .map(entry -> projectEntry(entry, worldSource, new HashSet<>(), false, editors))
                .toList();
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                world.name(),
                Optional.empty(),
                Optional.of(world.id()),
                true,
                EditorInspectorProjector.world(world, worldSource, context.projectRoot()),
                children);
    }

    private EditorHierarchyNode projectEntry(
            EntityEntry entry, Path source, Set<AssetId> ancestors, boolean generated, ProjectionEditors editors) {
        return switch (entry) {
            case LocalEntity local -> projectLocal(local, source, ancestors, generated, editors);
            case EntityPlacement placement -> projectPlacement(placement, source, ancestors, generated, editors);
        };
    }

    private EditorHierarchyNode projectLocal(
            LocalEntity local, Path source, Set<AssetId> ancestors, boolean generated, ProjectionEditors editors) {
        List<EditorHierarchyNode> children = local.children().stream()
                .map(child -> projectEntry(child, source, ancestors, generated, editors))
                .toList();
        Optional<Consumer<Boolean>> editor = generated
                ? Optional.empty()
                : Optional.of(value -> editors.enabled().accept(local.id(), value));
        return new EditorHierarchyNode(
                generated ? EditorHierarchyNode.Kind.GENERATED_ENTITY : EditorHierarchyNode.Kind.LOCAL_ENTITY,
                local.name().orElse("Unnamed entity"),
                Optional.of(local.id()),
                Optional.empty(),
                new EditorHierarchyNode.AuthoringState(
                        local.isEnabled(),
                        !generated && editors.modifiedEntityIds().contains(local.id())),
                EditorInspectorProjector.entity(
                        local,
                        source,
                        context.projectRoot(),
                        context.types(),
                        generated,
                        editor,
                        generated ? Optional.empty() : Optional.of(editors.componentProperty())),
                children);
    }

    private EditorHierarchyNode projectPlacement(
            EntityPlacement placement,
            Path source,
            Set<AssetId> ancestors,
            boolean generated,
            ProjectionEditors editors) {
        DefinitionLoadResult<EntityDefinition> result =
                context.definitions().loadEntity(placement.definition(), context.types());
        context.diagnostics().addAll(result.diagnostics());
        Optional<EntityDefinition> loaded = result.definition();
        if (loaded.isEmpty() || !ancestors.add(placement.definition().id())) {
            return unavailablePlacement(placement, loaded, source, generated, editors);
        }
        EntityDefinition definition = loaded.orElseThrow();
        Path definitionSource = context.authored()
                .find(definition.id())
                .map(AssetMetadata::path)
                .orElse(source);
        List<EditorHierarchyNode> children = definition.root().children().stream()
                .map(child -> projectEntry(child, definitionSource, ancestors, true, editors))
                .toList();
        ancestors.remove(placement.definition().id());
        String label = placement.name().orElseGet(() -> definition.root().name().orElse(definition.name()));
        return placementNode(placement, loaded, source, generated, editors, label, children);
    }

    private EditorHierarchyNode unavailablePlacement(
            EntityPlacement placement,
            Optional<EntityDefinition> loaded,
            Path source,
            boolean generated,
            ProjectionEditors editors) {
        String label = placement.name().orElse("Unavailable definition");
        return placementNode(placement, loaded, source, generated, editors, label, List.of());
    }

    private EditorHierarchyNode placementNode(
            EntityPlacement placement,
            Optional<EntityDefinition> loaded,
            Path source,
            boolean generated,
            ProjectionEditors editors,
            String label,
            List<EditorHierarchyNode> children) {
        Optional<Consumer<Boolean>> editor = generated
                ? Optional.empty()
                : Optional.of(value -> editors.enabled().accept(placement.id(), value));
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.PLACEMENT,
                label,
                Optional.of(placement.id()),
                Optional.of(placement.definition().id()),
                new EditorHierarchyNode.AuthoringState(
                        placement.isEnabled(),
                        !generated && editors.modifiedEntityIds().contains(placement.id())),
                EditorInspectorProjector.placement(
                        placement, loaded, source, context.projectRoot(), context.types(), generated, editor),
                children);
    }

    private record ProjectionEditors(
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabled,
            EditorComponentPropertyEditor componentProperty) {}

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
