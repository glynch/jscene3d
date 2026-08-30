/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.BoundingSphere;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Reusable renderer-internal camera frustum and world-space sphere test. */
final class Frustum {
    private final Matrix4f viewProjectionMatrix;
    private final FrustumIntersection intersection;
    private final Vector3f worldCenter;
    private final Vector3f worldScale;

    /** Creates reusable matrices and intersection state. */
    Frustum() {
        viewProjectionMatrix = new Matrix4f();
        intersection = new FrustumIntersection();
        worldCenter = new Vector3f();
        worldScale = new Vector3f();
    }

    /** Replaces the frustum from current view and projection matrices. */
    void update(Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
        intersection.set(viewProjectionMatrix);
    }

    /** Tests a local bounding sphere after applying its object's world transform. */
    boolean intersects(BoundingSphere sphere, Matrix4fc worldMatrix) {
        worldMatrix.transformPosition(sphere.center(), worldCenter);
        worldMatrix.getScale(worldScale);
        float maximumScale = Math.max(Math.max(Math.abs(worldScale.x), Math.abs(worldScale.y)), Math.abs(worldScale.z));
        return intersection.testSphere(worldCenter, sphere.radius() * maximumScale);
    }
}
