/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import io.github.glynch.jscene3d.render.internal.ShadowFrame;

/** Shared GLSL declarations and sampling functions for built-in lit programs. */
final class ShadowShaderSource {
    private static final String SOURCE = """
            const int MAX_TWO_DIMENSIONAL_SHADOWS = TWO_DIMENSIONAL_SHADOW_CAPACITY;
            const int MAX_POINT_SHADOWS = POINT_SHADOW_CAPACITY;

            uniform bool receiveShadow;
            uniform int directionalShadowIndices[MAX_DIRECTIONAL_LIGHTS];
            uniform int spotShadowIndices[MAX_SPOT_LIGHTS];
            uniform int pointShadowIndices[MAX_POINT_LIGHTS];
            uniform mat4 shadowMatrices[MAX_TWO_DIMENSIONAL_SHADOWS];
            uniform float shadowBiases[MAX_TWO_DIMENSIONAL_SHADOWS];
            uniform float shadowNormalBiases[MAX_TWO_DIMENSIONAL_SHADOWS];
            uniform sampler2D shadowMaps[MAX_TWO_DIMENSIONAL_SHADOWS];
            uniform vec3 pointShadowPositions[MAX_POINT_SHADOWS];
            uniform float pointShadowFarPlanes[MAX_POINT_SHADOWS];
            uniform float pointShadowBiases[MAX_POINT_SHADOWS];
            uniform float pointShadowNormalBiases[MAX_POINT_SHADOWS];
            uniform samplerCube pointShadowMaps[MAX_POINT_SHADOWS];
            uniform mat3 shadowViewToWorldMatrix;

            float sampleTwoDimensionalMap(int slot, vec2 coordinate) {
                if (slot == 0) return texture(shadowMaps[0], coordinate).r;
                if (slot == 1) return texture(shadowMaps[1], coordinate).r;
                if (slot == 2) return texture(shadowMaps[2], coordinate).r;
                return texture(shadowMaps[3], coordinate).r;
            }

            vec2 twoDimensionalTexelSize(int slot) {
                if (slot == 0) return 1.0 / vec2(textureSize(shadowMaps[0], 0));
                if (slot == 1) return 1.0 / vec2(textureSize(shadowMaps[1], 0));
                if (slot == 2) return 1.0 / vec2(textureSize(shadowMaps[2], 0));
                return 1.0 / vec2(textureSize(shadowMaps[3], 0));
            }

            float twoDimensionalShadow(int slot, vec3 viewPosition, vec3 viewNormal) {
                if (!receiveShadow || slot < 0) return 1.0;
                vec3 biasedPosition = viewPosition + viewNormal * shadowNormalBiases[slot];
                vec4 projected = shadowMatrices[slot] * vec4(biasedPosition, 1.0);
                vec3 coordinate = projected.xyz / projected.w;
                if (coordinate.x < 0.0 || coordinate.x > 1.0
                        || coordinate.y < 0.0 || coordinate.y > 1.0
                        || coordinate.z < 0.0 || coordinate.z > 1.0) {
                    return 1.0;
                }
                vec2 texel = twoDimensionalTexelSize(slot);
                float comparison = coordinate.z - shadowBiases[slot];
                float visibility = 0.0;
                for (int y = -1; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        float stored = sampleTwoDimensionalMap(slot, coordinate.xy + vec2(x, y) * texel);
                        visibility += comparison <= stored ? 1.0 : 0.0;
                    }
                }
                return visibility / 9.0;
            }

            float samplePointMap(int slot, vec3 direction) {
                if (slot == 0) return texture(pointShadowMaps[0], direction).r;
                if (slot == 1) return texture(pointShadowMaps[1], direction).r;
                if (slot == 2) return texture(pointShadowMaps[2], direction).r;
                return texture(pointShadowMaps[3], direction).r;
            }

            float pointShadow(int slot, vec3 viewPosition, vec3 viewNormal) {
                if (!receiveShadow || slot < 0) return 1.0;
                vec3 biasedPosition = viewPosition + viewNormal * pointShadowNormalBiases[slot];
                vec3 viewDirection = biasedPosition - pointShadowPositions[slot];
                float comparison = length(viewDirection) / pointShadowFarPlanes[slot]
                        - pointShadowBiases[slot];
                vec3 worldDirection = shadowViewToWorldMatrix * viewDirection;
                float diskRadius = 0.004 * (1.0 + comparison);
                const vec3 offsets[8] = vec3[](
                        vec3(1.0, 1.0, 1.0), vec3(-1.0, 1.0, 1.0),
                        vec3(1.0, -1.0, 1.0), vec3(-1.0, -1.0, 1.0),
                        vec3(1.0, 1.0, -1.0), vec3(-1.0, 1.0, -1.0),
                        vec3(1.0, -1.0, -1.0), vec3(-1.0, -1.0, -1.0));
                float visibility = 0.0;
                for (int sampleIndex = 0; sampleIndex < 8; sampleIndex++) {
                    float stored = samplePointMap(slot, worldDirection + offsets[sampleIndex] * diskRadius);
                    visibility += comparison <= stored ? 1.0 : 0.0;
                }
                return visibility / 8.0;
            }
            """.replace(
                    "TWO_DIMENSIONAL_SHADOW_CAPACITY", Integer.toString(ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS))
            .replace("POINT_SHADOW_CAPACITY", Integer.toString(ShadowFrame.MAX_POINT_SHADOWS));

    /** Prevents instantiation of this source fragment holder. */
    private ShadowShaderSource() {
        throw new AssertionError("ShadowShaderSource cannot be instantiated");
    }

    /** Returns shared shadow declarations and functions. */
    static String source() {
        return SOURCE;
    }
}
