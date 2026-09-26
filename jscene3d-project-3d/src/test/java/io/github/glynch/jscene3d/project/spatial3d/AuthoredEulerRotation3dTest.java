/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.junit.jupiter.api.Test;

/** Verifies the canonical conversion between authored XYZ degrees and runtime quaternions. */
final class AuthoredEulerRotation3dTest {
    private static final float ORIENTATION_TOLERANCE = 1.0E-5F;

    /** Converts identity and each positive cardinal-axis rotation. */
    @Test
    void convertsCardinalEulerDegreesToQuaternions() {
        assertEquivalent(quaternion(0, 0, 0), new Quaternionf());
        assertEquivalent(quaternion(90, 0, 0), new Quaternionf().rotationX((float) Math.PI / 2.0F));
        assertEquivalent(quaternion(0, 90, 0), new Quaternionf().rotationY((float) Math.PI / 2.0F));
        assertEquivalent(quaternion(0, 0, 90), new Quaternionf().rotationZ((float) Math.PI / 2.0F));
    }

    /** Applies a non-commutative multi-axis rotation in canonical XYZ order. */
    @Test
    void appliesCanonicalXyzOrder() {
        Quaternionf actual = quaternion(30, 45, 60);
        Quaternionf expected = new Quaternionf().rotationXYZ(radians(30), radians(45), radians(60));
        Quaternionf differentOrder = new Quaternionf().rotationZYX(radians(60), radians(45), radians(30));

        assertEquivalent(actual, expected);
        assertThat(Math.abs(actual.dot(differentOrder))).isLessThan(0.999F);
    }

    /** Converts source quaternions to concise derived degrees while preserving orientation. */
    @Test
    void convertsQuaternionsToCanonicalRoundedDegrees() {
        ProjectValue.ArrayValue identity = AuthoredEulerRotation3d.fromQuaternion(new Quaternionf());
        ProjectValue.ArrayValue xNinety =
                AuthoredEulerRotation3d.fromQuaternion(new Quaternionf().rotationX((float) Math.PI / 2.0F));
        Quaternionf source = new Quaternionf().rotationXYZ(radians(23), radians(-41), radians(67));
        ProjectValue.ArrayValue multiAxis = AuthoredEulerRotation3d.fromQuaternion(source);

        assertThat(decimals(identity)).containsExactly("0", "0", "0");
        assertThat(decimals(xNinety)).containsExactly("90", "0", "0");
        assertThat(decimals(multiAxis)).containsExactly("23", "-41", "67");
        assertEquivalent(AuthoredEulerRotation3d.toQuaternion(multiAxis, "orientation"), source);
    }

    /** Rejects stale quaternion arrays and invalid numerical inputs without a compatibility fallback. */
    @Test
    void rejectsInvalidAuthoredAndRuntimeValues() {
        ProjectValue.ArrayValue staleQuaternion = numbers(0, 0, 0, 1);
        ProjectValue notAnArray = new ProjectValue.TextValue("orientation");
        ProjectValue.ArrayValue nonNumeric =
                new ProjectValue.ArrayValue(List.of(number(0), new ProjectValue.TextValue("right"), number(0)));
        ProjectValue.ArrayValue nonFiniteDegree = new ProjectValue.ArrayValue(
                List.of(new ProjectValue.NumberValue(new BigDecimal("1E10000")), number(0), number(0)));
        Quaternionf zeroLengthQuaternion = new Quaternionf(0, 0, 0, 0);
        Quaternionf nonFiniteQuaternion = new Quaternionf(0, 0, 0, Float.NaN);

        assertThatThrownBy(() -> AuthoredEulerRotation3d.toQuaternion(staleQuaternion, "orientation"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.toQuaternion(notAnArray, "orientation"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.toQuaternion(nonNumeric, "orientation"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry 1 must be a number");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.toQuaternion(nonFiniteDegree, "orientation"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry 0 must be finite");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.fromQuaternion(zeroLengthQuaternion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero-length");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.fromQuaternion(nonFiniteQuaternion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite components");
        assertThatThrownBy(() -> AuthoredEulerRotation3d.fromRadians(Float.NaN, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x angle must be finite");
    }

    /** Converts authored degrees through the production boundary. */
    private static Quaternionf quaternion(int x, int y, int z) {
        return AuthoredEulerRotation3d.toQuaternion(numbers(x, y, z), "orientation");
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(int... values) {
        return new ProjectValue.ArrayValue(Arrays.stream(values)
                .mapToObj(value -> (ProjectValue) number(value))
                .toList());
    }

    /** Creates one portable integer-valued number. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Extracts canonical decimal spellings. */
    private static List<String> decimals(ProjectValue.ArrayValue value) {
        return value.values().stream()
                .map(ProjectValue.NumberValue.class::cast)
                .map(number -> number.value().toString())
                .toList();
    }

    /** Converts one integral degree value to radians. */
    private static float radians(int degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** Compares quaternion rotations while allowing the equivalent negated representation. */
    private static void assertEquivalent(Quaternionfc actual, Quaternionfc expected) {
        assertThat(Math.abs(actual.dot(expected))).isCloseTo(1.0F, offset(ORIENTATION_TOLERANCE));
    }
}
