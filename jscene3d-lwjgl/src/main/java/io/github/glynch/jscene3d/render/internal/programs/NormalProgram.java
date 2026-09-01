/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.fogs.Fog;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Compiled built-in normal-visualization mesh program. */
public final class NormalProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec3 normal;

            INSTANCING_SOURCE
            MORPH_SOURCE

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform mat3 normalMatrix;

            out vec3 resolvedViewNormal;
            out float resolvedFogDepth;

            void main() {
                mat4 instanceMatrix = resolvedInstanceMatrix();
                vec4 viewPosition = viewMatrix * modelMatrix * instanceMatrix
                        * vec4(resolvedMorphPosition(position), 1.0);
                resolvedViewNormal = normalize(normalMatrix * resolvedInstanceNormalMatrix(instanceMatrix)
                        * resolvedMorphNormal(normal));
                resolvedFogDepth = -viewPosition.z;
                gl_Position = projectionMatrix * viewPosition;
            }
            """.replace("INSTANCING_SOURCE", InstancingShaderSource.source())
            .replace("MORPH_SOURCE", MorphShaderSource.source());
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec3 resolvedViewNormal;
            in float resolvedFogDepth;

            uniform float opacity;
            uniform float alphaCutoff;

            FOG_SOURCE

            out vec4 fragmentColor;

            void main() {
                if (alphaCutoff >= 0.0 && opacity < alphaCutoff) {
                    discard;
                }
                vec3 surfaceNormal = gl_FrontFacing ? resolvedViewNormal : -resolvedViewNormal;
                vec3 normalColor = normalize(surfaceNormal) * 0.5 + 0.5;
                fragmentColor = vec4(applyFog(normalColor, resolvedFogDepth), opacity);
            }
            """.replace("FOG_SOURCE", FogShaderSource.source());

    private final int id;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int normalMatrixLocation;
    private final int opacityLocation;
    private final int alphaCutoffLocation;
    private final FogProgramState fogState;
    private final InstancingProgramState instancingState;
    private final MorphProgramState morphState;
    private final Matrix4f modelViewMatrix;
    private final Matrix3f normalMatrix;
    private final float[] matrix4Values;
    private final float[] matrix3Values;

    /** Retains a linked program and reusable transform staging. */
    private NormalProgram(int id) {
        this.id = id;
        modelMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "projectionMatrix");
        normalMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "normalMatrix");
        opacityLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "opacity");
        alphaCutoffLocation = ProgramSupport.requiredUniform(id, "Built-in Normal", "alphaCutoff");
        fogState = new FogProgramState(id, "Built-in Normal");
        instancingState = new InstancingProgramState(id, "Built-in Normal");
        morphState = new MorphProgramState(id, "Built-in Normal");
        modelViewMatrix = new Matrix4f();
        normalMatrix = new Matrix3f();
        matrix4Values = new float[16];
        matrix3Values = new float[9];
    }

    /**
     * Compiles, links, validates, and returns the built-in normal program.
     *
     * @return linked normal program
     */
    public static NormalProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in Normal", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new NormalProgram(program);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /**
     * Returns the context-local OpenGL program name.
     *
     * @return OpenGL program name
     */
    public int id() {
        return id;
    }

    /**
     * Returns the required opacity uniform location.
     *
     * @return uniform location
     */
    public int opacityLocation() {
        return opacityLocation;
    }

    /**
     * Returns the required alpha-cutoff uniform location.
     *
     * @return uniform location
     */
    public int alphaCutoffLocation() {
        return alphaCutoffLocation;
    }

    /**
     * Uploads the current optional scene fog.
     *
     * @param fog scene fog, or {@code null} when disabled
     */
    public void uploadFog(@Nullable Fog fog) {
        fogState.upload(fog);
    }

    /**
     * Uploads optional batch-transform and color switches.
     *
     * @param instanced whether the draw consumes per-instance transforms
     * @param colors whether the draw consumes per-instance colors
     */
    public void uploadInstancing(boolean instanced, boolean colors) {
        instancingState.upload(instanced, colors);
    }

    /**
     * Uploads the current morph-target data layout.
     *
     * @param enabled whether morph deformation is enabled
     * @param targetCount number of morph targets
     * @param vertexCount number of vertices in each target
     * @param instanceWeights whether weights vary by instance
     */
    public void uploadMorphing(boolean enabled, int targetCount, int vertexCount, boolean instanceWeights) {
        morphState.upload(enabled, targetCount, vertexCount, instanceWeights);
    }

    /**
     * Uploads object, camera, and inverse-transpose normal transforms without allocating.
     *
     * @param modelMatrix object model matrix
     * @param viewMatrix current view matrix
     * @param projectionMatrix current projection matrix
     */
    public void uploadTransforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        uploadMatrix4(modelMatrixLocation, modelMatrix);
        uploadMatrix4(viewMatrixLocation, viewMatrix);
        uploadMatrix4(projectionMatrixLocation, projectionMatrix);
        modelViewMatrix.set(viewMatrix).mul(modelMatrix);
        normalMatrix.set(modelViewMatrix).normal().get(matrix3Values);
        glUniformMatrix3fv(normalMatrixLocation, false, matrix3Values);
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }

    /** Copies and uploads one four-by-four matrix. */
    private void uploadMatrix4(int location, Matrix4fc matrix) {
        matrix.get(matrix4Values);
        glUniformMatrix4fv(location, false, matrix4Values);
    }
}
