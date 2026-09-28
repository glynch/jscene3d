/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Converts the canonical authored XYZ Euler-degree representation to and from runtime quaternions. */
public final class AuthoredEulerRotation3d {
    private static final int DERIVED_DEGREE_SCALE = 4;

    /** Prevents construction of this stateless conversion boundary. */
    private AuthoredEulerRotation3d() {
        throw new AssertionError("AuthoredEulerRotation3d cannot be instantiated");
    }

    /**
     * Converts one exact three-number authored Euler-degree array to a normalized XYZ quaternion.
     *
     * @param value authored {@code [xDegrees, yDegrees, zDegrees]} value
     * @param name property name used in validation failures
     * @return normalized runtime quaternion
     * @throws NullPointerException if {@code value} or {@code name} is {@code null}
     * @throws IllegalArgumentException if the value is not exactly three finite numbers
     */
    public static Quaternionf toQuaternion(ProjectValue value, String name) {
        Objects.requireNonNull(value, "value");
        String validName = Objects.requireNonNull(name, "name");
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != 3) {
            throw new IllegalArgumentException(validName + " must contain exactly 3 numbers");
        }
        List<ProjectValue> values = array.values();
        return RotationOrder.XYZ.quaternion(
                radians(number(values.get(0), validName, 0)),
                radians(number(values.get(1), validName, 1)),
                radians(number(values.get(2), validName, 2)));
    }

    /**
     * Converts one finite quaternion to canonical XYZ Euler degrees.
     *
     * <p>Derived degree values are rounded to four decimal places with half-even rounding so source floating-point
     * noise is not serialized into project data.
     *
     * @param orientation finite non-zero source quaternion
     * @return authored {@code [xDegrees, yDegrees, zDegrees]} value
     * @throws NullPointerException if {@code orientation} is {@code null}
     * @throws IllegalArgumentException if the quaternion is non-finite or zero-length
     */
    public static ProjectValue.ArrayValue fromQuaternion(Quaternionfc orientation) {
        Quaternionfc value = Objects.requireNonNull(orientation, "orientation");
        requireFinite(value);
        double length = Math.sqrt((double) value.x() * value.x()
                + (double) value.y() * value.y()
                + (double) value.z() * value.z()
                + (double) value.w() * value.w());
        if (length == 0.0) {
            throw new IllegalArgumentException("orientation must not be a zero-length quaternion");
        }
        Quaternionf normalized = new Quaternionf(
                (float) (value.x() / length), (float) (value.y() / length), (float) (value.z() / length), (float)
                        (value.w() / length));
        Vector3f radians = normalized.getEulerAnglesXYZ(new Vector3f());
        return fromRadians(radians.x(), radians.y(), radians.z());
    }

    /**
     * Converts finite XYZ Euler angles in radians to the authored degree representation.
     *
     * <p>Derived degree values are rounded to four decimal places with half-even rounding.
     *
     * @param x rotation about the X axis in radians
     * @param y rotation about the Y axis in radians
     * @param z rotation about the Z axis in radians
     * @return authored {@code [xDegrees, yDegrees, zDegrees]} value
     * @throws IllegalArgumentException if any angle is not finite
     */
    public static ProjectValue.ArrayValue fromRadians(float x, float y, float z) {
        return new ProjectValue.ArrayValue(List.of(degreeNumber(x, "x"), degreeNumber(y, "y"), degreeNumber(z, "z")));
    }

    /** Reads one finite authored degree value. */
    private static float number(ProjectValue value, String name, int index) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(name + " entry " + index + " must be a number");
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(name + " entry " + index + " must be finite");
        }
        return result;
    }

    /** Converts finite degrees to finite radians. */
    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** Converts and deliberately rounds one derived radian angle to a portable degree number. */
    private static ProjectValue.NumberValue degreeNumber(float radians, String name) {
        if (!Float.isFinite(radians)) {
            throw new IllegalArgumentException(name + " angle must be finite");
        }
        BigDecimal rounded = BigDecimal.valueOf(Math.toDegrees(radians))
                .setScale(DERIVED_DEGREE_SCALE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros();
        if (rounded.signum() == 0) {
            rounded = BigDecimal.ZERO;
        } else if (rounded.scale() < 0) {
            rounded = rounded.setScale(0);
        }
        return new ProjectValue.NumberValue(rounded);
    }

    /** Requires each quaternion component to be finite. */
    private static void requireFinite(Quaternionfc orientation) {
        if (!Float.isFinite(orientation.x())
                || !Float.isFinite(orientation.y())
                || !Float.isFinite(orientation.z())
                || !Float.isFinite(orientation.w())) {
            throw new IllegalArgumentException("orientation must contain only finite components");
        }
    }
}
