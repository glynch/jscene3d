/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.project.runtime.Entity;
import java.util.Objects;

/** World-owned orthographic-camera component backed by one hidden JScene3D camera. */
final class Object3dOrthographicCamera extends Object3dCameraComponent<OrthographicCamera>
        implements OrthographicCamera3d {
    /** Creates complete validated projection state. */
    Object3dOrthographicCamera(
            Object3dSpatialAdapter adapter, Entity owner, OrthographicProjection3d projection, boolean primary) {
        super(adapter, owner, camera(projection), primary, "OrthographicCamera3d");
    }

    @Override
    public float left() {
        requireOpen();
        return camera().left();
    }

    @Override
    public float right() {
        requireOpen();
        return camera().right();
    }

    @Override
    public float top() {
        requireOpen();
        return camera().top();
    }

    @Override
    public float bottom() {
        requireOpen();
        return camera().bottom();
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
    public float zoom() {
        requireOpen();
        return camera().zoom();
    }

    @Override
    public void setBounds(float left, float right, float top, float bottom) {
        requireOpen();
        camera().setBounds(left, right, top, bottom);
    }

    @Override
    public void setClippingPlanes(float near, float far) {
        requireOpen();
        camera().setClippingPlanes(near, far);
    }

    @Override
    public void setZoom(float zoom) {
        requireOpen();
        camera().setZoom(zoom);
    }

    /** Creates the engine camera without duplicating projection validation or mathematics. */
    private static OrthographicCamera camera(OrthographicProjection3d projection) {
        OrthographicProjection3d validProjection = Objects.requireNonNull(projection, "projection");
        OrthographicCamera camera = new OrthographicCamera(
                validProjection.left(),
                validProjection.right(),
                validProjection.top(),
                validProjection.bottom(),
                validProjection.near(),
                validProjection.far());
        camera.setZoom(validProjection.zoom());
        return camera;
    }
}
