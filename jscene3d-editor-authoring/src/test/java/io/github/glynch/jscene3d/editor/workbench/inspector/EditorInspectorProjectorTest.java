/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantic;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
import io.github.glynch.jscene3d.project.extension.PropertyNumericBound;
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
    private static final ComponentId UNKNOWN_COMPONENT_ID = ComponentId.from("f4d181e0-b05a-4e6f-96fa-56db4774de70");
    private static final ComponentType COMPONENT_TYPE = ComponentType.of("example.inspector/mover", 1);
    private static final Path SOURCE = Path.of("/project/worlds/map01.scene.json");

    /** Retains typed authored/default values, provenance, required state, and structured constraints. */
    @Test
    void projectsDescriptorBackedComponentProperties() {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));

        LocalEntity entity = entity();
        InspectorProjection inspection =
                EditorInspectorProjector.entity(entity, context(entity, catalog(), occurrence, true, false));

        assertThat(inspection)
                .returns(AuthoringText.literal("Player"), InspectorProjection::title)
                .returns(InspectorProjection.Provenance.LOCAL, InspectorProjection::provenance)
                .returns(true, InspectorProjection::editable);
        InspectorSection component = inspection.sections().get(1);
        assertThat(component)
                .returns(AuthoringText.literal("Movement"), InspectorSection::label)
                .returns(Optional.of(COMPONENT_TYPE), InspectorSection::componentType)
                .returns(true, InspectorSection::metadataAvailable);
        InspectorProperty speed = component.properties().get(0);
        assertThat(speed.state())
                .returns(
                        Optional.of(new InspectorValue.NumberValue(new BigDecimal("3.5"))),
                        InspectorProperty.State::effectiveValue)
                .returns(
                        Optional.of(new InspectorValue.NumberValue(new BigDecimal("3.5"))),
                        InspectorProperty.State::authoredValue)
                .returns(
                        Optional.of(new InspectorValue.NumberValue(new BigDecimal("2.5"))),
                        InspectorProperty.State::defaultValue)
                .returns(InspectorProperty.Origin.AUTHORED, InspectorProperty.State::origin);
        assertThat(speed.presentation().constraints().editor().minimum())
                .contains(new PropertyNumericBound(BigDecimal.ZERO, true));
        InspectorProperty shape = component.properties().get(1);
        assertThat(shape.state().origin()).isEqualTo(InspectorProperty.Origin.AUTHORED);
        assertThat(shape.presentation().required()).isTrue();
        assertThat(shape.presentation().constraints().acceptedReferenceKinds())
                .containsExactly(ResourceReference.Kind.ASSET);
        assertThat(shape.mutationTarget())
                .contains(new InspectorMutationTarget.ComponentProperty(
                        occurrence, ENTITY_ID, COMPONENT_ID, new PropertyId("shape")));
        InspectorProperty target = component.properties().get(2);
        assertThat(target.state())
                .returns(Optional.empty(), InspectorProperty.State::authoredValue)
                .returns(Optional.empty(), InspectorProperty.State::defaultValue)
                .returns(Optional.empty(), InspectorProperty.State::effectiveValue)
                .returns(InspectorProperty.Origin.UNSET, InspectorProperty.State::origin)
                .returns(InspectorProperty.Validity.REQUIRED_UNSET, InspectorProperty.State::validity);
    }

    /** Removes all mutation targets from generated content while preserving typed values. */
    @Test
    void projectsGeneratedContentAsReadOnly() {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));

        LocalEntity entity = entity();
        InspectorProjection inspection =
                EditorInspectorProjector.entity(entity, context(entity, catalog(), occurrence, false, true));

        assertThat(inspection.provenance()).isEqualTo(InspectorProjection.Provenance.GENERATED);
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

        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));
        InspectorProperty property = EditorInspectorProjector.entity(
                        entity, context(entity, types, occurrence, true, false))
                .sections()
                .get(1)
                .properties()
                .getFirst();

        assertThat(property.presentation().constraints().exactElementCount()).contains(3);
        assertThat(property.presentation().constraints().elementKind()).contains(ProjectValueKind.NUMBER);
        assertThat(property.presentation().constraints().editor().semantic()).isEqualTo(PropertyEditorSemantic.VECTOR3);
    }

    /** Keeps components and authored properties visible when their extension metadata is unavailable. */
    @Test
    void projectsMissingComponentMetadataAsVisibleAndReadOnly() {
        ComponentType missingType = ComponentType.of("missing.extension/type", 1);
        ComponentDefinition missing = new ComponentDefinition(
                UNKNOWN_COMPONENT_ID,
                missingType.id(),
                missingType.version(),
                Map.of(new PropertyId("opaque"), new ProjectValue.TextValue("visible")));
        LocalEntity entity = new LocalEntity(ENTITY_ID, "Player", true, List.of(missing), List.of());
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));

        InspectorSection section = EditorInspectorProjector.entity(
                        entity, context(entity, RegisteredTypeCatalog.of(List.of()), occurrence, true, false))
                .sections()
                .get(1);

        assertThat(section)
                .returns(false, InspectorSection::metadataAvailable)
                .returns(false, InspectorSection::editable)
                .returns(Optional.of(missingType), InspectorSection::componentType);
        assertThat(section.properties().getFirst().state())
                .returns(InspectorProperty.Validity.METADATA_UNAVAILABLE, InspectorProperty.State::validity)
                .returns(false, InspectorProperty.State::editable);
        assertThat(section.properties().getFirst().mutationTarget()).isEmpty();
    }

    /** Resolves semantic references and target labels while retaining broken values as typed state. */
    @Test
    void resolvesReferenceEntityAndComponentTargets() {
        PropertyDescriptor resource = PropertyDescriptor.required(
                "resource",
                ProjectValueKind.REFERENCE,
                DescriptorPresentation.named("Resource"),
                Map.of(),
                Set.of(ResourceReference.Kind.ASSET));
        PropertyDescriptor entityTarget = PropertyDescriptor.required(
                "entity", ProjectValueKind.ENTITY_TARGET, DescriptorPresentation.named("Entity"), Map.of(), Set.of());
        PropertyDescriptor componentTarget = PropertyDescriptor.required(
                "component",
                ProjectValueKind.COMPONENT_TARGET,
                DescriptorPresentation.named("Component"),
                Map.of(),
                Set.of());
        PropertyDescriptor brokenTarget = PropertyDescriptor.required(
                "broken", ProjectValueKind.ENTITY_TARGET, DescriptorPresentation.named("Broken"), Map.of(), Set.of());
        RegisteredTypeCatalog types =
                types(ComponentTypeDescriptor.builder(COMPONENT_TYPE, DescriptorPresentation.named("Movement"))
                        .properties(List.of(resource, entityTarget, componentTarget, brokenTarget))
                        .build());
        EntityId missingEntity = EntityId.from("e1c4e98f-688b-402c-8ff7-a61b623ac63f");
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                new PropertyId("resource"), new ProjectValue.ReferenceValue(ResourceReference.asset("shape-asset")));
        properties.put(new PropertyId("entity"), new ProjectValue.EntityTargetValue(ENTITY_ID));
        properties.put(
                new PropertyId("component"),
                new ProjectValue.ComponentTargetValue(new ComponentTarget(ENTITY_ID, COMPONENT_ID)));
        properties.put(new PropertyId("broken"), new ProjectValue.EntityTargetValue(missingEntity));
        LocalEntity entity = new LocalEntity(
                ENTITY_ID,
                "Player",
                true,
                List.of(new ComponentDefinition(COMPONENT_ID, COMPONENT_TYPE.id(), 1, properties)),
                List.of());
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(ENTITY_ID));
        ProjectAsset asset = new ProjectAsset(
                "Player Shape", "shape-asset", ProjectAsset.Kind.SOURCE_ASSET, Path.of("/project/assets/shape.glb"));
        EditorInspectorProjector.Context context = new EditorInspectorProjector.Context(
                new InspectorTarget(
                        InspectorTarget.Kind.LOCAL_ENTITY, SOURCE, ENTITY_ID.toString(), Optional.of(occurrence)),
                InspectorProjection.DefinitionOrigin.AUTHORED,
                InspectorProjection.Provenance.LOCAL,
                true,
                types,
                List.of(entity),
                new HierarchyOccurrenceId(WORLD_ID, List.of()),
                List.of(asset));

        List<InspectorProperty> projected = EditorInspectorProjector.entity(entity, context)
                .sections()
                .get(1)
                .properties();

        assertThat(projected.get(0).state().effectiveValue())
                .contains(new InspectorValue.ReferenceValue(
                        ResourceReference.asset("shape-asset"),
                        AuthoringText.literal("Player Shape"),
                        InspectorValue.Resolution.RESOLVED,
                        Optional.of(asset.source().toUri())));
        assertThat(projected.get(1).state().effectiveValue())
                .contains(new InspectorValue.EntityTargetValue(
                        ENTITY_ID,
                        AuthoringText.literal("Player"),
                        InspectorValue.Resolution.RESOLVED,
                        Optional.of(occurrence)));
        assertThat(projected.get(2).state().effectiveValue())
                .contains(new InspectorValue.ComponentTargetValue(
                        new ComponentTarget(ENTITY_ID, COMPONENT_ID),
                        AuthoringText.literal("Player"),
                        AuthoringText.literal("Movement"),
                        Optional.of(COMPONENT_TYPE),
                        InspectorValue.Resolution.RESOLVED,
                        Optional.of(occurrence)));
        assertThat(projected.get(3).state())
                .returns(InspectorProperty.Validity.BROKEN_REFERENCE, InspectorProperty.State::validity);
    }

    /** Exposes placement context while keeping realized definition-root components read-only. */
    @Test
    void projectsPlacementRootComponentsAsReadOnly() {
        LocalEntity root = entity();
        AssetId definitionId = AssetId.from("47450ef4-6921-4485-8c40-67c358e41d40");
        EntityDefinition definition = new EntityDefinition(definitionId, "Reusable Player", root);
        EntityId placementId = EntityId.from("422c96fc-dfd5-4399-8f06-02e6b786e49f");
        EntityPlacement placement =
                new EntityPlacement(placementId, "Player Instance", true, AssetRef.to(definitionId), Map.of());
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(WORLD_ID, List.of(placementId));
        EditorInspectorProjector.Context context = new EditorInspectorProjector.Context(
                new InspectorTarget(
                        InspectorTarget.Kind.PLACEMENT, SOURCE, placementId.toString(), Optional.of(occurrence)),
                InspectorProjection.DefinitionOrigin.AUTHORED,
                InspectorProjection.Provenance.GENERATED,
                true,
                catalog(),
                List.of(placement),
                new HierarchyOccurrenceId(WORLD_ID, List.of()),
                List.of());

        InspectorProjection inspection =
                EditorInspectorProjector.placement(placement, Optional.of(definition), context);

        assertThat(inspection.sections().getFirst())
                .returns(InspectorSection.Kind.PLACEMENT, InspectorSection::kind)
                .returns(true, InspectorSection::editable);
        assertThat(inspection.sections().getFirst().properties().getFirst().mutationTarget())
                .isPresent();
        assertThat(inspection.sections().get(1))
                .returns(InspectorSection.Kind.COMPONENT, InspectorSection::kind)
                .returns(false, InspectorSection::editable);
        assertThat(inspection.sections().get(1).properties())
                .extracting(InspectorProperty::mutationTarget)
                .containsOnly(Optional.empty());
    }

    /** Copies section collections so callers cannot mutate projected state. */
    @Test
    void producesImmutableInspectorData() {
        List<InspectorSection> sections = new ArrayList<>();
        InspectorProjection inspection = new InspectorProjection(
                new InspectorTarget(InspectorTarget.Kind.ASSET, SOURCE, "asset", Optional.empty()),
                AuthoringText.literal("Asset"),
                InspectorProjection.DefinitionOrigin.AUTHORED,
                InspectorProjection.Provenance.LOCAL,
                false,
                sections);
        InspectorSection value = new InspectorSection(
                "late",
                InspectorSection.Kind.ENTITY,
                AuthoringText.literal("Late"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                false,
                List.of());
        List<InspectorSection> immutableSections = inspection.sections();

        assertThatThrownBy(() -> immutableSections.add(value)).isInstanceOf(UnsupportedOperationException.class);
    }

    private static LocalEntity entity() {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyId("speed"), new ProjectValue.NumberValue(new BigDecimal("3.5")));
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
        PropertyDescriptor target = PropertyDescriptor.required(
                "target", ProjectValueKind.ENTITY_TARGET, DescriptorPresentation.named("Target"), Map.of(), Set.of());
        ComponentTypeDescriptor component = ComponentTypeDescriptor.builder(
                        COMPONENT_TYPE, DescriptorPresentation.described("Movement", "Moves the entity"))
                .properties(List.of(speed, shape, target))
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

    private static EditorInspectorProjector.Context context(
            LocalEntity entity,
            RegisteredTypeCatalog types,
            HierarchyOccurrenceId occurrence,
            boolean editable,
            boolean generated) {
        return new EditorInspectorProjector.Context(
                new InspectorTarget(
                        generated ? InspectorTarget.Kind.GENERATED_ENTITY : InspectorTarget.Kind.LOCAL_ENTITY,
                        SOURCE,
                        entity.id().toString(),
                        Optional.of(occurrence)),
                InspectorProjection.DefinitionOrigin.AUTHORED,
                generated ? InspectorProjection.Provenance.GENERATED : InspectorProjection.Provenance.LOCAL,
                editable,
                types,
                List.of(entity),
                new HierarchyOccurrenceId(WORLD_ID, List.of()),
                List.of());
    }
}
