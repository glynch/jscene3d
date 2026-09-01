/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

/** Shared texture-buffer morph-target functions for built-in vertex programs. */
final class MorphShaderSource {
    private static final String SOURCE = """
            uniform bool useMorphTargets;
            uniform bool useInstanceMorphWeights;
            uniform int morphTargetCount;
            uniform int morphVertexCount;
            uniform samplerBuffer morphTargetData;
            uniform samplerBuffer morphWeightData;

            float resolvedMorphWeight(int targetIndex) {
                int weightRow = useInstanceMorphWeights ? gl_InstanceID : 0;
                return texelFetch(morphWeightData, weightRow * morphTargetCount + targetIndex).r;
            }

            vec3 resolvedMorphPosition(vec3 basePosition) {
                if (!useMorphTargets) {
                    return basePosition;
                }
                vec3 result = basePosition;
                for (int targetIndex = 0; targetIndex < morphTargetCount; targetIndex++) {
                    int texelIndex = 2 * (targetIndex * morphVertexCount + gl_VertexID);
                    result += texelFetch(morphTargetData, texelIndex).xyz * resolvedMorphWeight(targetIndex);
                }
                return result;
            }

            vec3 resolvedMorphNormal(vec3 baseNormal) {
                if (!useMorphTargets) {
                    return baseNormal;
                }
                vec3 result = baseNormal;
                for (int targetIndex = 0; targetIndex < morphTargetCount; targetIndex++) {
                    int texelIndex = 2 * (targetIndex * morphVertexCount + gl_VertexID) + 1;
                    result += texelFetch(morphTargetData, texelIndex).xyz * resolvedMorphWeight(targetIndex);
                }
                return result;
            }
            """;

    private MorphShaderSource() {}

    /** Returns the reusable GLSL declarations and deformation functions. */
    static String source() {
        return SOURCE;
    }
}
