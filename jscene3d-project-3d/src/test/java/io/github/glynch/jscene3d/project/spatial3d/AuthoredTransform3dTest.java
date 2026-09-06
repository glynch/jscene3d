/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises numeric validation kept behind the Transform3d factory boundary. */
final class AuthoredTransform3dTest {
    /** Rejects an orientation which cannot define a rotation. */
    @Test
    void rejectsZeroLengthOrientation() {
        Map<PropertyId, ProjectValue> properties =
                properties(numbers("0", "0", "0"), numbers("0", "0", "0", "0"), numbers("1", "1", "1"));

        assertThatThrownBy(() -> AuthoredTransform3d.from(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero-length quaternion");
    }

    /** Rejects a non-numeric array entry after structural array validation. */
    @Test
    void rejectsNonNumericEntry() {
        ProjectValue.ArrayValue position = array(number("0"), new ProjectValue.TextValue("invalid"), number("0"));
        Map<PropertyId, ProjectValue> properties =
                properties(position, numbers("0", "0", "0", "1"), numbers("1", "1", "1"));

        assertThatThrownBy(() -> AuthoredTransform3d.from(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a number");
    }

    /** Rejects a number whose portable value cannot be represented as a finite float. */
    @Test
    void rejectsNonFiniteFloatConversion() {
        Map<PropertyId, ProjectValue> properties =
                properties(numbers("1E10000", "0", "0"), numbers("0", "0", "0", "1"), numbers("1", "1", "1"));

        assertThatThrownBy(() -> AuthoredTransform3d.from(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be finite");
    }

    /** Creates a complete effective transform-property map. */
    private static Map<PropertyId, ProjectValue> properties(
            ProjectValue position, ProjectValue orientation, ProjectValue scale) {
        return Map.of(
                Spatial3dDescriptors.positionProperty(), position,
                Spatial3dDescriptors.orientationProperty(), orientation,
                Spatial3dDescriptors.scaleProperty(), scale);
    }

    /** Creates a numeric array from decimal representations. */
    private static ProjectValue.ArrayValue numbers(String... values) {
        return array(Arrays.stream(values).map(AuthoredTransform3dTest::number).toArray(ProjectValue[]::new));
    }

    /** Creates one immutable project array. */
    private static ProjectValue.ArrayValue array(ProjectValue... values) {
        return new ProjectValue.ArrayValue(List.of(values));
    }

    /** Creates one portable decimal number. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }
}
