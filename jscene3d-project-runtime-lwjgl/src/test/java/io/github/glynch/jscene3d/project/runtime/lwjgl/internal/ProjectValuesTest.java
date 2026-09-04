/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises strict conversion from portable project values to renderer values. */
final class ProjectValuesTest {
    /** Converts every supported representation without weakening its source type. */
    @Test
    void convertsSupportedValues(@TempDir Path temporaryDirectory) {
        Path path = temporaryDirectory.resolve("resource.json");
        ResourceReference reference = ResourceReference.project("resource.json", path);
        Map<String, ProjectValue> properties = Map.of(
                "number", number("1.25"),
                "boolean", new ProjectValue.BooleanValue(true),
                "reference", new ProjectValue.ReferenceValue(reference),
                "vector", vector(number("1"), number("2"), number("3")),
                "color", new ProjectValue.TextValue("#123abc"));

        assertThat(ProjectValues.number(properties, "number")).isEqualTo(1.25f);
        assertThat(ProjectValues.bool(properties, "boolean")).isTrue();
        assertThat(ProjectValues.reference(properties, "reference")).isEqualTo(reference);
        assertThat(ProjectValues.vector3(properties, "vector")).isEqualTo(new Vector3f(1.0f, 2.0f, 3.0f));
        assertThat(ProjectValues.color(properties, "color")).isEqualTo(Color.srgb(0x123abc));
    }

    /** Rejects numbers outside the finite single-precision range. */
    @Test
    void rejectsNonFiniteFloat() {
        Map<String, ProjectValue> properties = Map.of("value", number("1e1000"));

        assertThatThrownBy(() -> ProjectValues.number(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be representable as a finite float");
    }

    /** Rejects vectors that do not contain exactly three entries. */
    @Test
    void rejectsWrongVectorSize() {
        Map<String, ProjectValue> properties = Map.of("value", vector(number("1"), number("2")));

        assertThatThrownBy(() -> ProjectValues.vector3(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must contain exactly three numbers");
    }

    /** Rejects non-numeric vector entries. */
    @Test
    void rejectsNonNumericVectorEntry() {
        Map<String, ProjectValue> properties =
                Map.of("value", vector(number("1"), new ProjectValue.TextValue("two"), number("3")));

        assertThatThrownBy(() -> ProjectValues.vector3(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must contain only numbers");
    }

    /** Rejects vector entries outside the finite single-precision range. */
    @Test
    void rejectsNonFiniteVectorEntry() {
        Map<String, ProjectValue> properties = Map.of("value", vector(number("1"), number("1e1000"), number("3")));

        assertThatThrownBy(() -> ProjectValues.vector3(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must contain only finite float values");
    }

    /** Rejects color text that does not have the required shape. */
    @Test
    void rejectsMalformedColorShape() {
        Map<String, ProjectValue> properties = Map.of("value", new ProjectValue.TextValue("123abc"));

        assertThatThrownBy(() -> ProjectValues.color(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must use #RRGGBB syntax");
    }

    /** Rejects seven-character color text that omits the hexadecimal marker. */
    @Test
    void rejectsColorWithoutMarker() {
        Map<String, ProjectValue> properties = Map.of("value", new ProjectValue.TextValue("x123abc"));

        assertThatThrownBy(() -> ProjectValues.color(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must use #RRGGBB syntax");
    }

    /** Rejects six-character color payloads containing non-hexadecimal digits. */
    @Test
    void rejectsMalformedColorDigits() {
        Map<String, ProjectValue> properties = Map.of("value", new ProjectValue.TextValue("#12zzzz"));

        assertThatThrownBy(() -> ProjectValues.color(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must use #RRGGBB syntax")
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    /** Rejects a portable value whose representation does not match the requested conversion. */
    @Test
    void rejectsWrongValueKind() {
        Map<String, ProjectValue> properties = Map.of("value", new ProjectValue.TextValue("true"));

        assertThatThrownBy(() -> ProjectValues.bool(properties, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a BooleanValue");
    }

    /** Creates one arbitrary-precision number value. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Creates one ordered array value. */
    private static ProjectValue.ArrayValue vector(ProjectValue... values) {
        return new ProjectValue.ArrayValue(List.of(values));
    }
}
