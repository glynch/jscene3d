/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.project.runtime.Entity;

/** World-owned perspective-camera component backed by one hidden JScene3D camera. */
final class Object3dPerspectiveCamera extends Object3dCameraComponent<PerspectiveCamera>
        implements PerspectiveCamera3d {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float RADIANS_TO_DEGREES = (float) (180.0 / Math.PI);

    /** Creates complete projection state using a placeholder aspect ratio until first render. */
    Object3dPerspectiveCamera(
            Object3dSpatialAdapter adapter,
            Entity owner,
            float fieldOfViewDegrees,
            float near,
            float far,
            boolean primary) {
        super(
                adapter,
                owner,
                new PerspectiveCamera(radians(fieldOfViewDegrees), 1.0F, near, far),
                primary,
                "PerspectiveCamera3d");
    }

    @Override
    public float fieldOfViewDegrees() {
        requireOpen();
        return camera().fieldOfView() * RADIANS_TO_DEGREES;
    }

    @Override
    public float near() {
        requireOpen();
        return camera().near();
    }

    @Override
    public float far() {
        requireOpen();
        return camera().far();
    }

    @Override
    public void setFieldOfViewDegrees(float fieldOfViewDegrees) {
        requireOpen();
        camera().setFieldOfView(radians(fieldOfViewDegrees));
    }

    @Override
    public void setClippingPlanes(float near, float far) {
        requireOpen();
        camera().setClippingPlanes(near, far);
    }

    /** Updates the current projection aspect ratio. */
    @Override
    void prepareForRender(float aspectRatio) {
        camera().setAspectRatio(aspectRatio);
    }

    /** Converts and validates a field of view expressed in authored degrees. */
    private static float radians(float degrees) {
        if (!Float.isFinite(degrees) || degrees <= 0.0F || degrees >= 180.0F) {
            throw new IllegalArgumentException(
                    "fieldOfViewDegrees must be finite and between zero and 180: " + degrees);
        }
        return degrees * DEGREES_TO_RADIANS;
    }
}
