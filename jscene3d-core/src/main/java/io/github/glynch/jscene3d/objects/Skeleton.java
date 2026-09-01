/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Ordered bones and inverse bind matrices used for linear-blend skinning.
 *
 * <p>Bone order is stable and corresponds directly to indices stored in a skinned geometry's
 * {@link BufferGeometry#JOINTS joints} attribute. Inverse bind matrices are copied during
 * construction. Bones remain caller-owned mutable scene nodes, while this skeleton is an immutable
 * binding description.
 */
public final class Skeleton {
    /** Number of scalar values in one column-major 4-by-4 joint matrix. */
    public static final int MATRIX_COMPONENTS = 16;

    private final List<Bone> bones;
    private final Matrix4f[] inverseBindMatrices;
    private final Matrix4f meshWorldInverse = new Matrix4f();
    private final Matrix4f jointMatrix = new Matrix4f();

    /**
     * Creates a skeleton by copying an inverse bind matrix for every ordered bone.
     *
     * @param bones non-empty ordered bone list without duplicate identities
     * @param inverseBindMatrices one finite inverse bind matrix per bone
     * @throws NullPointerException if either list or any element is {@code null}
     * @throws IllegalArgumentException if the lists are empty, have different sizes, contain a
     *     duplicate bone, or contain a non-finite matrix
     */
    public Skeleton(List<? extends Bone> bones, List<? extends Matrix4fc> inverseBindMatrices) {
        List<? extends Bone> validBones = Objects.requireNonNull(bones, "bones");
        List<? extends Matrix4fc> validMatrices = Objects.requireNonNull(inverseBindMatrices, "inverseBindMatrices");
        if (validBones.isEmpty()) {
            throw new IllegalArgumentException("bones must not be empty");
        }
        if (validBones.size() != validMatrices.size()) {
            throw new IllegalArgumentException("inverseBindMatrices size must equal bones size: " + validMatrices.size()
                    + " != " + validBones.size());
        }

        Map<Bone, Boolean> identities = new IdentityHashMap<>();
        ArrayList<Bone> copiedBones = new ArrayList<>(validBones.size());
        this.inverseBindMatrices = new Matrix4f[validMatrices.size()];
        for (int index = 0; index < validBones.size(); index++) {
            Bone bone = Objects.requireNonNull(validBones.get(index), "bones[" + index + "]");
            if (identities.put(bone, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("bones contains duplicate identity at index " + index);
            }
            Matrix4fc inverseBindMatrix =
                    Objects.requireNonNull(validMatrices.get(index), "inverseBindMatrices[" + index + "]");
            if (!inverseBindMatrix.isFinite()) {
                throw new IllegalArgumentException("inverseBindMatrices[" + index + "] must be finite");
            }
            copiedBones.add(bone);
            this.inverseBindMatrices[index] = new Matrix4f(inverseBindMatrix);
        }
        this.bones = Collections.unmodifiableList(copiedBones);
    }

    /**
     * Captures inverse world transforms from the bones' current pose.
     *
     * <p>This convenience factory is appropriate when the bones are already in their bind pose.
     * Imported formats should use their explicit inverse bind matrices when supplied.
     *
     * @param bones non-empty ordered bind-pose bones
     * @return a skeleton bound to the current pose
     * @throws NullPointerException if the list or any bone is {@code null}
     * @throws IllegalArgumentException if a bone is duplicated or has a non-invertible world
     *     transform
     */
    public static Skeleton fromCurrentPose(List<? extends Bone> bones) {
        List<? extends Bone> validBones = Objects.requireNonNull(bones, "bones");
        ArrayList<Matrix4fc> inverseBindMatrices = new ArrayList<>(validBones.size());
        for (int index = 0; index < validBones.size(); index++) {
            Bone bone = Objects.requireNonNull(validBones.get(index), "bones[" + index + "]");
            Matrix4fc worldMatrix = bone.matrixWorld();
            if (!isInvertible(worldMatrix)) {
                throw new IllegalArgumentException("bones[" + index + "] world transform must be invertible");
            }
            inverseBindMatrices.add(new Matrix4f(worldMatrix).invert());
        }
        return new Skeleton(validBones, inverseBindMatrices);
    }

    /**
     * Returns the stable immutable ordered bone list.
     *
     * @return ordered bones addressed by geometry joint indices
     */
    public List<Bone> bones() {
        return bones;
    }

    /**
     * Returns the number of bound joints.
     *
     * @return positive joint count
     */
    public int jointCount() {
        return bones.size();
    }

    /**
     * Copies the current mesh-local joint palette into caller-owned storage.
     *
     * <p>Matrices use JOML/OpenGL column-major scalar order and are written consecutively. This
     * operation updates bone world transforms automatically and performs no allocation after
     * construction.
     *
     * @param meshWorldMatrix current skinned-mesh world transform
     * @param destination array whose length is exactly {@code jointCount() * MATRIX_COMPONENTS}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the destination size differs or the mesh transform is
     *     non-finite or non-invertible
     */
    public void copyJointMatrices(Matrix4fc meshWorldMatrix, float[] destination) {
        Matrix4fc validMeshWorldMatrix = Objects.requireNonNull(meshWorldMatrix, "meshWorldMatrix");
        float[] validDestination = Objects.requireNonNull(destination, "destination");
        int expectedLength = Math.multiplyExact(bones.size(), MATRIX_COMPONENTS);
        if (validDestination.length != expectedLength) {
            throw new IllegalArgumentException("destination length must equal jointCount * 16: "
                    + validDestination.length + " != " + expectedLength);
        }
        if (!validMeshWorldMatrix.isFinite() || !isInvertible(validMeshWorldMatrix)) {
            throw new IllegalArgumentException("meshWorldMatrix must be finite and invertible");
        }
        meshWorldInverse.set(validMeshWorldMatrix).invert();
        for (int index = 0; index < bones.size(); index++) {
            jointMatrix
                    .set(meshWorldInverse)
                    .mul(bones.get(index).matrixWorld())
                    .mul(inverseBindMatrices[index])
                    .get(validDestination, index * MATRIX_COMPONENTS);
        }
    }

    /** Returns whether a finite matrix has a usable inverse. */
    private static boolean isInvertible(Matrix4fc matrix) {
        float determinant = matrix.determinant();
        return Float.isFinite(determinant) && Math.abs(determinant) > 1.0e-8f;
    }
}
