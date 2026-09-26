/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exercises context-free authoritative validation during descriptor construction. */
final class PropertyDescriptorValidationTest {
    /** Rejects semantically invalid scalar defaults. */
    @Test
    void rejectsInvalidIntegerAndBoundedDefaults() {
        ProjectValue nonIntegral = number("1.5");
        ProjectValue belowMinimum = number("-0.1");
        Map<String, ProjectValue> integerEditor = Map.of("semantic", text("integer"));
        Map<String, ProjectValue> boundedEditor = Map.of("minimum", number("0"));

        assertThatThrownBy(() -> scalarDefault("count", nonIntegral, integerEditor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue");
        assertThatThrownBy(() -> scalarDefault("bounded", belowMinimum, boundedEditor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue");
    }

    /** Rejects zero-quaternion and out-of-range linear-color defaults. */
    @Test
    void rejectsInvalidArraySemanticDefaults() {
        ProjectValue.ArrayValue zeroQuaternion = array("0", "0", "0", "0");
        ProjectValue.ArrayValue invalidColor = array("0", "1.01", "1");

        assertThatThrownBy(() -> arrayDefault("orientation", zeroQuaternion, "quaternion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue");
        assertThatThrownBy(() -> arrayDefault("color", invalidColor, PropertyEditorSemantics.LINEAR_COLOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue");
    }

    /** Rejects empty intervals while accepting an inclusive singleton domain. */
    @Test
    void validatesNumericIntervalCoherence() {
        Map<String, ProjectValue> reversed = Map.of("minimum", number("2"), "maximum", number("1"));
        Map<String, ProjectValue> empty = Map.of("minimum-exclusive", number("1"), "maximum", number("1"));

        assertThatThrownBy(() -> scalar("reversed", reversed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-empty interval");
        assertThatThrownBy(() -> scalar("empty", empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-empty interval");
        assertThat(scalar("singleton", Map.of("minimum", number("1"), "maximum", number("1")))
                        .editor()
                        .minimum())
                .contains(new PropertyNumericBound(BigDecimal.ONE, true));
    }

    /** Creates one optional scalar descriptor with a default. */
    private static PropertyDescriptor scalarDefault(String id, ProjectValue value, Map<String, ProjectValue> editor) {
        return PropertyDescriptor.optionalWithDefault(
                id, ProjectValueKind.NUMBER, value, DescriptorPresentation.named(id), editor, Set.of());
    }

    /** Creates one required scalar descriptor. */
    private static PropertyDescriptor scalar(String id, Map<String, ProjectValue> editor) {
        return PropertyDescriptor.required(
                id, ProjectValueKind.NUMBER, DescriptorPresentation.named(id), editor, Set.of());
    }

    /** Creates one fixed numeric array descriptor with a default. */
    private static PropertyDescriptor arrayDefault(String id, ProjectValue.ArrayValue value, String semantic) {
        return PropertyDescriptor.optionalArrayWithDefault(
                id,
                ProjectValueKind.NUMBER,
                value.values().size(),
                value,
                DescriptorPresentation.named(id),
                Map.of("semantic", text(semantic)));
    }

    /** Creates one exact decimal project number. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Creates one project text value. */
    private static ProjectValue.TextValue text(String value) {
        return new ProjectValue.TextValue(value);
    }

    /** Creates one numeric project array. */
    private static ProjectValue.ArrayValue array(String... values) {
        return new ProjectValue.ArrayValue(Arrays.stream(values)
                .<ProjectValue>map(PropertyDescriptorValidationTest::number)
                .toList());
    }
}
