/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glUniform1i;

/** Resolved uniform state shared by built-in morph-capable programs. */
final class MorphProgramState {
    static final int TARGET_TEXTURE_UNIT = 16;
    static final int WEIGHT_TEXTURE_UNIT = 17;

    private final int useMorphTargetsLocation;
    private final int useInstanceMorphWeightsLocation;
    private final int morphTargetCountLocation;
    private final int morphVertexCountLocation;
    private final int morphTargetDataLocation;
    private final int morphWeightDataLocation;

    /** Resolves every required morph-target uniform. */
    MorphProgramState(int program, String label) {
        useMorphTargetsLocation = ProgramSupport.requiredUniform(program, label, "useMorphTargets");
        useInstanceMorphWeightsLocation = ProgramSupport.requiredUniform(program, label, "useInstanceMorphWeights");
        morphTargetCountLocation = ProgramSupport.requiredUniform(program, label, "morphTargetCount");
        morphVertexCountLocation = ProgramSupport.requiredUniform(program, label, "morphVertexCount");
        morphTargetDataLocation = ProgramSupport.requiredUniform(program, label, "morphTargetData");
        morphWeightDataLocation = ProgramSupport.requiredUniform(program, label, "morphWeightData");
    }

    /** Uploads the deformation layout for the current draw. */
    void upload(boolean enabled, int targetCount, int vertexCount, boolean instanceWeights) {
        glUniform1i(useMorphTargetsLocation, enabled ? 1 : 0);
        glUniform1i(useInstanceMorphWeightsLocation, instanceWeights ? 1 : 0);
        glUniform1i(morphTargetCountLocation, targetCount);
        glUniform1i(morphVertexCountLocation, vertexCount);
        glUniform1i(morphTargetDataLocation, TARGET_TEXTURE_UNIT);
        glUniform1i(morphWeightDataLocation, WEIGHT_TEXTURE_UNIT);
    }
}
