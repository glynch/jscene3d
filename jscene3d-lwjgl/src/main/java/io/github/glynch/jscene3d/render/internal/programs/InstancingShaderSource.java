/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

/** Shared GLSL declarations and helpers for optional mesh instancing. */
final class InstancingShaderSource {
    private static final String SOURCE = """
            layout(location = 7) in vec4 instanceMatrixColumn0;
            layout(location = 8) in vec4 instanceMatrixColumn1;
            layout(location = 9) in vec4 instanceMatrixColumn2;
            layout(location = 10) in vec4 instanceMatrixColumn3;
            layout(location = 11) in vec3 instanceColor;

            uniform bool useInstancing;
            uniform bool useInstanceColor;

            mat4 resolvedInstanceMatrix() {
                return useInstancing
                        ? mat4(
                                instanceMatrixColumn0,
                                instanceMatrixColumn1,
                                instanceMatrixColumn2,
                                instanceMatrixColumn3)
                        : mat4(1.0);
            }

            mat3 resolvedInstanceNormalMatrix(mat4 transform) {
                return useInstancing ? transpose(inverse(mat3(transform))) : mat3(1.0);
            }

            vec4 resolvedInstanceColor() {
                return useInstanceColor ? vec4(instanceColor, 1.0) : vec4(1.0);
            }
            """;

    /** Prevents instantiation of this static source holder. */
    private InstancingShaderSource() {
        throw new AssertionError("InstancingShaderSource cannot be instantiated");
    }

    /** Returns shared optional-instancing declarations and functions. */
    static String source() {
        return SOURCE;
    }
}
