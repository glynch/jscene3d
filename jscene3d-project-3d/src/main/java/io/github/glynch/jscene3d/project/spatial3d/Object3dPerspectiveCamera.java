/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import java.util.Objects;

/** World-owned perspective-camera component backed by one hidden JScene3D camera. */
final class Object3dPerspectiveCamera implements PerspectiveCamera3d, ComponentLifecycleCallbacks {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float RADIANS_TO_DEGREES = (float) (180.0 / Math.PI);

    private final Object3dSpatialAdapter adapter;
    private final Entity owner;
    private final PerspectiveCamera camera;
    private final boolean primary;

    private boolean active;
    private boolean closed;

    /** Creates complete projection state using a placeholder aspect ratio until first render. */
    Object3dPerspectiveCamera(
            Object3dSpatialAdapter adapter,
            Entity owner,
            float fieldOfViewDegrees,
            float near,
            float far,
            boolean primary) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        camera = new PerspectiveCamera(radians(fieldOfViewDegrees), 1.0F, near, far);
        this.primary = primary;
    }

    @Override
    public float fieldOfViewDegrees() {
        requireOpen();
        return camera.fieldOfView() * RADIANS_TO_DEGREES;
    }

    @Override
    public float near() {
        requireOpen();
        return camera.near();
    }

    @Override
    public float far() {
        requireOpen();
        return camera.far();
    }

    @Override
    public boolean isPrimary() {
        requireOpen();
        return primary;
    }

    @Override
    public void setFieldOfViewDegrees(float fieldOfViewDegrees) {
        requireOpen();
        camera.setFieldOfView(radians(fieldOfViewDegrees));
    }

    @Override
    public void setClippingPlanes(float near, float far) {
        requireOpen();
        camera.setClippingPlanes(near, far);
    }

    @Override
    public void onActivated() {
        requireOpen();
        active = true;
    }

    @Override
    public void onDeactivated() {
        requireOpen();
        active = false;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        active = false;
        adapter.release(this);
    }

    /** Returns the hidden backend camera. */
    PerspectiveCamera camera() {
        return camera;
    }

    /** Returns the owning live entity. */
    Entity owner() {
        return owner;
    }

    /** Returns whether entity lifecycle currently permits camera selection. */
    boolean isActive() {
        return active && !closed;
    }

    /** Updates the current projection aspect ratio. */
    void setAspectRatio(float aspectRatio) {
        camera.setAspectRatio(aspectRatio);
    }

    /** Converts and validates a field of view expressed in authored degrees. */
    private static float radians(float degrees) {
        if (!Float.isFinite(degrees) || degrees <= 0.0F || degrees >= 180.0F) {
            throw new IllegalArgumentException(
                    "fieldOfViewDegrees must be finite and between zero and 180: " + degrees);
        }
        return degrees * DEGREES_TO_RADIANS;
    }

    /** Rejects projection access after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("PerspectiveCamera3d is closed");
        }
    }
}
