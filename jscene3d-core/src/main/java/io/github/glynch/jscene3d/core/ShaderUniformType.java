/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Uniform value types supported by version 0.1 custom shaders. */
public enum ShaderUniformType {
    /** One GLSL {@code float}. */
    FLOAT(1),

    /** One GLSL {@code int}. */
    INTEGER(0),

    /** One GLSL {@code bool}. */
    BOOLEAN(0),

    /** One GLSL {@code vec2}. */
    VECTOR2(2),

    /** One GLSL {@code vec3}. */
    VECTOR3(3),

    /** One GLSL {@code vec4}. */
    VECTOR4(4),

    /** One column-major GLSL {@code mat3}. */
    MATRIX3(9),

    /** One column-major GLSL {@code mat4}. */
    MATRIX4(16),

    /** One linear-sRGB {@link Color}, supplied to GLSL as {@code vec3}. */
    COLOR(3),

    /** One shared two-dimensional {@link Texture}, supplied as {@code sampler2D}. */
    TEXTURE(0);

    private final int floatComponentCount;

    /** Retains the number of float components used by this type. */
    ShaderUniformType(int floatComponentCount) {
        this.floatComponentCount = floatComponentCount;
    }

    /**
     * Returns the number of float components stored by this type.
     *
     * @return zero for integer, boolean, and texture values
     */
    public int floatComponentCount() {
        return floatComponentCount;
    }
}
