/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class SkeletonTest {
    @Test
    void computesMeshLocalJointMatricesFromCurrentWorldTransforms() {
        Bone root = new Bone();
        Bone child = new Bone();
        root.add(child);
        child.setPosition(0.0f, 2.0f, 0.0f);
        Skeleton skeleton = Skeleton.fromCurrentPose(List.of(root, child));
        child.setPosition(0.0f, 3.0f, 0.0f);
        float[] matrices = new float[skeleton.jointCount() * Skeleton.MATRIX_COMPONENTS];

        skeleton.copyJointMatrices(new Matrix4f(), matrices);

        assertThat(skeleton.bones()).containsExactly(root, child);
        assertThat(matrices[12]).isZero();
        assertThat(matrices[13]).isZero();
        assertThat(matrices[16 + 13]).isEqualTo(1.0f);
    }

    @Test
    void copiesSuppliedInverseBindMatrices() {
        Bone bone = new Bone();
        Matrix4f inverseBind = new Matrix4f().translation(-2.0f, 0.0f, 0.0f);
        Skeleton skeleton = new Skeleton(List.of(bone), List.of(inverseBind));
        inverseBind.identity();
        bone.setPosition(3.0f, 0.0f, 0.0f);
        float[] matrices = new float[Skeleton.MATRIX_COMPONENTS];

        skeleton.copyJointMatrices(new Matrix4f(), matrices);

        assertThat(matrices[12]).isEqualTo(1.0f);
    }

    @Test
    void rejectsInvalidBindingsAndOutputStorage() {
        Bone bone = new Bone();

        assertThatIllegalArgumentException().isThrownBy(() -> new Skeleton(List.of(), List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Skeleton(List.of(bone), List.of(new Matrix4f(), new Matrix4f())));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Skeleton(List.of(bone, bone), List.of(new Matrix4f(), new Matrix4f())));

        Skeleton skeleton = new Skeleton(List.of(bone), List.of(new Matrix4f()));
        assertThatIllegalArgumentException().isThrownBy(() -> skeleton.copyJointMatrices(new Matrix4f(), new float[1]));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> skeleton.copyJointMatrices(new Matrix4f().scaling(0.0f), new float[16]));
    }
}
