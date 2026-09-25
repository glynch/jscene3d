/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
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

/** Verifies typed target-scoped Inspector projection independently of any editor UI. */
final class EditorInspectorProjectorTest {
    private static final AssetId WORLD_ID = AssetId.from("3b406aba-26fb-4681-9abe-7a952c321f9a");
    private static final EntityId ENTITY_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");
    private static final ComponentId COMPONENT_ID = ComponentId.from("3e940be7-e58d-4f3a-8b5e-e61c99c00904");
    private static final ComponentType COMPONENT_TYPE = ComponentType.of("example.inspector/mover", 1);
    private static final Path SOURCE = Path.of("/project/worlds/map01.world.json");

    /** Retains typed authored/default values, provenance, required state, and structured constraints. */
    @Test
    void projectsDescriptorBackedComponentProperties() {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));

        InspectorProjection inspection =
                EditorInspectorProjector.entity(entity(), SOURCE, catalog(), occurrence, false);

        assertThat(inspection)
                .returns(AuthoringText.literal("Player"), InspectorProjection::title)
                .returns(false, InspectorProjection::generated)
                .returns(true, InspectorProjection::editable);
        InspectorSection component = inspection.sections().get(1);
        assertThat(component)
                .returns(AuthoringText.literal("Movement"), InspectorSection::label)
                .returns(Optional.of(COMPONENT_TYPE), InspectorSection::componentType)
                .returns(true, InspectorSection::metadataAvailable);
        InspectorProperty speed = component.properties().get(0);
        assertThat(speed.state())
                .returns(
                        Optional.of(new ProjectValue.NumberValue(new BigDecimal("2.5"))),
                        InspectorProperty.State::value)
                .returns(InspectorProperty.Origin.DEFAULT, InspectorProperty.State::origin);
        assertThat(speed.presentation().constraints().semanticMetadata())
                .containsEntry("minimum", new ProjectValue.NumberValue(BigDecimal.ZERO));
        InspectorProperty shape = component.properties().get(1);
        assertThat(shape.state().origin()).isEqualTo(InspectorProperty.Origin.AUTHORED);
        assertThat(shape.presentation().required()).isTrue();
        assertThat(shape.presentation().constraints().acceptedReferenceKinds())
                .containsExactly(ResourceReference.Kind.ASSET);
        assertThat(shape.mutationTarget())
                .contains(new InspectorMutationTarget.ComponentProperty(
                        occurrence, ENTITY_ID, COMPONENT_ID, new PropertyId("shape")));
    }

    /** Removes all mutation targets from generated content while preserving typed values. */
    @Test
    void projectsGeneratedContentAsReadOnly() {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));

        InspectorProjection inspection = EditorInspectorProjector.entity(entity(), SOURCE, catalog(), occurrence, true);

        assertThat(inspection.generated()).isTrue();
        assertThat(inspection.editable()).isFalse();
        assertThat(inspection.sections())
                .flatExtracting(InspectorSection::properties)
                .extracting(InspectorProperty::mutationTarget)
                .containsOnly(Optional.empty());
    }

    /** Keeps semantic metadata typed instead of choosing or invoking a Java property editor. */
    @Test
    void retainsVectorSemanticsWithoutCallbacksOrStringParsing() {
        PropertyId location = new PropertyId("location");
        PropertyDescriptor descriptor = PropertyDescriptor.optionalArrayWithDefault(
                location.value(),
                ProjectValueKind.NUMBER,
                3,
                new ProjectValue.ArrayValue(List.of(
                        new ProjectValue.NumberValue(BigDecimal.ZERO),
                        new ProjectValue.NumberValue(BigDecimal.ZERO),
                        new ProjectValue.NumberValue(BigDecimal.ZERO))),
                DescriptorPresentation.named("Location"),
                Map.of(
                        PropertyDescriptorKeys.EDITOR_SEMANTIC,
                        new ProjectValue.TextValue(PropertyEditorSemantics.VECTOR3)));
        ComponentTypeDescriptor component = ComponentTypeDescriptor.builder(
                        COMPONENT_TYPE, DescriptorPresentation.named("Movement"))
                .properties(List.of(descriptor))
                .build();
        RegisteredTypeCatalog types = types(component);
        LocalEntity entity = new LocalEntity(
                ENTITY_ID,
                "Player",
                true,
                List.of(new ComponentDefinition(COMPONENT_ID, COMPONENT_TYPE.id(), 1, Map.of())),
                List.of());

        InspectorProperty property = EditorInspectorProjector.entity(
                        entity, SOURCE, types, new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID)), false)
                .sections()
                .get(1)
                .properties()
                .getFirst();

        assertThat(property.presentation().constraints().exactElementCount()).contains(3);
        assertThat(property.presentation().constraints().elementKind()).contains(ProjectValueKind.NUMBER);
        assertThat(property.presentation().constraints().semanticMetadata())
                .containsEntry(
                        PropertyDescriptorKeys.EDITOR_SEMANTIC,
                        new ProjectValue.TextValue(PropertyEditorSemantics.VECTOR3));
    }

    /** Copies section collections so callers cannot mutate projected state. */
    @Test
    void producesImmutableInspectorData() {
        List<InspectorSection> sections = new ArrayList<>();
        InspectorProjection inspection = new InspectorProjection(
                new InspectorTarget(InspectorTarget.Kind.ASSET, SOURCE, "asset", Optional.empty()),
                AuthoringText.literal("Asset"),
                false,
                false,
                sections);
        InspectorSection value = new InspectorSection(
                "late", AuthoringText.literal("Late"), Optional.empty(), Optional.empty(), true, List.of());
        List<InspectorSection> immutableSections = inspection.sections();

        assertThatThrownBy(() -> immutableSections.add(value)).isInstanceOf(UnsupportedOperationException.class);
    }

    private static LocalEntity entity() {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                new PropertyId("shape"), new ProjectValue.ReferenceValue(ResourceReference.asset("player-capsule")));
        ComponentDefinition component =
                new ComponentDefinition(COMPONENT_ID, COMPONENT_TYPE.id(), COMPONENT_TYPE.version(), properties);
        return new LocalEntity(ENTITY_ID, "Player", true, List.of(component), List.of());
    }

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
                        COMPONENT_TYPE, DescriptorPresentation.described("Movement", "Moves the entity"))
                .properties(List.of(speed, shape))
                .build();
        return types(component);
    }

    private static RegisteredTypeCatalog types(ComponentTypeDescriptor component) {
        return RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                "example.inspector",
                "1.0.0",
                ">=0.1.0 <1.0.0",
                DescriptorPresentation.named("Inspector Test"),
                List.of(),
                List.of(component))));
    }
}
