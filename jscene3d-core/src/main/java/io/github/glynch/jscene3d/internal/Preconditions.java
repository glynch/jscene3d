/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.internal;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.Objects;
import java.util.regex.Pattern;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

/** Shared implementation-only precondition checks used across feature packages. */
public final class Preconditions {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");

    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires an open geometry description.
     *
     * @param geometry geometry to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated geometry
     */
    public static BufferGeometry requireOpen(BufferGeometry geometry, String parameterName) {
        BufferGeometry validGeometry = Objects.requireNonNull(geometry, parameterName);
        if (validGeometry.isClosed()) {
            throw new IllegalArgumentException(parameterName + " must be open");
        }
        return validGeometry;
    }

    /**
     * Requires an open material description while preserving its concrete type.
     *
     * @param material material to validate
     * @param parameterName parameter name used in diagnostics
     * @param <M> concrete material type
     * @return the validated material
     */
    public static <M extends Material> M requireOpen(M material, String parameterName) {
        M validMaterial = Objects.requireNonNull(material, parameterName);
        if (validMaterial.isClosed()) {
            throw new IllegalArgumentException(parameterName + " must be open");
        }
        return validMaterial;
    }

    /**
     * Requires an open texture description.
     *
     * @param texture texture to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated texture
     */
    public static Texture requireOpen(Texture texture, String parameterName) {
        Texture validTexture = Objects.requireNonNull(texture, parameterName);
        if (validTexture.isClosed()) {
            throw new IllegalArgumentException(parameterName + " must be open");
        }
        return validTexture;
    }

    /**
     * Requires a finite floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static float requireFinite(float value, String parameterName) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(parameterName + " must be finite: " + value);
        }
        return value;
    }

    /**
     * Requires a non-null two-dimensional vector with finite components.
     *
     * @param value vector to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated vector
     */
    public static Vector2fc requireFinite(Vector2fc value, String parameterName) {
        Vector2fc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        return validValue;
    }

    /**
     * Requires a finite positive floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static float requirePositive(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue <= 0.0f) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + finiteValue);
        }
        return finiteValue;
    }

    /**
     * Requires a finite non-negative floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static float requireNonNegative(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < 0.0f) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + finiteValue);
        }
        return finiteValue;
    }

    /**
     * Requires two finite values in strictly increasing order.
     *
     * @param lowerValue lower value
     * @param lowerName lower parameter name used in diagnostics
     * @param upperValue upper value
     * @param upperName upper parameter name used in diagnostics
     */
    public static void requireLessThan(float lowerValue, String lowerName, float upperValue, String upperName) {
        requireFinite(lowerValue, lowerName);
        requireFinite(upperValue, upperName);
        if (lowerValue >= upperValue) {
            throw new IllegalArgumentException(
                    lowerName + " must be less than " + upperName + ": " + lowerValue + " >= " + upperValue);
        }
    }

    /**
     * Requires a finite floating-point value in an inclusive interval.
     *
     * @param value value to validate
     * @param minimum inclusive minimum
     * @param maximum inclusive maximum
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static float requireInRange(float value, float minimum, float maximum, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < minimum || finiteValue > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + finiteValue);
        }
        return finiteValue;
    }

    /**
     * Requires an integer value in an inclusive interval.
     *
     * @param value value to validate
     * @param minimum inclusive minimum
     * @param maximum inclusive maximum
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static int requireInRange(int value, int minimum, int maximum, String parameterName) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + value);
        }
        return value;
    }

    /**
     * Requires a positive integer value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static int requirePositive(int value, String parameterName) {
        if (value <= 0) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + value);
        }
        return value;
    }

    /**
     * Requires a non-negative integer value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static int requireNonNegative(int value, String parameterName) {
        if (value < 0) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + value);
        }
        return value;
    }

    /**
     * Requires a non-null, non-empty string.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static String requireNonEmpty(String value, String parameterName) {
        String validValue = Objects.requireNonNull(value, parameterName);
        if (validValue.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " must not be empty");
        }
        return validValue;
    }

    /**
     * Requires a non-null, non-blank string.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated value
     */
    public static String requireNonBlank(String value, String parameterName) {
        String validValue = Objects.requireNonNull(value, parameterName);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        return validValue;
    }

    /**
     * Requires an identifier accepted by GLSL and Java-style named interfaces.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated identifier
     */
    public static String requireIdentifier(String value, String parameterName) {
        String validValue = requireNonEmpty(value, parameterName);
        if (!IDENTIFIER.matcher(validValue).matches() || validValue.startsWith("gl_")) {
            throw new IllegalArgumentException(parameterName + " must be a valid non-reserved identifier: " + value);
        }
        return validValue;
    }

    /**
     * Returns a checked Java-array length for an item count and item size.
     *
     * @param itemCount number of items
     * @param itemSize scalar values per item
     * @param parameterName parameter name used in diagnostics
     * @return the checked scalar-array length
     */
    public static int requireArrayLength(long itemCount, int itemSize, String parameterName) {
        if (itemCount < 0L || itemSize <= 0 || itemCount > Integer.MAX_VALUE / itemSize) {
            throw new IllegalArgumentException(parameterName + " data exceeds Java array limits");
        }
        return (int) itemCount * itemSize;
    }

    /**
     * Requires a non-null vector with finite components.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated vector
     */
    public static Vector3fc requireFinite(Vector3fc value, String parameterName) {
        Vector3fc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        return validValue;
    }

    /**
     * Requires a non-null quaternion with finite components.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return the validated quaternion
     */
    public static Quaternionfc requireFinite(Quaternionfc value, String parameterName) {
        Quaternionfc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        requireFinite(validValue.w(), parameterName + ".w");
        return validValue;
    }
}
