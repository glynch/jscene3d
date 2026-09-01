/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;

/** Required switch uniforms shared by built-in programs with optional instancing. */
final class InstancingProgramState {
    private final int useInstancingLocation;
    private final int useInstanceColorLocation;

    /** Resolves the required transform switch and the optional color switch. */
    InstancingProgramState(int program, String label) {
        useInstancingLocation = ProgramSupport.requiredUniform(program, label, "useInstancing");
        useInstanceColorLocation = glGetUniformLocation(program, "useInstanceColor");
    }

    /** Uploads whether the current draw consumes instance transforms and colors. */
    void upload(boolean instanced, boolean colors) {
        glUniform1i(useInstancingLocation, instanced ? 1 : 0);
        glUniform1i(useInstanceColorLocation, instanced && colors ? 1 : 0);
    }
}
