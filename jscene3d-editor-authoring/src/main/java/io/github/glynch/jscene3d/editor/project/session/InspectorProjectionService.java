/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProjector;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProperty;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorSection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Resolves retained hierarchy occurrences and projects authoritative Inspector data. */
final class InspectorProjectionService {
    private final RetainedDefinitionRegistry retainedDefinitions;
    private final DefinitionResolver definitions;
    private final RegisteredTypeCatalog types;
    private final List<ProjectAsset> assets;

    InspectorProjectionService(
            RetainedDefinitionRegistry retainedDefinitions,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            List<ProjectAsset> assets) {
        this.retainedDefinitions = Objects.requireNonNull(retainedDefinitions, "retainedDefinitions");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.types = Objects.requireNonNull(types, "types");
        this.assets = List.copyOf(assets);
    }

    InspectorProjection inspect(InspectorTarget target, long expectedRevision) {
        InspectorTarget validTarget = Objects.requireNonNull(target, "target");
        HierarchyOccurrenceId occurrence = validTarget
                .occurrence()
                .orElseThrow(() -> new IllegalArgumentException("Inspector target must have a hierarchy occurrence"));
        EditorRetainedDefinition current = retainedDefinitions
                .snapshot(occurrence.definition())
                .orElseThrow(() ->
                        new IllegalStateException("Inspector definition is not retained: " + occurrence.definition()));
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
        InspectorProjection.DefinitionOrigin origin = current.origin() == EditorRetainedDefinition.Origin.AUTHORED
                ? InspectorProjection.DefinitionOrigin.AUTHORED
                : InspectorProjection.DefinitionOrigin.GENERATED;
        EditorInspectorProjector.Context context = new EditorInspectorProjector.Context(
                validTarget,
                origin,
                provenance,
                node.isEditable(),
                types,
                resolved.scopeRoots(),
                resolved.scopeOccurrence(),
                assets);
        InspectorProjection projection =
                switch (resolved.entry()) {
                    case LocalEntity entity -> EditorInspectorProjector.entity(entity, context);
                    case EntityPlacement placement ->
                        EditorInspectorProjector.placement(placement, resolvePlacementDefinition(placement), context);
                };
        return retainedDefinitions
                .workingCopy(occurrence.definition())
                .map(workingCopy -> withModificationState(projection, workingCopy))
                .orElse(projection);
    }

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

    private Optional<EntityDefinition> resolvePlacementDefinition(EntityPlacement placement) {
        return definitions.loadEntity(placement.definition(), types).definition();
    }

    private static InspectorProjection withModificationState(
            InspectorProjection projection, AuthoredDefinitionWorkingCopy workingCopy) {
        List<InspectorSection> sections = projection.sections().stream()
                .map(section -> new InspectorSection(
                        section.identity(),
                        section.kind(),
                        section.label(),
                        section.description(),
                        section.componentId(),
                        section.componentType(),
                        section.metadataAvailable(),
                        section.editable(),
                        section.properties().stream()
                                .map(property -> withModificationState(property, workingCopy))
                                .toList()))
                .toList();
        return new InspectorProjection(
                projection.target(),
                projection.title(),
                projection.definitionOrigin(),
                projection.provenance(),
                projection.editable(),
                sections);
    }

    private static InspectorProperty withModificationState(
            InspectorProperty property, AuthoredDefinitionWorkingCopy workingCopy) {
        boolean modified =
                property.mutationTarget().map(workingCopy::isModified).orElse(false);
        InspectorProperty.State state = property.state();
        return new InspectorProperty(
                property.identity(),
                property.presentation(),
                new InspectorProperty.State(
                        state.authoredValue(),
                        state.defaultValue(),
                        state.effectiveValue(),
                        state.origin(),
                        state.validity(),
                        state.editable(),
                        modified),
                property.mutationTarget());
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

    private static List<EntityEntry> scopeRoots(EditorRetainedDefinition.Content content) {
        return switch (content) {
            case EditorRetainedDefinition.Content.Scene scene ->
                scene.definition().roots();
            case EditorRetainedDefinition.Content.Entity entity ->
                List.of(entity.definition().root());
        };
    }

    private static void requireRevision(long currentRevision, long expectedRevision) {
        if (expectedRevision != currentRevision) {
            throw new IllegalStateException("stale authoring revision: expected " + expectedRevision
                    + " but current revision is " + currentRevision);
        }
    }

    private record ResolvedInspectorEntry(
            EntityEntry entry, boolean generated, List<EntityEntry> scopeRoots, HierarchyOccurrenceId scopeOccurrence) {
        private ResolvedInspectorEntry {
            Objects.requireNonNull(entry, "entry");
            scopeRoots = List.copyOf(scopeRoots);
            Objects.requireNonNull(scopeOccurrence, "scopeOccurrence");
        }
    }
}
