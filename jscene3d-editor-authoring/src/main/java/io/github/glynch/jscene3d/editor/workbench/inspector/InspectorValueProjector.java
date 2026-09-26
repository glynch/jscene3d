/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.net.URI;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Resolves portable project values into current semantic Inspector presentation. */
final class InspectorValueProjector {
    private final List<EntityEntry> scopeRoots;
    private final HierarchyOccurrenceId scope;
    private final RegisteredTypeCatalog types;
    private final List<ProjectAsset> assets;

    /** Stores the authored scope and catalogs used by one complete Inspector snapshot. */
    InspectorValueProjector(
            List<EntityEntry> scopeRoots,
            HierarchyOccurrenceId scope,
            RegisteredTypeCatalog types,
            List<ProjectAsset> assets) {
        this.scopeRoots = List.copyOf(scopeRoots);
        this.scope = Objects.requireNonNull(scope, "scope");
        this.types = Objects.requireNonNull(types, "types");
        this.assets = List.copyOf(assets);
    }

    /** Projects one value recursively without erasing semantic reference kinds. */
    InspectorValue project(ProjectValue value) {
        return switch (Objects.requireNonNull(value, "value")) {
            case ProjectValue.NullValue ignored -> InspectorValue.NullValue.INSTANCE;
            case ProjectValue.BooleanValue booleanValue -> new InspectorValue.BooleanValue(booleanValue.value());
            case ProjectValue.NumberValue number -> new InspectorValue.NumberValue(number.value());
            case ProjectValue.TextValue text -> new InspectorValue.TextValue(text.value());
            case ProjectValue.ArrayValue array ->
                new InspectorValue.ArrayValue(
                        array.values().stream().map(this::project).toList());
            case ProjectValue.ObjectValue object -> projectObject(object);
            case ProjectValue.ReferenceValue reference -> projectReference(reference.reference());
            case ProjectValue.EntityTargetValue target -> projectEntityTarget(target.entity());
            case ProjectValue.ComponentTargetValue target -> projectComponentTarget(target.target());
        };
    }

    /** Projects an ordered object without claiming a member schema. */
    private InspectorValue.ObjectValue projectObject(ProjectValue.ObjectValue object) {
        LinkedHashMap<String, InspectorValue> values = new LinkedHashMap<>();
        object.values().forEach((key, value) -> values.put(key, project(value)));
        return new InspectorValue.ObjectValue(values);
    }

    /** Resolves current display and reveal information for one resource reference. */
    private InspectorValue.ReferenceValue projectReference(ResourceReference reference) {
        if (reference.kind() == ResourceReference.Kind.PROJECT) {
            URI reveal = reference.projectPath().orElseThrow().toUri();
            InspectorValue.Resolution resolution =
                    Files.exists(reference.projectPath().orElseThrow())
                            ? InspectorValue.Resolution.RESOLVED
                            : InspectorValue.Resolution.BROKEN;
            return new InspectorValue.ReferenceValue(
                    reference, AuthoringText.literal(reference.locator()), resolution, Optional.of(reveal));
        }
        String identity = reference.kind() == ResourceReference.Kind.IMPORT
                ? reference.locator().substring(0, reference.locator().indexOf('/'))
                : reference.locator();
        Optional<ProjectAsset> asset = assets.stream()
                .filter(candidate -> candidate.identity().equals(identity))
                .findFirst();
        return new InspectorValue.ReferenceValue(
                reference,
                AuthoringText.literal(asset.map(ProjectAsset::label).orElse(reference.locator())),
                asset.isPresent() ? InspectorValue.Resolution.RESOLVED : InspectorValue.Resolution.BROKEN,
                asset.map(value -> value.source().toUri()));
    }

    /** Resolves one target to its authored label and occurrence in the containing definition. */
    private InspectorValue.EntityTargetValue projectEntityTarget(EntityId target) {
        Optional<ScopedEntity> resolved = findEntity(scopeRoots, target, scope);
        return new InspectorValue.EntityTargetValue(
                target,
                resolved.map(ScopedEntity::label).orElseGet(() -> AuthoringText.literal(target.toString())),
                resolved.isPresent() ? InspectorValue.Resolution.RESOLVED : InspectorValue.Resolution.BROKEN,
                resolved.map(ScopedEntity::occurrence));
    }

    /** Resolves one component target without asking TypeScript to scan definition snapshots. */
    private InspectorValue.ComponentTargetValue projectComponentTarget(ComponentTarget target) {
        Optional<ScopedEntity> entity = findEntity(scopeRoots, target.entity(), scope);
        Optional<ComponentDefinition> component = entity.flatMap(value -> value.entity().components().stream()
                .filter(candidate -> candidate.id().equals(target.component()))
                .findFirst());
        Optional<ComponentType> componentType =
                component.map(value -> new ComponentType(value.type(), value.typeVersion()));
        AuthoringText componentLabel = componentType
                .flatMap(types::findComponent)
                .map(value -> AuthoringText.literal(value.presentation().displayName()))
                .orElseGet(() -> AuthoringText.literal(target.component().toString()));
        return new InspectorValue.ComponentTargetValue(
                target,
                entity.map(ScopedEntity::label)
                        .orElseGet(() -> AuthoringText.literal(target.entity().toString())),
                componentLabel,
                componentType,
                component.isPresent() ? InspectorValue.Resolution.RESOLVED : InspectorValue.Resolution.BROKEN,
                entity.map(ScopedEntity::occurrence));
    }

    /** Finds a locally authored entity without traversing into referenced definitions. */
    private static Optional<ScopedEntity> findEntity(
            List<EntityEntry> entries, EntityId target, HierarchyOccurrenceId parent) {
        for (EntityEntry entry : entries) {
            HierarchyOccurrenceId occurrence = parent.child(entry.id());
            if (entry instanceof LocalEntity local) {
                if (local.id().equals(target)) {
                    AuthoringText label = local.name()
                            .<AuthoringText>map(AuthoringText::literal)
                            .orElseGet(
                                    () -> AuthoringText.message("editor.hierarchy.unnamed-entity", "Unnamed entity"));
                    return Optional.of(new ScopedEntity(local, label, occurrence));
                }
                Optional<ScopedEntity> child = findEntity(local.children(), target, occurrence);
                if (child.isPresent()) {
                    return child;
                }
            }
        }
        return Optional.empty();
    }

    /** Resolved authored entity and its occurrence-safe display context. */
    private record ScopedEntity(LocalEntity entity, AuthoringText label, HierarchyOccurrenceId occurrence) {}
}
