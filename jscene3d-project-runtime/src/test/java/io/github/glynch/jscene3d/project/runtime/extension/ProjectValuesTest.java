/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the common portable-value access policy for runtime extensions. */
final class ProjectValuesTest {
    /** Reads every supported top-level property representation. */
    @Test
    void readsSupportedProperties(@TempDir Path temporaryDirectory) {
        ResourceReference reference =
                ResourceReference.project("resource.json", temporaryDirectory.resolve("resource.json"));
        Map<String, ProjectValue> properties = Map.of(
                "text", text("value"),
                "integer", number("42"),
                "float", number("1.25"),
                "boolean", new ProjectValue.BooleanValue(true),
                "array", new ProjectValue.ArrayValue(List.of(text("entry"))),
                "float-array", new ProjectValue.ArrayValue(List.of(number("1.25"), number("2.5"))),
                "integer-array", new ProjectValue.ArrayValue(List.of(number("1"), number("2"))),
                "object", new ProjectValue.ObjectValue(Map.of("key", text("value"))),
                "reference", new ProjectValue.ReferenceValue(reference));

        assertThat(ProjectValues.required(properties, "text")).isEqualTo(text("value"));
        assertThat(ProjectValues.text(properties, "text")).isEqualTo("value");
        assertThat(ProjectValues.integer(properties, "integer")).isEqualTo(42);
        assertThat(ProjectValues.finiteFloat(properties, "float")).isEqualTo(1.25F);
        assertThat(ProjectValues.bool(properties, "boolean")).isTrue();
        assertThat(ProjectValues.array(properties, "array")).containsExactly(text("entry"));
        assertThat(ProjectValues.finiteFloatArray(properties, "float-array")).containsExactly(1.25F, 2.5F);
        assertThat(ProjectValues.integerArray(properties, "integer-array")).containsExactly(1, 2);
        ProjectValue object = ProjectValues.required(properties, "object");
        assertThat(ProjectValues.object(object, "object")).containsEntry("key", text("value"));
        assertThat(ProjectValues.reference(properties, "reference")).isEqualTo(reference);
    }

    /** Reads nested values while retaining caller-supplied diagnostic locations. */
    @Test
    void readsNestedValues() {
        Map<String, ProjectValue> properties = Map.of("child", number("7"));

        ProjectValue value = ProjectValues.required(properties, "child", "/properties");

        assertThat(ProjectValues.integer(value, "/properties/child")).isEqualTo(7);
        assertThat(ProjectValues.finiteFloat(value, "/properties/child")).isEqualTo(7.0F);
    }

    /** Rejects absent top-level and nested properties with stable locations. */
    @Test
    void rejectsMissingProperties() {
        Map<String, ProjectValue> properties = Map.of();

        assertThatThrownBy(() -> ProjectValues.required(properties, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing is required");
        assertThatThrownBy(() -> ProjectValues.required(properties, "missing", "/properties"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("/properties/missing is required");
    }

    /** Rejects incompatible value representations. */
    @Test
    void rejectsWrongKinds() {
        ProjectValue text = text("wrong");

        assertThatThrownBy(() -> ProjectValues.integer(text, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be an integer");
        assertThatThrownBy(() -> ProjectValues.finiteFloat(text, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a number");
        assertThatThrownBy(() -> ProjectValues.bool(text, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be boolean");
        assertThatThrownBy(() -> ProjectValues.array(text, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be an array");
        assertThatThrownBy(() -> ProjectValues.object(text, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be an object");
    }

    /** Rejects invalid numeric ranges and resource-reference kinds. */
    @Test
    void rejectsInvalidNumbersAndReference() {
        ProjectValue fractional = number("1.5");
        ProjectValue excessive = number("1e1000");
        Map<String, ProjectValue> properties = Map.of("reference", text("resource.json"));

        assertThatThrownBy(() -> ProjectValues.integer(fractional, "integer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("integer must be a 32-bit integer: 1.5")
                .hasCauseInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> ProjectValues.finiteFloat(excessive, "float"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("float must be representable as a finite float");
        assertThatThrownBy(() -> ProjectValues.reference(properties, "reference"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reference must be a resource reference");
    }

    /** Creates one arbitrary-precision numeric value. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Creates one portable text value. */
    private static ProjectValue.TextValue text(String value) {
        return new ProjectValue.TextValue(value);
    }
}
