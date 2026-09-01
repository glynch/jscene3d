/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import io.github.glynch.jscene3d.render.internal.resources.EnvironmentResource;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

/** Fullscreen renderer-owned equirectangular HDR background program. */
public final class EnvironmentBackgroundProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            out vec2 clipCoordinate;

            void main() {
                vec2 position = vec2(
                        gl_VertexID == 1 ? 3.0 : -1.0,
                        gl_VertexID == 2 ? 3.0 : -1.0);
                clipCoordinate = position;
                gl_Position = vec4(position, 1.0, 1.0);
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            const float PI = 3.141592653589793;

            in vec2 clipCoordinate;

            uniform sampler2D environmentMap;
            uniform mat4 inverseProjectionMatrix;
            uniform mat3 viewToWorldMatrix;
            uniform mat3 environmentRotationMatrix;
            uniform float backgroundIntensity;

            out vec4 fragmentColor;

            vec2 equirectangularCoordinate(vec3 direction) {
                vec3 normalizedDirection = normalize(direction);
                float u = atan(normalizedDirection.z, normalizedDirection.x) / (2.0 * PI) + 0.5;
                float v = acos(clamp(normalizedDirection.y, -1.0, 1.0)) / PI;
                return vec2(u, v);
            }

            void main() {
                vec4 viewPosition = inverseProjectionMatrix * vec4(clipCoordinate, 1.0, 1.0);
                vec3 worldDirection = viewToWorldMatrix * normalize(viewPosition.xyz / viewPosition.w);
                vec3 sampleDirection = environmentRotationMatrix * worldDirection;
                vec3 radiance = texture(environmentMap, equirectangularCoordinate(sampleDirection)).rgb;
                fragmentColor = vec4(radiance * backgroundIntensity, 1.0);
            }
            """;

    private final int id;
    private final int vertexArray;
    private final int environmentMapLocation;
    private final int inverseProjectionMatrixLocation;
    private final int viewToWorldMatrixLocation;
    private final int environmentRotationMatrixLocation;
    private final int backgroundIntensityLocation;
    private final float[] matrix4Values = new float[16];
    private final float[] matrix3Values = new float[9];

    /** Resolves required uniforms and creates the empty fullscreen vertex array. */
    private EnvironmentBackgroundProgram(int id) {
        this.id = id;
        String label = "Environment background";
        environmentMapLocation = ProgramSupport.requiredUniform(id, label, "environmentMap");
        inverseProjectionMatrixLocation = ProgramSupport.requiredUniform(id, label, "inverseProjectionMatrix");
        viewToWorldMatrixLocation = ProgramSupport.requiredUniform(id, label, "viewToWorldMatrix");
        environmentRotationMatrixLocation = ProgramSupport.requiredUniform(id, label, "environmentRotationMatrix");
        backgroundIntensityLocation = ProgramSupport.requiredUniform(id, label, "backgroundIntensity");
        vertexArray = glGenVertexArrays();
    }

    /**
     * Compiles and links the background program.
     *
     * @return linked renderer-owned program
     */
    public static EnvironmentBackgroundProgram create() {
        int program = ProgramSupport.createLinkedProgram("Environment background", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new EnvironmentBackgroundProgram(program);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /**
     * Draws one raw HDR background into the active scene target.
     *
     * @param environment context-local environment textures
     * @param inverseProjection inverse camera projection
     * @param viewToWorld camera-view to world-space rotation
     * @param environmentRotation world-to-environment rotation
     * @param intensity background radiance multiplier
     */
    public void render(
            EnvironmentResource environment,
            Matrix4fc inverseProjection,
            Matrix3fc viewToWorld,
            Matrix3fc environmentRotation,
            float intensity) {
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glUseProgram(id);
        inverseProjection.get(matrix4Values);
        glUniformMatrix4fv(inverseProjectionMatrixLocation, false, matrix4Values);
        viewToWorld.get(matrix3Values);
        glUniformMatrix3fv(viewToWorldMatrixLocation, false, matrix3Values);
        environmentRotation.get(matrix3Values);
        glUniformMatrix3fv(environmentRotationMatrixLocation, false, matrix3Values);
        glUniform1f(backgroundIntensityLocation, intensity);
        glActiveTexture(GL_TEXTURE0);
        environment.bindSource();
        glUniform1i(environmentMapLocation, 0);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
    }

    /** Deletes program and vertex-array names. */
    @Override
    public void close() {
        glDeleteVertexArrays(vertexArray);
        glDeleteProgram(id);
    }
}
