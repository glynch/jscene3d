/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Projects validated domain data into immutable typed Inspector data. */
public final class EditorInspectorProjector {
    private EditorInspectorProjector() {}

    /** Projects the opened world root.
     *
     * @param world current world definition
     * @param source authoritative world source
     * @param occurrence world-root occurrence identity
     * @return typed world Inspector projection
     */
    public static InspectorProjection world(WorldDefinition world, Path source, HierarchyOccurrenceId occurrence) {
        InspectorTarget target = new InspectorTarget(
                InspectorTarget.Kind.WORLD, source, world.id().toString(), Optional.of(occurrence));
        return new InspectorProjection(
                target,
                AuthoringText.literal(world.name()),
                false,
                false,
                List.of(new InspectorSection(
                        "world",
                        message("editor.inspector.section.world", "World"),
                        Optional.empty(),
                        Optional.empty(),
                        true,
                        List.of(
                                summary(
                                        "asset-id",
                                        message("editor.inspector.property.asset-id", "Asset ID"),
                                        new ProjectValue.TextValue(world.id().toString())),
                                summary(
                                        "root-count",
                                        message("editor.inspector.property.root-entities", "Root entities"),
                                        number(world.roots().size())),
                                summary(
                                        "connection-count",
                                        message("editor.inspector.property.connections", "Connections"),
                                        number(world.connections().size()))))));
    }

    /** Projects one local or generated entity and its descriptor-backed components.
     *
     * @param entity entity to project
     * @param source authored source containing the entity
     * @param types registered component descriptor catalog
     * @param occurrence occurrence-safe hierarchy identity
     * @param generated whether the entity originates in a placed definition
     * @return typed entity Inspector projection
     */
    public static InspectorProjection entity(
            LocalEntity entity,
            Path source,
            RegisteredTypeCatalog types,
            HierarchyOccurrenceId occurrence,
            boolean generated) {
        InspectorTarget.Kind kind =
                generated ? InspectorTarget.Kind.GENERATED_ENTITY : InspectorTarget.Kind.LOCAL_ENTITY;
        InspectorTarget target = new InspectorTarget(kind, source, entity.id().toString(), Optional.of(occurrence));
        Optional<EditorComponentSectionProjector.EditableEntity> editable = generated
                ? Optional.empty()
                : Optional.of(new EditorComponentSectionProjector.EditableEntity(occurrence, entity.id()));
        Optional<InspectorMutationTarget> enabledMutation = generated
                ? Optional.empty()
                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, entity.id()));
        List<InspectorSection> sections = new ArrayList<>();
        sections.add(new InspectorSection(
                "entity",
                message("editor.inspector.section.entity", "Entity"),
                Optional.empty(),
                Optional.empty(),
                true,
                List.of(
                        property(
                                "enabled",
                                message("editor.inspector.property.enabled", "Enabled"),
                                new ProjectValue.BooleanValue(entity.isEnabled()),
                                enabledMutation),
                        summary(
                                "child-count",
                                message("editor.inspector.property.children", "Children"),
                                number(entity.children().size())))));
        sections.addAll(EditorComponentSectionProjector.componentSections(editable, entity.components(), types));
        AuthoringText title = entity.name()
                .<AuthoringText>map(AuthoringText::literal)
                .orElseGet(() -> AuthoringText.message("editor.hierarchy.unnamed-entity", "Unnamed entity"));
        return new InspectorProjection(target, title, generated, !generated, sections);
    }

    /** Projects a reusable-definition placement and its realized root components.
     *
     * @param placement placement to project
     * @param definition resolved reusable definition when available
     * @param source authored source containing the placement
     * @param types registered component descriptor catalog
     * @param occurrence occurrence-safe hierarchy identity
     * @param generated whether the placement originates in another placed definition
     * @return typed placement Inspector projection
     */
    public static InspectorProjection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            RegisteredTypeCatalog types,
            HierarchyOccurrenceId occurrence,
            boolean generated) {
        AuthoringText title = placement
                .name()
                .<AuthoringText>map(AuthoringText::literal)
                .orElseGet(() -> definition
                        .map(EntityDefinition::name)
                        .<AuthoringText>map(AuthoringText::literal)
                        .orElseGet(() -> AuthoringText.message(
                                "editor.hierarchy.unavailable-definition", "Unavailable definition")));
        InspectorTarget target = new InspectorTarget(
                InspectorTarget.Kind.PLACEMENT, source, placement.id().toString(), Optional.of(occurrence));
        Optional<InspectorMutationTarget> enabledMutation = generated
                ? Optional.empty()
                : Optional.of(new InspectorMutationTarget.EntityEnabled(occurrence, placement.id()));
        List<InspectorProperty> placementProperties = new ArrayList<>();
        placementProperties.add(summary(
                "definition",
                message("editor.inspector.property.definition", "Definition"),
                new ProjectValue.TextValue(placement.definition().id().toString())));
        placementProperties.add(property(
                "enabled",
                message("editor.inspector.property.enabled", "Enabled"),
                new ProjectValue.BooleanValue(placement.isEnabled()),
                enabledMutation));
        placement
                .arguments()
                .forEach((id, value) ->
                        placementProperties.add(summary(id.value(), AuthoringText.literal(id.value()), value)));
        List<InspectorSection> sections = new ArrayList<>();
        sections.add(new InspectorSection(
                "placement",
                message("editor.inspector.section.placement", "Placement"),
                Optional.empty(),
                Optional.empty(),
                true,
                placementProperties));
        definition.ifPresent(value -> sections.addAll(EditorComponentSectionProjector.componentSections(
                Optional.empty(), value.root().components(), types)));
        return new InspectorProjection(target, title, generated, !generated, sections);
    }

    private static ProjectValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    private static InspectorProperty summary(String identity, AuthoringText label, ProjectValue value) {
        return property(identity, label, value, Optional.empty());
    }

    private static InspectorProperty property(
            String identity,
            AuthoringText label,
            ProjectValue value,
            Optional<InspectorMutationTarget> mutationTarget) {
        return new InspectorProperty(
                identity,
                new InspectorProperty.Presentation(
                        label, ProjectValueKind.of(value), false, Optional.empty(), InspectorConstraints.empty()),
                new InspectorProperty.State(Optional.of(value), InspectorProperty.Origin.AUTHORED),
                mutationTarget);
    }

    private static AuthoringText message(String code, String defaultMessage) {
        return AuthoringText.message(code, defaultMessage);
    }
}
