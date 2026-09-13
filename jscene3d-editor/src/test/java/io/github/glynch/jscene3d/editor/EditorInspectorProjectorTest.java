/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies safe descriptor-to-Inspector projection independently of JavaFX. */
final class EditorInspectorProjectorTest {
    private static final String ENTITY_ID = "0b295328-b5a3-4f41-9f34-e9b4abc430a7";
    private static final String COMPONENT_ID = "3e940be7-e58d-4f3a-8b5e-e61c99c00904";
    private static final ComponentType COMPONENT_TYPE = ComponentType.of("example.inspector/mover", 1);

    /** Uses descriptor display names, authored values, defaults, descriptions, and constraints. */
    @Test
    void projectsDescriptorBackedComponentProperties() {
        RegisteredTypeCatalog types = catalog();
        LocalEntity entity = entity(COMPONENT_TYPE.id());

        EditorDetails inspection = EditorInspectorProjector.entity(
                        entity, Path.of("/project/worlds/map01.world.json"), Path.of("/project"), types, false)
                .details()
                .orElseThrow();

        assertThat(inspection)
                .returns("Player", EditorDetails::title)
                .returns("Local entity", EditorDetails::kind)
                .returns("worlds/map01.world.json", EditorDetails::source);
        assertThat(inspection.decorations()).containsExactly(new EditorIcon(EditorIcons.READ_ONLY, "Read-only"));
        EditorDetails.Section component = inspection.sections().get(1);
        assertThat(component)
                .returns("Movement", EditorDetails.Section::title)
                .returns(true, EditorDetails.Section::metadataAvailable);
        assertThat(component.properties())
                .extracting(EditorDetails.Property::displayName)
                .containsExactly("Speed", "Shape");
        assertThat(component.properties().get(0))
                .returns("2.5", EditorDetails.Property::value)
                .returns(EditorDetails.ValueOrigin.DEFAULT, EditorDetails.Property::origin)
                .satisfies(property -> assertThat(property.constraints()).containsEntry("minimum", "0"));
        assertThat(component.properties().get(1))
                .returns("asset:player-capsule", EditorDetails.Property::value)
                .returns(EditorDetails.ValueOrigin.AUTHORED, EditorDetails.Property::origin)
                .satisfies(property -> assertThat(property.constraints())
                        .containsEntry(PropertyDescriptorKeys.ACCEPTED_REFERENCE_KINDS, "asset"));
    }

    /** Retains authored values and visibly marks a missing exact component descriptor. */
    @Test
    void projectsAuthoredValuesWhenMetadataIsUnavailable() {
        LocalEntity entity = entity(new ComponentTypeId("example.inspector/missing"));

        EditorDetails inspection = EditorInspectorProjector.entity(
                        entity,
                        Path.of("/project/worlds/map01.world.json"),
                        Path.of("/project"),
                        RegisteredTypeCatalog.of(List.of()),
                        true)
                .details()
                .orElseThrow();

        EditorDetails.Section component = inspection.sections().get(1);
        assertThat(component.metadataAvailable()).isFalse();
        assertThat(component.description())
                .hasValueSatisfying(description -> assertThat(description).contains("metadata unavailable"));
        assertThat(component.properties())
                .singleElement()
                .returns("asset:player-capsule", EditorDetails.Property::value);
        assertThat(inspection.decorations())
                .containsExactly(new EditorIcon(EditorIcons.READ_ONLY, "Generated content · read-only"));
    }

    /** Copies section and property collections so loaded state cannot be mutated through the Inspector. */
    @Test
    void producesImmutableInspectorData() {
        List<EditorDetails.Section> sections = new ArrayList<>();
        EditorDetails inspection = new EditorDetails("Player", "Local entity", "world", ENTITY_ID, List.of(), sections);

        sections.add(new EditorDetails.Section("Late", Optional.empty(), true, List.of()));

        assertThat(inspection.sections()).isEmpty();
        EditorDetails.Section lateSection = sections.getFirst();
        List<EditorDetails.Section> immutableSections = inspection.sections();
        assertThatThrownBy(() -> immutableSections.add(lateSection)).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Creates a local entity with one authored resource-reference property. */
    private static LocalEntity entity(ComponentTypeId componentType) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                new PropertyId("shape"), new ProjectValue.ReferenceValue(ResourceReference.asset("player-capsule")));
        ComponentDefinition component =
                new ComponentDefinition(ComponentId.from(COMPONENT_ID), componentType, 1, properties);
        return new LocalEntity(EntityId.from(ENTITY_ID), "Player", true, List.of(component), List.of());
    }

    /** Creates exact safe metadata without any runtime implementation class. */
    private static RegisteredTypeCatalog catalog() {
        PropertyDescriptor speed = PropertyDescriptor.optionalWithDefault(
                "speed",
                ProjectValueKind.NUMBER,
                new ProjectValue.NumberValue(new BigDecimal("2.5")),
                DescriptorPresentation.described("Speed", "Movement speed per second"),
                Map.of("minimum", new ProjectValue.NumberValue(BigDecimal.ZERO)),
                Set.of());
        PropertyDescriptor shape = PropertyDescriptor.required(
                "shape",
                ProjectValueKind.REFERENCE,
                DescriptorPresentation.named("Shape"),
                Map.of(),
                Set.of(ResourceReference.Kind.ASSET));
        ComponentTypeDescriptor component = ComponentTypeDescriptor.builder(
                        COMPONENT_TYPE, DescriptorPresentation.described("Movement", "Moves the selected entity"))
                .properties(List.of(speed, shape))
                .build();
        ExtensionDescriptor extension = new ExtensionDescriptor(
                "example.inspector",
                "1.0.0",
                ">=0.1.0 <1.0.0",
                DescriptorPresentation.named("Inspector Test"),
                List.of(),
                List.of(component));
        return RegisteredTypeCatalog.of(List.of(extension));
    }
}
