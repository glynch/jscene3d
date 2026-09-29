/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Projects validated domain data into immutable typed Inspector data. */
public final class EditorInspectorProjector {
    private EditorInspectorProjector() {}

    /** Projects one local or generated entity and its descriptor-backed components.
     *
     * @param entity entity to project
     * @param context projection context and current editability
     * @return typed entity Inspector projection
     */
    public static InspectorProjection entity(LocalEntity entity, Context context) {
        InspectorTarget target = context.target();
        Optional<EditorComponentSectionProjector.EditableEntity> editable = context.editable()
                ? Optional.of(new EditorComponentSectionProjector.EditableEntity(
                        target.occurrence().orElseThrow(), entity.id()))
                : Optional.empty();
        Optional<InspectorMutationTarget> enabledMutation = context.editable()
                ? Optional.of(new InspectorMutationTarget.EntityEnabled(
                        target.occurrence().orElseThrow(), entity.id()))
                : Optional.empty();
        InspectorValueProjector values = context.values();
        List<InspectorSection> sections = new ArrayList<>();
        sections.add(new InspectorSection(
                "entity",
                InspectorSection.Kind.ENTITY,
                message("editor.inspector.section.entity", "Entity"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                context.editable(),
                List.of(property(
                        "enabled",
                        message("editor.inspector.property.enabled", "Enabled"),
                        new ProjectValue.BooleanValue(entity.isEnabled()),
                        enabledMutation,
                        values))));
        sections.addAll(EditorComponentSectionProjector.componentSections(
                editable, entity.components(), context.types(), values));
        AuthoringText title = entity.name()
                .<AuthoringText>map(AuthoringText::literal)
                .orElseGet(() -> AuthoringText.message("editor.hierarchy.unnamed-entity", "Unnamed entity"));
        return new InspectorProjection(
                target, title, context.definitionOrigin(), context.provenance(), context.editable(), sections);
    }

    /** Projects a reusable-definition placement and its realized root components.
     *
     * @param placement placement to project
     * @param definition resolved reusable definition when available
     * @param context projection context and current editability
     * @return typed placement Inspector projection
     */
    public static InspectorProjection placement(
            EntityPlacement placement, Optional<EntityDefinition> definition, Context context) {
        AuthoringText title = placement
                .name()
                .<AuthoringText>map(AuthoringText::literal)
                .orElseGet(() -> definition
                        .map(EntityDefinition::name)
                        .<AuthoringText>map(AuthoringText::literal)
                        .orElseGet(() -> AuthoringText.message(
                                "editor.hierarchy.unavailable-definition", "Unavailable definition")));
        InspectorTarget target = context.target();
        Optional<InspectorMutationTarget> enabledMutation = context.editable()
                ? Optional.of(new InspectorMutationTarget.EntityEnabled(
                        target.occurrence().orElseThrow(), placement.id()))
                : Optional.empty();
        InspectorValueProjector values = context.values();
        List<InspectorProperty> placementProperties = new ArrayList<>();
        placementProperties.add(property(
                "enabled",
                message("editor.inspector.property.enabled", "Enabled"),
                new ProjectValue.BooleanValue(placement.isEnabled()),
                enabledMutation,
                values));
        List<InspectorSection> sections = new ArrayList<>();
        sections.add(new InspectorSection(
                "placement",
                InspectorSection.Kind.PLACEMENT,
                message("editor.inspector.section.placement", "Placement"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                context.editable(),
                placementProperties));
        definition.ifPresent(value -> sections.addAll(EditorComponentSectionProjector.componentSections(
                Optional.empty(), value.root().components(), context.types(), values)));
        return new InspectorProjection(
                target, title, context.definitionOrigin(), context.provenance(), context.editable(), sections);
    }

    private static InspectorProperty property(
            String identity,
            AuthoringText label,
            ProjectValue value,
            Optional<InspectorMutationTarget> mutationTarget,
            InspectorValueProjector values) {
        InspectorValue projected = values.project(value);
        return new InspectorProperty(
                identity,
                new InspectorProperty.Presentation(
                        label, ProjectValueKind.of(value), false, Optional.empty(), InspectorConstraints.empty()),
                new InspectorProperty.State(
                        Optional.of(projected),
                        Optional.empty(),
                        Optional.of(projected),
                        InspectorProperty.Origin.AUTHORED,
                        InspectorProperty.Validity.VALID,
                        mutationTarget.isPresent(),
                        false),
                mutationTarget);
    }

    private static AuthoringText message(String code, String defaultMessage) {
        return AuthoringText.message(code, defaultMessage);
    }

    /** Cohesive current state used to project one selected occurrence.
     *
     * @param target selected Java-issued semantic target
     * @param definitionOrigin origin of the retained definition
     * @param provenance provenance of the selected occurrence
     * @param editable whether the selected target currently accepts supported mutations
     * @param types registered descriptor catalog
     * @param scopeRoots locally authored roots used for target resolution
     * @param scopeOccurrence occurrence prefix for the authored scope
     * @param assets current project assets used for reference resolution
     */
    public record Context(
            InspectorTarget target,
            InspectorProjection.DefinitionOrigin definitionOrigin,
            InspectorProjection.Provenance provenance,
            boolean editable,
            RegisteredTypeCatalog types,
            List<EntityEntry> scopeRoots,
            HierarchyOccurrenceId scopeOccurrence,
            List<ProjectAsset> assets) {
        /** Validates and prepares one complete projection context. */
        public Context {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(definitionOrigin, "definitionOrigin");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(types, "types");
            scopeRoots = List.copyOf(scopeRoots);
            Objects.requireNonNull(scopeOccurrence, "scopeOccurrence");
            assets = List.copyOf(assets);
            if (target.occurrence().isEmpty()) {
                throw new IllegalArgumentException("entity and placement inspection requires an occurrence");
            }
        }

        /** Creates the semantic value projector sharing this occurrence's authored scope. */
        private InspectorValueProjector values() {
            return new InspectorValueProjector(scopeRoots, scopeOccurrence, types, assets);
        }
    }
}
