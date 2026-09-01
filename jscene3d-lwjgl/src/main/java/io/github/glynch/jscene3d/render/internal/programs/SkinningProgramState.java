/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.Skeleton;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import io.github.glynch.jscene3d.render.Renderer;
import org.joml.Matrix4fc;

/** Uniform locations and reusable joint-matrix staging shared by built-in skinned programs. */
public final class SkinningProgramState {
    private final int useSkinningLocation;
    private final int jointMatricesLocation;
    private float[] jointMatrices = new float[0];

    /**
     * Resolves the required skinning uniforms for one linked built-in program.
     *
     * @param program linked OpenGL program identifier
     * @param label program label included in missing-uniform diagnostics
     */
    public SkinningProgramState(int program, String label) {
        useSkinningLocation = ProgramSupport.requiredUniform(program, label, "useSkinning");
        jointMatricesLocation = ProgramSupport.requiredUniform(program, label, "jointMatrices[0]");
    }

    /**
     * Uploads either an identity deformation switch or the current mesh-local joint palette.
     *
     * @param object current render object
     * @param meshWorldMatrix current object-to-world transform
     */
    public void upload(Object3D object, Matrix4fc meshWorldMatrix) {
        if (!(object instanceof SkinnedMesh skinnedMesh)) {
            glUniform1i(useSkinningLocation, 0);
            return;
        }
        int jointCount = skinnedMesh.skeleton().jointCount();
        if (jointCount > Renderer.MAX_SKIN_JOINTS) {
            throw new IllegalStateException(
                    "Skeleton joint count exceeds renderer limit: " + jointCount + " > " + Renderer.MAX_SKIN_JOINTS);
        }
        int valueCount = Math.multiplyExact(jointCount, Skeleton.MATRIX_COMPONENTS);
        if (jointMatrices.length != valueCount) {
            jointMatrices = new float[valueCount];
        }
        skinnedMesh.skeleton().copyJointMatrices(meshWorldMatrix, jointMatrices);
        glUniform1i(useSkinningLocation, 1);
        glUniformMatrix4fv(jointMatricesLocation, false, jointMatrices);
    }
}
