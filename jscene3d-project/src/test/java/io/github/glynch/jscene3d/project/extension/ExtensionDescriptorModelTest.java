/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies descriptor-model value semantics, copying, and invariants. */
final class ExtensionDescriptorModelTest {
    private static final DescriptorPresentation PRESENTATION = DescriptorPresentation.named("Visible");

    /** Gives property and endpoint descriptors structural value semantics. */
    @Test
    void givesDescriptorLeavesValueSemantics() {
        PropertyDescriptor property = PropertyDescriptor.optionalWithDefault(
                "visible",
                ProjectValueKind.BOOLEAN,
                new ProjectValue.BooleanValue(true),
                PRESENTATION,
                Map.of("group", new ProjectValue.TextValue("Rendering")),
                Set.of());
        PropertyDescriptor sameProperty = PropertyDescriptor.optionalWithDefault(
                "visible",
                ProjectValueKind.BOOLEAN,
                new ProjectValue.BooleanValue(true),
                PRESENTATION,
                Map.of("group", new ProjectValue.TextValue("Rendering")),
                Set.of());
        EndpointDescriptor endpoint = EndpointDescriptor.withPayload(
                "changed", new RegisteredType("example.game/change", 1), DescriptorPresentation.named("Changed"));
        EndpointDescriptor sameEndpoint = EndpointDescriptor.withPayload(
                "changed", new RegisteredType("example.game/change", 1), DescriptorPresentation.named("Changed"));

        assertThat(property).isEqualTo(sameProperty).hasSameHashCodeAs(sameProperty);
        assertThat(property.toString()).contains("id=visible", "valueKind=BOOLEAN");
        assertThat(endpoint).isEqualTo(sameEndpoint).hasSameHashCodeAs(sameEndpoint);
        assertThat(endpoint.toString()).contains("id=changed", "example.game/change");
    }

    /** Gives complete registered-type and extension descriptors structural value semantics. */
    @Test
    void givesAggregateDescriptorsValueSemantics() {
        RegisteredTypeDescriptor type = typeDescriptor();
        RegisteredTypeDescriptor sameType = typeDescriptor();
        ExtensionDescriptor extension = extension(type);
        ExtensionDescriptor sameExtension = extension(sameType);

        assertThat(type).isEqualTo(sameType).hasSameHashCodeAs(sameType);
        assertThat(type.toString()).contains("example.game/group-3d", "SCENE_NODE");
        assertThat(extension).isEqualTo(sameExtension).hasSameHashCodeAs(sameExtension);
        assertThat(extension.toString()).contains("id=example.game", "version=1.0.0");
    }

    /** Defensively copies mutable descriptor collections. */
    @Test
    void copiesDescriptorCollections() {
        Map<String, ProjectValue> metadata = new LinkedHashMap<>();
        metadata.put("group", new ProjectValue.TextValue("Rendering"));
        List<PropertyDescriptor> properties = new ArrayList<>();
        PropertyDescriptor property = PropertyDescriptor.optional(
                "mesh", ProjectValueKind.REFERENCE, PRESENTATION, metadata, Set.of(ResourceReference.Kind.PROJECT));
        properties.add(property);
        RegisteredTypeDescriptor type = new RegisteredTypeDescriptor(
                new RegisteredType("example.game/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                PRESENTATION,
                properties,
                List.of(),
                List.of(),
                List.of());

        metadata.clear();
        properties.clear();

        assertThat(property.editorMetadata()).containsKey("group");
        assertThat(type.properties()).containsKey("mesh");
        assertThatThrownBy(type.properties()::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Applies value-kind and reference-namespace constraints. */
    @Test
    void checksPropertyValues() {
        PropertyDescriptor reference = PropertyDescriptor.optional(
                "mesh", ProjectValueKind.REFERENCE, PRESENTATION, Map.of(), Set.of(ResourceReference.Kind.ASSET));
        ProjectValue accepted = new ProjectValue.ReferenceValue(ResourceReference.asset("mesh"));
        ProjectValue wrongKind = new ProjectValue.TextValue("mesh");
        ProjectValue wrongNamespace = new ProjectValue.ReferenceValue(ResourceReference.imported("mesh/output"));
        PropertyDescriptor target = PropertyDescriptor.required(
                "body", ProjectValueKind.COMPONENT_TARGET, PRESENTATION, Map.of(), Set.of());

        assertThat(reference.accepts(accepted)).isTrue();
        assertThat(reference.accepts(wrongKind)).isFalse();
        assertThat(reference.accepts(wrongNamespace)).isFalse();
        assertThat(ProjectValueKind.of(ProjectValue.NullValue.INSTANCE)).isEqualTo(ProjectValueKind.NULL);
        assertThat(ProjectValueKind.of(new ProjectValue.ArrayValue(List.of()))).isEqualTo(ProjectValueKind.ARRAY);
        assertThat(target.accepts(new ProjectValue.TextValue("body"))).isFalse();
    }

    /** Validates homogeneous array element kinds without treating editor metadata as schema. */
    @Test
    void checksHomogeneousArrayElements() {
        PropertyDescriptor targets =
                PropertyDescriptor.requiredArray("shapes", ProjectValueKind.COMPONENT_TARGET, PRESENTATION, Map.of());
        EntityId entity = EntityId.from("f3c9430c-482b-4862-854a-17f6c51de14b");
        ComponentId component = ComponentId.from("65a5af52-2488-4ca3-be63-7161a8a65a76");
        ProjectValue target = new ProjectValue.ComponentTargetValue(new ComponentTarget(entity, component));

        assertThat(targets.valueKind()).isEqualTo(ProjectValueKind.ARRAY);
        assertThat(targets.elementKind()).contains(ProjectValueKind.COMPONENT_TARGET);
        assertThat(targets.accepts(new ProjectValue.ArrayValue(List.of(target))))
                .isTrue();
        assertThat(targets.accepts(new ProjectValue.ArrayValue(List.of(new ProjectValue.TextValue("shape")))))
                .isFalse();
    }

    /** Applies exact array cardinality independently of homogeneous element validation. */
    @Test
    void checksFixedArraySize() {
        PropertyDescriptor vector = PropertyDescriptor.optionalArrayWithDefault(
                "position",
                ProjectValueKind.NUMBER,
                3,
                numbers("0", "0", "0"),
                DescriptorPresentation.named("Position"),
                Map.of());

        assertThat(vector.exactElementCount()).contains(3);
        assertThat(vector.accepts(numbers("1", "2", "3"))).isTrue();
        assertThat(vector.accepts(numbers("1", "2"))).isFalse();
        assertThat(vector.accepts(new ProjectValue.ArrayValue(List.of(new ProjectValue.TextValue("1")))))
                .isFalse();
    }

    /** Projects only known typed editor semantics and preserves exact numeric bounds. */
    @Test
    void validatesTypedEditorSemantics() {
        PropertyDescriptor vector = PropertyDescriptor.optionalArray(
                "position",
                ProjectValueKind.NUMBER,
                3,
                PRESENTATION,
                Map.of("semantic", new ProjectValue.TextValue("vector3")));
        PropertyDescriptor integer = PropertyDescriptor.optional(
                "layers",
                ProjectValueKind.NUMBER,
                PRESENTATION,
                Map.of(
                        "semantic", new ProjectValue.TextValue("integer"),
                        "minimum", new ProjectValue.NumberValue(new BigDecimal("0")),
                        "maximum-exclusive", new ProjectValue.NumberValue(new BigDecimal("32"))),
                Set.of());
        PropertyDescriptor structurallySimilar =
                PropertyDescriptor.optionalArray("weights", ProjectValueKind.NUMBER, 3, PRESENTATION, Map.of());
        PropertyDescriptor unknown = PropertyDescriptor.optional(
                "future",
                ProjectValueKind.TEXT,
                PRESENTATION,
                Map.of(
                        "semantic", new ProjectValue.TextValue("future-widget"),
                        "vendor-data", new ProjectValue.TextValue("retained")),
                Set.of());

        assertThat(vector.editor().semantic()).isEqualTo(PropertyEditorSemantic.VECTOR3);
        assertThat(integer.editor().semantic()).isEqualTo(PropertyEditorSemantic.INTEGER);
        assertThat(integer.editor().minimum()).contains(new PropertyNumericBound(new BigDecimal("0"), true));
        assertThat(integer.editor().maximum()).contains(new PropertyNumericBound(new BigDecimal("32"), false));
        assertThat(structurallySimilar.editor().semantic()).isEqualTo(PropertyEditorSemantic.DEFAULT);
        assertThat(unknown.editor().semantic()).isEqualTo(PropertyEditorSemantic.DEFAULT);
        assertThat(unknown.editorMetadata()).containsKey("vendor-data");
    }

    /** Defines Euler rotation as an exact three-number degree vector in canonical XYZ order. */
    @Test
    void validatesEulerRotationEditorSemantic() {
        PropertyDescriptor rotation = PropertyDescriptor.optionalArray(
                "rotation",
                ProjectValueKind.NUMBER,
                3,
                PRESENTATION,
                Map.of("semantic", new ProjectValue.TextValue(PropertyEditorSemantics.EULER_ROTATION)));

        assertThat(rotation.editor().semantic()).isEqualTo(PropertyEditorSemantic.EULER_ROTATION);
        assertThat(rotation.editor().semantic().serializedName()).isEqualTo("euler-rotation");
        assertThat(rotation.valueKind()).isEqualTo(ProjectValueKind.ARRAY);
        assertThat(rotation.elementKind()).contains(ProjectValueKind.NUMBER);
        assertThat(rotation.exactElementCount()).contains(3);
    }

    /** Keeps genuine quaternion semantics available independently of ordinary orientation properties. */
    @Test
    void validatesQuaternionEditorSemantic() {
        PropertyDescriptor quaternion = PropertyDescriptor.optionalArray(
                "quaternion",
                ProjectValueKind.NUMBER,
                4,
                PRESENTATION,
                Map.of("semantic", new ProjectValue.TextValue(PropertyEditorSemantics.QUATERNION)));

        assertThat(quaternion.editor().semantic()).isEqualTo(PropertyEditorSemantic.QUATERNION);
        assertThat(quaternion.exactElementCount()).contains(4);
    }

    /** Keeps the established semantic wire names stable while adding Euler rotation. */
    @Test
    void keepsExistingPropertyEditorSemanticWireNames() {
        assertThat(PropertyEditorSemantic.VECTOR2.serializedName()).isEqualTo("vector2");
        assertThat(PropertyEditorSemantic.VECTOR3.serializedName()).isEqualTo("vector3");
        assertThat(PropertyEditorSemantic.QUATERNION.serializedName()).isEqualTo("quaternion");
        assertThat(PropertyEditorSemantic.LINEAR_COLOR.serializedName()).isEqualTo("color-linear");
    }

    /** Rejects Euler semantics on every incompatible structural shape. */
    @Test
    void rejectsIncompatibleEulerRotationEditorSemantics() {
        Map<String, ProjectValue> euler =
                Map.of("semantic", new ProjectValue.TextValue(PropertyEditorSemantics.EULER_ROTATION));
        Set<ResourceReference.Kind> noReferences = Set.of();

        assertThatThrownBy(() -> PropertyDescriptor.optional(
                        "rotation", ProjectValueKind.NUMBER, PRESENTATION, euler, noReferences))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ARRAY<NUMBER>[3]");
        assertThatThrownBy(() ->
                        PropertyDescriptor.optionalArray("rotation", ProjectValueKind.NUMBER, 2, PRESENTATION, euler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ARRAY<NUMBER>[3]");
        assertThatThrownBy(() ->
                        PropertyDescriptor.optionalArray("rotation", ProjectValueKind.TEXT, 3, PRESENTATION, euler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ARRAY<NUMBER>[3]");
    }

    /** Rejects known semantic metadata that conflicts with the declared structural shape. */
    @Test
    void rejectsIncompatibleTypedEditorSemantics() {
        Map<String, ProjectValue> vector3 = Map.of("semantic", new ProjectValue.TextValue("vector3"));
        Map<String, ProjectValue> minimum = Map.of("minimum", new ProjectValue.NumberValue(BigDecimal.ZERO));
        Set<ResourceReference.Kind> noReferences = Set.of();

        assertThatThrownBy(() ->
                        PropertyDescriptor.optionalArray("position", ProjectValueKind.NUMBER, 2, PRESENTATION, vector3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ARRAY<NUMBER>[3]");
        assertThatThrownBy(() -> PropertyDescriptor.optional(
                        "label", ProjectValueKind.TEXT, PRESENTATION, minimum, noReferences))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number property");
    }

    /** Rejects inconsistent descriptor construction. */
    @Test
    void rejectsInvalidDescriptorInvariants() {
        ProjectValue text = new ProjectValue.TextValue("true");
        Map<String, ProjectValue> editorMetadata = Map.of();
        Set<ResourceReference.Kind> noReferences = Set.of();
        Set<ResourceReference.Kind> projectReferences = Set.of(ResourceReference.Kind.PROJECT);
        RegisteredTypeDescriptor foreignType = new RegisteredTypeDescriptor(
                new RegisteredType("example.other/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                PRESENTATION,
                List.of(),
                List.of(),
                List.of(),
                List.of());
        ProjectValue target = new ProjectValue.EntityTargetValue(EntityId.from("f3c9430c-482b-4862-854a-17f6c51de14b"));

        assertThatThrownBy(() -> PropertyDescriptor.optionalWithDefault(
                        "visible", ProjectValueKind.BOOLEAN, text, PRESENTATION, editorMetadata, noReferences))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyDescriptor.optional(
                        "visible", ProjectValueKind.BOOLEAN, PRESENTATION, editorMetadata, projectReferences))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyDescriptor.optionalWithDefault(
                        "owner", ProjectValueKind.ENTITY_TARGET, target, PRESENTATION, editorMetadata, noReferences))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyDescriptor.optionalArray(
                        "position", ProjectValueKind.NUMBER, 0, PRESENTATION, editorMetadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> extension(foreignType)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Creates one numeric array from portable decimal text. */
    private static ProjectValue.ArrayValue numbers(String... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (String value : values) {
            result.add(new ProjectValue.NumberValue(new BigDecimal(value)));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Creates one representative registered-type descriptor. */
    private static RegisteredTypeDescriptor typeDescriptor() {
        return new RegisteredTypeDescriptor(
                new RegisteredType("example.game/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                DescriptorPresentation.described("Group 3d", "Groups child nodes."),
                List.of(PropertyDescriptor.required(
                        "name", ProjectValueKind.TEXT, DescriptorPresentation.named("Name"), Map.of(), Set.of())),
                List.of(EndpointDescriptor.withoutPayload("ready", DescriptorPresentation.named("Ready"))),
                List.of(EndpointDescriptor.withoutPayload("reset", DescriptorPresentation.named("Reset"))),
                List.of("org.jscene3d.render/mesh-3d"));
    }

    /** Creates one representative extension descriptor. */
    private static ExtensionDescriptor extension(RegisteredTypeDescriptor type) {
        return new ExtensionDescriptor(
                "example.game", "1.0.0", ">=0.1.0 <0.2.0", DescriptorPresentation.named("Example Game"), List.of(type));
    }
}
