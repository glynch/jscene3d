/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

/** Shared GLSL declarations and calculations for built-in distance fog. */
final class FogShaderSource {
    private static final String SOURCE = """
            uniform int fogMode;
            uniform vec3 fogColor;
            uniform float fogNear;
            uniform float fogFar;
            uniform float fogDensity;

            vec3 applyFog(vec3 surfaceColor, float viewDepth) {
                float fogFactor = 0.0;
                if (fogMode == 1) {
                    fogFactor = smoothstep(fogNear, fogFar, viewDepth);
                } else if (fogMode == 2) {
                    float scaledDepth = fogDensity * viewDepth;
                    fogFactor = 1.0 - exp(-scaledDepth * scaledDepth);
                }
                return mix(surfaceColor, fogColor, clamp(fogFactor, 0.0, 1.0));
            }
            """;

    /** Prevents instantiation of this shader-source utility. */
    private FogShaderSource() {
        throw new AssertionError("FogShaderSource cannot be instantiated");
    }

    /** Returns GLSL declarations shared by every built-in fog-aware fragment shader. */
    static String source() {
        return SOURCE;
    }
}
