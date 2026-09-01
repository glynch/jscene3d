/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;

import io.github.glynch.jscene3d.fogs.ExponentialSquaredFog;
import io.github.glynch.jscene3d.fogs.Fog;
import io.github.glynch.jscene3d.fogs.LinearFog;
import io.github.glynch.jscene3d.math.Color;
import org.jspecify.annotations.Nullable;

/** Required uniform locations and upload logic shared by built-in fog-aware programs. */
final class FogProgramState {
    private static final int DISABLED = 0;
    private static final int LINEAR = 1;
    private static final int EXPONENTIAL_SQUARED = 2;

    private final int modeLocation;
    private final int colorLocation;
    private final int nearLocation;
    private final int farLocation;
    private final int densityLocation;

    /** Resolves the fog uniforms from one linked built-in program. */
    FogProgramState(int program, String label) {
        modeLocation = ProgramSupport.requiredUniform(program, label, "fogMode");
        colorLocation = ProgramSupport.requiredUniform(program, label, "fogColor");
        nearLocation = ProgramSupport.requiredUniform(program, label, "fogNear");
        farLocation = ProgramSupport.requiredUniform(program, label, "fogFar");
        densityLocation = ProgramSupport.requiredUniform(program, label, "fogDensity");
    }

    /** Uploads disabled, linear, or exponential-squared fog without retaining scene state. */
    void upload(@Nullable Fog fog) {
        if (fog == null) {
            glUniform1i(modeLocation, DISABLED);
            return;
        }
        Color color = fog.color();
        glUniform3f(colorLocation, color.red(), color.green(), color.blue());
        switch (fog) {
            case LinearFog linearFog -> uploadLinear(linearFog);
            case ExponentialSquaredFog exponentialFog -> uploadExponentialSquared(exponentialFog);
        }
    }

    /** Uploads the linear-fog mode and its distance interval. */
    private void uploadLinear(LinearFog fog) {
        glUniform1i(modeLocation, LINEAR);
        glUniform1f(nearLocation, fog.nearDistance());
        glUniform1f(farLocation, fog.farDistance());
        glUniform1f(densityLocation, 0.0f);
    }

    /** Uploads the exponential-squared mode and its density. */
    private void uploadExponentialSquared(ExponentialSquaredFog fog) {
        glUniform1i(modeLocation, EXPONENTIAL_SQUARED);
        glUniform1f(nearLocation, 0.0f);
        glUniform1f(farLocation, 1.0f);
        glUniform1f(densityLocation, fog.density());
    }
}
