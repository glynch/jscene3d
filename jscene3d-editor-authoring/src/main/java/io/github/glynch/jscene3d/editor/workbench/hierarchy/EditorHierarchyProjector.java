/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
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
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Projects structural definitions and placed definition roots into an immutable semantic hierarchy. */
public final class EditorHierarchyProjector {
    private final ProjectionContext context;

    /**
     * Creates a projector backed by loaded project catalogs.
     *
     * @param definitions definition resolver
     * @param types registered project types
     * @param diagnostics collection receiving definition-loading diagnostics
     */
    public EditorHierarchyProjector(
            DefinitionResolver definitions, RegisteredTypeCatalog types, Collection<ProjectDiagnostic> diagnostics) {
        context = new ProjectionContext(definitions, types, diagnostics);
    }

    /**
     * Projects the latest authored world.
     *
     * @param world world definition
     * @param source logical definition source
     * @param definitionEditable whether the definition has authored editability
     * @param entriesEditable whether local entries have an authoritative writable working copy
     * @param modifiedEntityIds entities modified since the saved revision
     * @return immutable definition context and actual ordered roots
     */
    public EditorHierarchyProjection project(
            WorldDefinition world,
            URI source,
            boolean definitionEditable,
            boolean entriesEditable,
            Set<EntityId> modifiedEntityIds) {
        Objects.requireNonNull(world, "world");
        URI validSource = requireAbsolute(source);
        ProjectionState state = new ProjectionState(Set.copyOf(modifiedEntityIds));
        HierarchyOccurrenceId root = new HierarchyOccurrenceId(world.id(), List.of());
        List<EditorHierarchyNode> roots = world.roots().stream()
                .map(entry -> projectEntry(entry, validSource, new HashSet<>(), false, entriesEditable, root, state))
                .toList();
        EditorHierarchyProjection.Context definition = new EditorHierarchyProjection.Context(
                world.id(),
                AssetKind.WORLD_DEFINITION,
                AuthoringText.literal(world.name()),
                validSource,
                definitionEditable);
        return new EditorHierarchyProjection(definition, roots);
    }

    /**
     * Projects one independently opened reusable entity definition.
     *
     * @param definition entity definition
     * @param source logical definition source
     * @param definitionEditable whether the definition has authored editability
     * @param entriesEditable whether local entries have an authoritative writable working copy
     * @param modifiedEntityIds entities modified since the saved revision
     * @return immutable definition context and actual root entity
     */
    public EditorHierarchyProjection project(
            EntityDefinition definition,
            URI source,
            boolean definitionEditable,
            boolean entriesEditable,
            Set<EntityId> modifiedEntityIds) {
        Objects.requireNonNull(definition, "definition");
        URI validSource = requireAbsolute(source);
        ProjectionState state = new ProjectionState(Set.copyOf(modifiedEntityIds));
        HierarchyOccurrenceId root = new HierarchyOccurrenceId(definition.id(), List.of());
        EditorHierarchyNode entityRoot = projectEntry(
                definition.root(),
                validSource,
                new HashSet<>(Set.of(definition.id())),
                false,
                entriesEditable,
                root,
                state);
        EditorHierarchyProjection.Context context = new EditorHierarchyProjection.Context(
                definition.id(),
                AssetKind.ENTITY_DEFINITION,
                AuthoringText.literal(definition.name()),
                validSource,
                definitionEditable);
        return new EditorHierarchyProjection(context, List.of(entityRoot));
    }

    private EditorHierarchyNode projectEntry(
            EntityEntry entry,
            URI source,
            Set<AssetId> ancestors,
            boolean generated,
            boolean editable,
            HierarchyOccurrenceId parent,
            ProjectionState state) {
        HierarchyOccurrenceId occurrence = parent.child(entry.id());
        return switch (entry) {
            case LocalEntity local -> projectLocal(local, source, ancestors, generated, editable, occurrence, state);
            case EntityPlacement placement ->
                projectPlacement(placement, source, ancestors, generated, editable, occurrence, state);
        };
    }

    private EditorHierarchyNode projectLocal(
            LocalEntity local,
            URI source,
            Set<AssetId> ancestors,
            boolean generated,
            boolean editable,
            HierarchyOccurrenceId occurrence,
            ProjectionState state) {
        List<EditorHierarchyNode> children = local.children().stream()
                .map(child -> projectEntry(child, source, ancestors, generated, editable, occurrence, state))
                .toList();
        EditorHierarchyNode.Kind kind =
                generated ? EditorHierarchyNode.Kind.GENERATED_ENTITY : EditorHierarchyNode.Kind.LOCAL_ENTITY;
        InspectorTarget.Kind targetKind =
                generated ? InspectorTarget.Kind.GENERATED_ENTITY : InspectorTarget.Kind.LOCAL_ENTITY;
        return new EditorHierarchyNode(
                new EditorHierarchyNode.Identity(occurrence, kind, Optional.of(local.id()), Optional.empty()),
                local.name()
                        .<AuthoringText>map(AuthoringText::literal)
                        .orElseGet(() -> AuthoringText.message("editor.hierarchy.unnamed-entity", "Unnamed entity")),
                new EditorHierarchyNode.AuthoringState(
                        local.isEnabled(),
                        editable && !generated && state.modified().contains(local.id()),
                        generated || !editable
                                ? Optional.empty()
                                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, local.id()))),
                new InspectorTarget(targetKind, source, local.id().toString(), Optional.of(occurrence)),
                children);
    }

    private EditorHierarchyNode projectPlacement(
            EntityPlacement placement,
            URI source,
            Set<AssetId> ancestors,
            boolean generated,
            boolean editable,
            HierarchyOccurrenceId occurrence,
            ProjectionState state) {
        EntryProjectionState entryState = new EntryProjectionState(source, generated, editable, state);
        DefinitionLoadResult<EntityDefinition> result =
                context.definitions().loadEntity(placement.definition(), context.types());
        context.diagnostics().addAll(result.diagnostics());
        Optional<EntityDefinition> loaded = result.definition();
        if (loaded.isEmpty() || !ancestors.add(placement.definition().id())) {
            return placementNode(
                    placement,
                    entryState,
                    occurrence,
                    placement
                            .name()
                            .<AuthoringText>map(AuthoringText::literal)
                            .orElseGet(() -> AuthoringText.message(
                                    "editor.hierarchy.unavailable-definition", "Unavailable definition")),
                    List.of());
        }
        EntityDefinition definition = loaded.orElseThrow();
        URI definitionSource = result.source();
        List<EditorHierarchyNode> children = definition.root().children().stream()
                .map(child -> projectEntry(child, definitionSource, ancestors, true, false, occurrence, state))
                .toList();
        ancestors.remove(placement.definition().id());
        AuthoringText label = AuthoringText.literal(
                placement.name().orElseGet(() -> definition.root().name().orElse(definition.name())));
        return placementNode(placement, entryState, occurrence, label, children);
    }

    private static EditorHierarchyNode placementNode(
            EntityPlacement placement,
            EntryProjectionState entryState,
            HierarchyOccurrenceId occurrence,
            AuthoringText label,
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
                        entryState.editable()
                                && !entryState.generated()
                                && entryState.projection().modified().contains(placement.id()),
                        entryState.generated() || !entryState.editable()
                                ? Optional.empty()
                                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, placement.id()))),
                new InspectorTarget(
                        InspectorTarget.Kind.PLACEMENT,
                        entryState.source(),
                        placement.id().toString(),
                        Optional.of(occurrence)),
                children);
    }

    /** Validates a logical source without assuming it is a filesystem path. */
    private static URI requireAbsolute(URI source) {
        URI validSource = Objects.requireNonNull(source, "source").normalize();
        if (!validSource.isAbsolute()) {
            throw new IllegalArgumentException("source must be absolute");
        }
        return validSource;
    }

    /** Values that vary for each hierarchy refresh. */
    private record ProjectionState(Set<EntityId> modified) {}

    /** Values inherited by one projected entry. */
    private record EntryProjectionState(URI source, boolean generated, boolean editable, ProjectionState projection) {}

    /** Stable services used by all hierarchy refreshes. */
    private record ProjectionContext(
            DefinitionResolver definitions, RegisteredTypeCatalog types, Collection<ProjectDiagnostic> diagnostics) {
        private ProjectionContext {
            Objects.requireNonNull(definitions, "definitions");
            Objects.requireNonNull(types, "types");
            Objects.requireNonNull(diagnostics, "diagnostics");
        }
    }
}
