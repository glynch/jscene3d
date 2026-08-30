/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Base scene node for a camera with automatic projection and view matrices.
 *
 * <p>Matrix accessors return stable live read-only views. Projection matrices are recomputed lazily
 * after a projection property changes. The view matrix is recomputed lazily after this camera or
 * one of its ancestors changes transform.
 *
 * <p>Cameras look down their local negative Z axis and use positive Y as the normal world-space up
 * direction. Camera instances are mutable and are not thread-safe.
 */
public abstract sealed class Camera extends Object3D permits OrthographicCamera, PerspectiveCamera {
    private static final float PARALLEL_UP_THRESHOLD = 1.0e-6f;

    private final Matrix4f projectionMatrix;
    private final Matrix4f inverseProjectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f lookAtMatrix;
    private final Matrix4f inverseParentWorldMatrix;
    private final Vector3f worldPosition;
    private final Vector3f lookDirection;
    private final Vector3f lookUp;
    private final Quaternionf localQuaternion;

    private long projectionVersion;
    private long resolvedProjectionVersion;
    private long resolvedViewWorldMatrixVersion;

    Camera() {
        projectionMatrix = new Matrix4f();
        inverseProjectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        lookAtMatrix = new Matrix4f();
        inverseParentWorldMatrix = new Matrix4f();
        worldPosition = new Vector3f();
        lookDirection = new Vector3f();
        lookUp = new Vector3f();
        localQuaternion = new Quaternionf();
        projectionVersion = 1L;
        resolvedProjectionVersion = -1L;
        resolvedViewWorldMatrixVersion = -1L;
    }

    /**
     * Returns the current projection matrix, recomputing it lazily when necessary.
     *
     * @return the stable live read-only projection-matrix view
     */
    public final Matrix4fc projectionMatrix() {
        updateProjectionMatrices();
        return projectionMatrix;
    }

    /**
     * Returns the inverse of the current projection matrix.
     *
     * @return the stable live read-only inverse-projection-matrix view
     */
    public final Matrix4fc inverseProjectionMatrix() {
        updateProjectionMatrices();
        return inverseProjectionMatrix;
    }

    /**
     * Returns the inverse of this camera's current world transform.
     *
     * @return the stable live read-only view-matrix view
     * @throws IllegalStateException if the camera world transform is not invertible
     */
    public final Matrix4fc viewMatrix() {
        Matrix4fc currentMatrixWorld = matrixWorld();
        long currentWorldMatrixVersion = matrixWorldVersion();
        if (resolvedViewWorldMatrixVersion != currentWorldMatrixVersion) {
            float determinant = currentMatrixWorld.determinant();
            if (!Float.isFinite(determinant) || determinant == 0.0f) {
                throw new IllegalStateException("Camera world transform must be finite and invertible");
            }
            currentMatrixWorld.invert(viewMatrix);
            if (!viewMatrix.isFinite()) {
                throw new IllegalStateException("Camera world transform must produce a finite view matrix");
            }
            resolvedViewWorldMatrixVersion = currentWorldMatrixVersion;
        }
        return viewMatrix;
    }

    /**
     * Returns the current projection-state version.
     *
     * <p>The version changes only when a projection property actually changes. It can be used to
     * avoid redundant renderer work; callers must not interpret its numeric value.
     *
     * @return the projection-state version
     */
    public final long projectionVersion() {
        return projectionVersion;
    }

    /**
     * Aims this camera's local negative Z axis at a world-space target.
     *
     * <p>Positive world Y is used as up. When the viewing direction is parallel to world Y, a
     * deterministic positive-Z fallback resolves the otherwise undefined roll.
     *
     * @param x target world X coordinate
     * @param y target world Y coordinate
     * @param z target world Z coordinate
     * @throws IllegalArgumentException if any coordinate is not finite or the target equals the
     *     camera's world position
     * @throws IllegalStateException if a parent world transform is not finite and invertible
     */
    public final void lookAt(float x, float y, float z) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        worldPosition(worldPosition);
        lookDirection.set(validX, validY, validZ).sub(worldPosition);
        float largestComponent =
                Math.max(Math.max(Math.abs(lookDirection.x), Math.abs(lookDirection.y)), Math.abs(lookDirection.z));
        if (!Float.isFinite(largestComponent)) {
            throw new IllegalArgumentException("target direction must be finite");
        }
        if (largestComponent == 0.0f) {
            throw new IllegalArgumentException("target must differ from the camera's world position");
        }
        lookDirection.div(largestComponent).normalize();

        lookUp.set(0.0f, 1.0f, 0.0f);
        float horizontalLengthSquared = lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z;
        if (horizontalLengthSquared < PARALLEL_UP_THRESHOLD) {
            lookUp.set(0.0f, 0.0f, 1.0f);
        }

        Object3D currentParent = parent();
        if (currentParent != null) {
            Matrix4fc parentMatrixWorld = currentParent.matrixWorld();
            float parentDeterminant = parentMatrixWorld.determinant();
            if (!Float.isFinite(parentDeterminant) || parentDeterminant == 0.0f) {
                throw new IllegalStateException("Camera parent world transform must be finite and invertible");
            }
            parentMatrixWorld.invert(inverseParentWorldMatrix);
            if (!inverseParentWorldMatrix.isFinite()) {
                throw new IllegalStateException("Camera parent world transform must have a finite inverse");
            }
            inverseParentWorldMatrix.transformDirection(lookDirection).normalize();
            inverseParentWorldMatrix.transformDirection(lookUp).normalize();
        }

        lookAtMatrix.setLookAlong(lookDirection, lookUp).invert().getNormalizedRotation(localQuaternion);
        setQuaternion(localQuaternion);
    }

    /**
     * Aims this camera's local negative Z axis at a world-space target.
     *
     * @param target target world position
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if any coordinate is not finite or the target equals the
     *     camera's world position
     * @throws IllegalStateException if a parent world transform is not finite and invertible
     */
    public final void lookAt(Vector3fc target) {
        Vector3fc validTarget = Preconditions.requireFinite(target, "target");
        lookAt(validTarget.x(), validTarget.y(), validTarget.z());
    }

    final void markProjectionChanged() {
        projectionVersion++;
    }

    abstract void calculateProjectionMatrix(Matrix4f destination);

    private void updateProjectionMatrices() {
        if (resolvedProjectionVersion != projectionVersion) {
            calculateProjectionMatrix(projectionMatrix);
            projectionMatrix.invert(inverseProjectionMatrix);
            resolvedProjectionVersion = projectionVersion;
        }
    }
}
