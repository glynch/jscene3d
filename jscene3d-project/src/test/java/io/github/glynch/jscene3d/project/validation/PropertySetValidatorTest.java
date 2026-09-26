/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.standard.game3d.StandardGame3dDescriptors;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exercises the authoritative validation facade through its supported interface. */
final class PropertySetValidatorTest {
    private static final URI SOURCE = URI.create("file:///project/definition.json");
    private static final PropertyTargetLookup ALL_TARGETS = new PropertyTargetLookup() {
        @Override
        public boolean containsEntity(EntityId entity) {
            return true;
        }

        @Override
        public boolean containsComponent(ComponentTarget target) {
            return true;
        }
    };

    /** Produces granular diagnostics in authored order and missing-required diagnostics last. */
    @Test
    void validatesCompleteSetInDeterministicPhaseOrder() {
        RegisteredTypeDescriptor owner = registered(List.of(
                required("required", ProjectValueKind.BOOLEAN, Map.of()),
                required("count", ProjectValueKind.NUMBER, semantic("integer")),
                PropertyDescriptor.requiredArray(
                        "pair", ProjectValueKind.NUMBER, 2, DescriptorPresentation.named("pair"), Map.of())));
        Map<String, ProjectValue> authored = new LinkedHashMap<>();
        authored.put("mystery", new ProjectValue.TextValue("unknown"));
        authored.put("count", number("1.5"));
        authored.put("pair", array(number("1")));

        List<ProjectDiagnostic> diagnostics = PropertySetValidator.validate(authored, owner, SOURCE, "/properties");

        assertThat(diagnostics)
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("property.unknown", "property.integer", "property.array.length", "property.required");
        assertThat(diagnostics)
                .extracting(ProjectDiagnostic::location)
                .containsExactly(
                        "/properties/mystery", "/properties/count", "/properties/pair", "/properties/required");
    }

    /** Applies exact mathematical integer and inclusive/exclusive decimal-bound semantics. */
    @Test
    void validatesExactNumbersWithoutPrimitiveConversion() {
        PropertyDescriptor integer = required("count", ProjectValueKind.NUMBER, semantic("integer"));
        PropertyDescriptor bounded = required(
                "bounded",
                ProjectValueKind.NUMBER,
                Map.of("minimum", number("0.1"), "maximum-exclusive", number("0.3")));
        RegisteredTypeDescriptor owner = registered(List.of(integer, bounded));

        assertThat(PropertySetValidator.validate(
                        Map.of("count", number("1.0"), "bounded", number("0.1")), owner, SOURCE, "/properties"))
                .isEmpty();
        assertThat(PropertySetValidator.validate(
                        Map.of("count", number("1e3"), "bounded", number("0.299999999999999999999")),
                        owner,
                        SOURCE,
                        "/properties"))
                .isEmpty();
        assertThat(PropertySetValidator.validate(
                        Map.of("count", number("1.01"), "bounded", number("0.3")), owner, SOURCE, "/properties"))
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactlyInAnyOrder("property.integer", "property.maximum");
    }

    /** Accepts arbitrary Euler degrees and non-unit quaternions but rejects zero quaternions. */
    @Test
    void validatesRotationSemanticsWithoutNormalization() {
        RegisteredTypeDescriptor owner = registered(
                List.of(arrayProperty("rotation", 3, "euler-rotation"), arrayProperty("orientation", 4, "quaternion")));
        Map<String, ProjectValue> valid = Map.of(
                "rotation", array(number("450"), number("-720"), number("0")),
                "orientation", array(number("0"), number("2"), number("0"), number("0")));

        assertThat(PropertySetValidator.validate(valid, owner, SOURCE, "/properties"))
                .isEmpty();
        assertThat(PropertySetValidator.validate(
                        Map.of(
                                "rotation", valid.get("rotation"),
                                "orientation", array(number("0"), number("0"), number("0"), number("0"))),
                        owner,
                        SOURCE,
                        "/properties"))
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("property.quaternion.zero");
    }

    /** Enforces exact inclusive linear-color channel bounds. */
    @Test
    void validatesLinearColorChannels() {
        RegisteredTypeDescriptor owner =
                registered(List.of(arrayProperty("color", 3, PropertyEditorSemantics.LINEAR_COLOR)));

        assertThat(PropertySetValidator.validate(
                        Map.of("color", array(number("0"), number("0.5"), number("1"))), owner, SOURCE, "/properties"))
                .isEmpty();
        assertThat(PropertySetValidator.validate(
                        Map.of("color", array(number("-0.0001"), number("0.5"), number("1.0001"))),
                        owner,
                        SOURCE,
                        "/properties"))
                .extracting(ProjectDiagnostic::location)
                .containsExactly("/properties/color/0", "/properties/color/2");
    }

    /** Applies whole-owner rules after core validation over effective property values. */
    @Test
    void validatesBuiltInCrossPropertyRules() {
        ComponentTypeDescriptor camera = StandardSpatial3dDescriptors.extensionDescriptor().components().stream()
                .filter(descriptor -> descriptor.type().equals(StandardSpatial3dDescriptors.perspectiveCameraType()))
                .findFirst()
                .orElseThrow();
        Map<PropertyId, ProjectValue> invalidCamera = Map.of(
                StandardSpatial3dDescriptors.nearProperty(), number("10"),
                StandardSpatial3dDescriptors.farProperty(), number("10"));
        ComponentTypeDescriptor controller =
                StandardGame3dDescriptors.extensionDescriptor().components().getFirst();
        Map<PropertyId, ProjectValue> invalidController = validControllerProperties();
        invalidController.put(StandardGame3dDescriptors.turnSpeedDegreesProperty(), number("100"));
        invalidController.put(StandardGame3dDescriptors.maximumKeyboardTurnSpeedDegreesProperty(), number("99"));

        assertThat(PropertySetValidator.validateComponent(invalidCamera, camera, SOURCE, "/properties", ALL_TARGETS))
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("property.domain");
        assertThat(PropertySetValidator.validateComponent(
                        invalidController, controller, SOURCE, "/properties", ALL_TARGETS))
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("property.domain");
    }

    /** Creates a mutable complete controller property map from descriptor-valid values. */
    private static Map<PropertyId, ProjectValue> validControllerProperties() {
        Map<PropertyId, ProjectValue> values = new LinkedHashMap<>();
        values.put(
                StandardGame3dDescriptors.bodyProperty(),
                new ProjectValue.ComponentTargetValue(new ComponentTarget(
                        EntityId.from("11111111-1111-4111-8111-111111111111"),
                        ComponentId.from("22222222-2222-4222-8222-222222222222"))));
        values.put(
                StandardGame3dDescriptors.viewTransformProperty(),
                values.get(StandardGame3dDescriptors.bodyProperty()));
        values.put(StandardGame3dDescriptors.moveActionProperty(), new ProjectValue.TextValue("move"));
        values.put(StandardGame3dDescriptors.lookActionProperty(), new ProjectValue.TextValue("look"));
        values.put(StandardGame3dDescriptors.turnLeftActionProperty(), new ProjectValue.TextValue("left"));
        values.put(StandardGame3dDescriptors.turnRightActionProperty(), new ProjectValue.TextValue("right"));
        values.put(StandardGame3dDescriptors.moveSpeedProperty(), number("8"));
        values.put(StandardGame3dDescriptors.turnSpeedDegreesProperty(), number("90"));
        values.put(StandardGame3dDescriptors.maximumKeyboardTurnSpeedDegreesProperty(), number("180"));
        values.put(StandardGame3dDescriptors.keyboardTurnAccelerationDegreesProperty(), number("360"));
        values.put(StandardGame3dDescriptors.pointerSensitivityProperty(), number("0.001"));
        values.put(StandardGame3dDescriptors.maximumPitchDegreesProperty(), number("85"));
        return values;
    }

    /** Creates one required scalar descriptor. */
    private static PropertyDescriptor required(String id, ProjectValueKind kind, Map<String, ProjectValue> editor) {
        return PropertyDescriptor.required(id, kind, DescriptorPresentation.named(id), editor, Set.of());
    }

    /** Creates one fixed numeric array with specialized semantics. */
    private static PropertyDescriptor arrayProperty(String id, int count, String semantic) {
        return PropertyDescriptor.requiredArray(
                id, ProjectValueKind.NUMBER, count, DescriptorPresentation.named(id), semantic(semantic));
    }

    /** Creates one test registered-type descriptor. */
    private static RegisteredTypeDescriptor registered(List<PropertyDescriptor> properties) {
        return new RegisteredTypeDescriptor(
                new RegisteredType("example.validation/type", 1),
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.named("Validation type"),
                properties,
                List.of(),
                List.of(),
                List.of());
    }

    /** Creates editor semantic metadata. */
    private static Map<String, ProjectValue> semantic(String semantic) {
        return Map.of("semantic", new ProjectValue.TextValue(semantic));
    }

    /** Creates one exact decimal project number. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Creates one portable project array. */
    private static ProjectValue.ArrayValue array(ProjectValue... values) {
        return new ProjectValue.ArrayValue(List.of(values));
    }
}
