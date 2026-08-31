/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Stable read-only view of one typed custom-shader uniform value. */
public final class ShaderUniform {
    private final ShaderUniformType type;
    private final float[] floatComponents;

    private int integerValue;
    private boolean booleanValue;
    private @Nullable Texture textureValue;
    private boolean initialized;

    /** Creates storage for one declared uniform type. */
    ShaderUniform(ShaderUniformType type) {
        this.type = Objects.requireNonNull(type, "type");
        floatComponents = new float[type.floatComponentCount()];
    }

    /**
     * Returns the immutable declared type.
     *
     * @return uniform type
     */
    public ShaderUniformType type() {
        return type;
    }

    /**
     * Returns one float component.
     *
     * <p>Vectors use XYZW order and matrices use column-major order.
     *
     * @param index zero-based component index
     * @return requested component
     * @throws IllegalStateException if this uniform has no float components
     * @throws IndexOutOfBoundsException if {@code index} is outside the component range
     */
    public float floatComponent(int index) {
        if (floatComponents.length == 0) {
            throw new IllegalStateException(type + " uniform has no float components");
        }
        return floatComponents[index];
    }

    /**
     * Returns the integer value.
     *
     * @return current integer
     * @throws IllegalStateException if this is not an integer uniform
     */
    public int integerValue() {
        requireType(ShaderUniformType.INTEGER);
        return integerValue;
    }

    /**
     * Returns the boolean value.
     *
     * @return current boolean
     * @throws IllegalStateException if this is not a boolean uniform
     */
    public boolean booleanValue() {
        requireType(ShaderUniformType.BOOLEAN);
        return booleanValue;
    }

    /**
     * Returns the shared texture value.
     *
     * @return current open texture
     * @throws IllegalStateException if this is not a texture uniform
     */
    public Texture textureValue() {
        requireType(ShaderUniformType.TEXTURE);
        return Objects.requireNonNull(textureValue, "Texture uniform has no value");
    }

    /** Replaces one through four finite float components and reports whether they changed. */
    boolean set(float first, float second, float third, float fourth) {
        float[] values = floatComponents;
        boolean changed = !initialized || values[0] != first;
        values[0] = first;
        if (values.length > 1) {
            changed |= values[1] != second;
            values[1] = second;
        }
        if (values.length > 2) {
            changed |= values[2] != third;
            values[2] = third;
        }
        if (values.length > 3) {
            changed |= values[3] != fourth;
            values[3] = fourth;
        }
        initialized = true;
        return changed;
    }

    /** Copies a finite three-by-three matrix and reports whether it changed. */
    boolean set(Matrix3fc matrix) {
        return setMatrix(matrix, 3);
    }

    /** Copies a finite four-by-four matrix and reports whether it changed. */
    boolean set(Matrix4fc matrix) {
        return setMatrix(matrix, 4);
    }

    /** Replaces an integer and reports whether it changed. */
    boolean set(int value) {
        boolean changed = !initialized || integerValue != value;
        integerValue = value;
        initialized = true;
        return changed;
    }

    /** Replaces a boolean and reports whether it changed. */
    boolean set(boolean value) {
        boolean changed = !initialized || booleanValue != value;
        booleanValue = value;
        initialized = true;
        return changed;
    }

    /** Replaces a shared texture reference and reports whether it changed. */
    // Texture descriptions deliberately use stable reference identity.
    @SuppressWarnings("ReferenceEquality")
    boolean set(Texture value) {
        boolean changed = !initialized || textureValue != value;
        textureValue = value;
        initialized = true;
        return changed;
    }

    /** Copies and validates a square matrix in column-major order. */
    private boolean setMatrix(Matrix3fc matrix, int size) {
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                Preconditions.requireFinite(matrix.get(column, row), "matrix[" + column + "][" + row + "]");
            }
        }
        boolean changed = !initialized;
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                int index = column * size + row;
                float value = matrix.get(column, row);
                changed |= floatComponents[index] != value;
                floatComponents[index] = value;
            }
        }
        initialized = true;
        return changed;
    }

    /** Copies and validates a square matrix in column-major order. */
    private boolean setMatrix(Matrix4fc matrix, int size) {
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                Preconditions.requireFinite(matrix.get(column, row), "matrix[" + column + "][" + row + "]");
            }
        }
        boolean changed = !initialized;
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                int index = column * size + row;
                float value = matrix.get(column, row);
                changed |= floatComponents[index] != value;
                floatComponents[index] = value;
            }
        }
        initialized = true;
        return changed;
    }

    /** Requires this value to have the requested type. */
    private void requireType(ShaderUniformType expectedType) {
        if (type != expectedType) {
            throw new IllegalStateException("Uniform type is " + type + ", not " + expectedType);
        }
    }
}
