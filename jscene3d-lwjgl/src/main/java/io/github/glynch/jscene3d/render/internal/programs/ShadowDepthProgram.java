/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.render.Renderer;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

/** One projected or radial depth-only program used by shadow-map passes. */
public final class ShadowDepthProgram implements AutoCloseable {
    private static final String PROJECTED_VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 4) in vec4 jointIndices;
            layout(location = 5) in vec4 skinWeights;
            layout(location = 7) in vec4 instanceMatrixColumn0;
            layout(location = 8) in vec4 instanceMatrixColumn1;
            layout(location = 9) in vec4 instanceMatrixColumn2;
            layout(location = 10) in vec4 instanceMatrixColumn3;

            uniform mat4 lightViewProjectionMatrix;
            uniform mat4 modelMatrix;
            uniform bool useSkinning;
            uniform bool useInstancing;
            uniform mat4 jointMatrices[SKIN_JOINT_CAPACITY];

            MORPH_SOURCE

            void main() {
                mat4 skinMatrix = mat4(1.0);
                if (useSkinning) {
                    vec4 normalizedWeights = skinWeights / max(dot(skinWeights, vec4(1.0)), 1e-7);
                    skinMatrix = normalizedWeights.x * jointMatrices[int(jointIndices.x)]
                            + normalizedWeights.y * jointMatrices[int(jointIndices.y)]
                            + normalizedWeights.z * jointMatrices[int(jointIndices.z)]
                            + normalizedWeights.w * jointMatrices[int(jointIndices.w)];
                }
                mat4 instanceMatrix = useInstancing
                        ? mat4(instanceMatrixColumn0, instanceMatrixColumn1, instanceMatrixColumn2, instanceMatrixColumn3)
                        : mat4(1.0);
                gl_Position = lightViewProjectionMatrix * modelMatrix * instanceMatrix * skinMatrix
                        * vec4(resolvedMorphPosition(position), 1.0);
            }
            """.replace(
                    "SKIN_JOINT_CAPACITY", Integer.toString(Renderer.MAX_SKIN_JOINTS))
            .replace("MORPH_SOURCE", MorphShaderSource.source());
    private static final String PROJECTED_FRAGMENT_SOURCE = """
            #version 330 core

            void main() {
                // The fixed-function depth value is the complete projected shadow output.
            }
            """;
    private static final String POINT_VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 4) in vec4 jointIndices;
            layout(location = 5) in vec4 skinWeights;
            layout(location = 7) in vec4 instanceMatrixColumn0;
            layout(location = 8) in vec4 instanceMatrixColumn1;
            layout(location = 9) in vec4 instanceMatrixColumn2;
            layout(location = 10) in vec4 instanceMatrixColumn3;

            uniform mat4 lightViewProjectionMatrix;
            uniform mat4 modelMatrix;
            uniform bool useSkinning;
            uniform bool useInstancing;
            uniform mat4 jointMatrices[SKIN_JOINT_CAPACITY];

            MORPH_SOURCE

            out vec3 worldPosition;

            void main() {
                mat4 skinMatrix = mat4(1.0);
                if (useSkinning) {
                    vec4 normalizedWeights = skinWeights / max(dot(skinWeights, vec4(1.0)), 1e-7);
                    skinMatrix = normalizedWeights.x * jointMatrices[int(jointIndices.x)]
                            + normalizedWeights.y * jointMatrices[int(jointIndices.y)]
                            + normalizedWeights.z * jointMatrices[int(jointIndices.z)]
                            + normalizedWeights.w * jointMatrices[int(jointIndices.w)];
                }
                mat4 instanceMatrix = useInstancing
                        ? mat4(instanceMatrixColumn0, instanceMatrixColumn1, instanceMatrixColumn2, instanceMatrixColumn3)
                        : mat4(1.0);
                vec4 resolvedWorldPosition = modelMatrix * instanceMatrix * skinMatrix
                        * vec4(resolvedMorphPosition(position), 1.0);
                worldPosition = resolvedWorldPosition.xyz;
                gl_Position = lightViewProjectionMatrix * resolvedWorldPosition;
            }
            """.replace(
                    "SKIN_JOINT_CAPACITY", Integer.toString(Renderer.MAX_SKIN_JOINTS))
            .replace("MORPH_SOURCE", MorphShaderSource.source());
    private static final String POINT_FRAGMENT_SOURCE = """
            #version 330 core
            in vec3 worldPosition;

            uniform vec3 lightPosition;
            uniform float lightFarPlane;

            void main() {
                gl_FragDepth = length(worldPosition - lightPosition) / lightFarPlane;
            }
            """;

    private final int id;
    private final boolean radial;
    private final int lightViewProjectionMatrixLocation;
    private final int modelMatrixLocation;
    private final int useInstancingLocation;
    private final int lightPositionLocation;
    private final int lightFarPlaneLocation;
    private final SkinningProgramState skinningState;
    private final MorphProgramState morphState;
    private final float[] matrixValues;

    /** Retains one linked depth variant and reusable matrix staging. */
    private ShadowDepthProgram(int id, String label, boolean radial) {
        this.id = id;
        this.radial = radial;
        lightViewProjectionMatrixLocation = ProgramSupport.requiredUniform(id, label, "lightViewProjectionMatrix");
        modelMatrixLocation = ProgramSupport.requiredUniform(id, label, "modelMatrix");
        useInstancingLocation = ProgramSupport.requiredUniform(id, label, "useInstancing");
        lightPositionLocation = radial ? ProgramSupport.requiredUniform(id, label, "lightPosition") : -1;
        lightFarPlaneLocation = radial ? ProgramSupport.requiredUniform(id, label, "lightFarPlane") : -1;
        skinningState = new SkinningProgramState(id, label);
        morphState = new MorphProgramState(id, label);
        matrixValues = new float[16];
    }

    /**
     * Compiles a projected-depth variant that leaves depth generation to the fixed-function path.
     *
     * @return linked shadow-depth program
     */
    public static ShadowDepthProgram createProjected() {
        return create("Projected shadow depth", PROJECTED_VERTEX_SOURCE, PROJECTED_FRAGMENT_SOURCE, false);
    }

    /**
     * Compiles a point-light variant that writes normalized radial distance.
     *
     * @return linked radial shadow-depth program
     */
    public static ShadowDepthProgram createPoint() {
        return create("Point shadow depth", POINT_VERTEX_SOURCE, POINT_FRAGMENT_SOURCE, true);
    }

    /** Links one depth variant and closes its program if location resolution fails. */
    private static ShadowDepthProgram create(String label, String vertexSource, String fragmentSource, boolean radial) {
        int program = ProgramSupport.createLinkedProgram(label, vertexSource, fragmentSource);
        try {
            return new ShadowDepthProgram(program, label, radial);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /**
     * Returns the OpenGL program name.
     *
     * @return context-local program name
     */
    public int id() {
        return id;
    }

    /**
     * Uploads pass-wide projected-light camera state.
     *
     * @param lightViewProjection light view-projection transform
     * @throws IllegalStateException if this is the radial variant
     */
    public void uploadProjectedPass(Matrix4fc lightViewProjection) {
        if (radial) {
            throw new IllegalStateException("Cannot upload a projected pass to the point shadow-depth program");
        }
        uploadLightViewProjection(lightViewProjection);
    }

    /**
     * Uploads pass-wide point-light camera and radial-depth state.
     *
     * @param lightViewProjection light view-projection transform
     * @param position world-space light position
     * @param farPlane positive shadow-camera far distance
     * @throws IllegalStateException if this is the projected variant
     */
    public void uploadPointPass(Matrix4fc lightViewProjection, Vector3fc position, float farPlane) {
        if (!radial) {
            throw new IllegalStateException("Cannot upload a point pass to the projected shadow-depth program");
        }
        uploadLightViewProjection(lightViewProjection);
        glUniform3f(lightPositionLocation, position.x(), position.y(), position.z());
        glUniform1f(lightFarPlaneLocation, farPlane);
    }

    /** Uploads the common light view-projection matrix. */
    private void uploadLightViewProjection(Matrix4fc lightViewProjection) {
        lightViewProjection.get(matrixValues);
        glUniformMatrix4fv(lightViewProjectionMatrixLocation, false, matrixValues);
    }

    /**
     * Uploads the current caster model matrix.
     *
     * @param modelMatrix caster world transform
     */
    public void uploadModel(Matrix4fc modelMatrix) {
        modelMatrix.get(matrixValues);
        glUniformMatrix4fv(modelMatrixLocation, false, matrixValues);
    }

    /**
     * Uploads whether the current depth draw consumes per-instance transforms.
     *
     * @param instanced whether the draw consumes per-instance transforms
     */
    public void uploadInstancing(boolean instanced) {
        glUniform1i(useInstancingLocation, instanced ? 1 : 0);
    }

    /**
     * Uploads the current caster morph-target data layout.
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
     * Uploads the optional skeletal deformation palette for the current shadow caster.
     *
     * @param object current caster
     * @param modelMatrix current caster-to-world transform
     */
    public void uploadSkinning(Object3D object, Matrix4fc modelMatrix) {
        skinningState.upload(object, modelMatrix);
    }

    /** Deletes the linked program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
