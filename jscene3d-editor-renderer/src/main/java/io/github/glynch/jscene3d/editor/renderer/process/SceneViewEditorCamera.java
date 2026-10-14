/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.math.BoundingBox;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Session-local editor camera and orbit pivot, independent of authored Scene state. */
final class SceneViewEditorCamera {
    private static final float FIELD_OF_VIEW = (float) Math.toRadians(60.0);
    private static final float MINIMUM_DISTANCE = 0.05f;
    private static final float MAXIMUM_DISTANCE = 10_000.0f;
    private static final float POLAR_EPSILON = 0.001f;
    private static final float FRAME_PADDING = 1.25f;
    private static final float ORTHOGRAPHIC_HALF_HEIGHT = 10.0f;

    private final PerspectiveCamera perspective = new PerspectiveCamera(FIELD_OF_VIEW, 1.0f, 0.01f, 20_000.0f);
    private final OrthographicCamera orthographic = new OrthographicCamera(
            -ORTHOGRAPHIC_HALF_HEIGHT,
            ORTHOGRAPHIC_HALF_HEIGHT,
            ORTHOGRAPHIC_HALF_HEIGHT,
            -ORTHOGRAPHIC_HALF_HEIGHT,
            0.0f,
            20_000.0f);
    private final Vector3f pivot = new Vector3f();
    private final Vector3f position = new Vector3f();
    private final Vector3f viewDirection = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Vector3f orientationDegrees = new Vector3f();

    private Camera active = perspective;
    private float distance;
    private float azimuth;
    private float polar;

    /** Creates the conventional elevated perspective editor view around the Scene origin. */
    SceneViewEditorCamera() {
        perspective.setPosition(6.0f, 4.0f, 6.0f);
        synchronizeFrom(perspective.position());
        applyPose();
        synchronizeProjectionPose();
    }

    /** Returns the currently active perspective or orthographic camera. */
    Camera active() {
        return active;
    }

    /** Updates projection dimensions without changing the camera pose or pivot. */
    void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        float aspect = (float) width / height;
        perspective.setAspectRatio(aspect);
        orthographic.setBounds(
                -ORTHOGRAPHIC_HALF_HEIGHT * aspect,
                ORTHOGRAPHIC_HALF_HEIGHT * aspect,
                ORTHOGRAPHIC_HALF_HEIGHT,
                -ORTHOGRAPHIC_HALF_HEIGHT);
    }

    /** Orbits around the current pivot using logical pointer displacement. */
    void orbit(float horizontal, float vertical, int width, int height) {
        requireFinite(horizontal, "horizontal");
        requireFinite(vertical, "vertical");
        resize(width, height);
        float radiansPerPixel = (float) (Math.PI * 2.0) / height;
        azimuth -= horizontal * radiansPerPixel;
        polar = Math.clamp(polar + vertical * radiansPerPixel, POLAR_EPSILON, (float) Math.PI - POLAR_EPSILON);
        applyPose();
    }

    /** Pans the camera and pivot together in the current screen plane. */
    void pan(float horizontal, float vertical, int width, int height) {
        requireFinite(horizontal, "horizontal");
        requireFinite(vertical, "vertical");
        resize(width, height);
        position.set(active.position());
        viewDirection.set(pivot).sub(position).normalize();
        viewDirection.cross(0.0f, 1.0f, 0.0f, right);
        if (right.lengthSquared() < 1.0e-8f) {
            viewDirection.cross(0.0f, 0.0f, 1.0f, right);
        }
        right.normalize();
        right.cross(viewDirection, up).normalize();
        float verticalUnits = active instanceof PerspectiveCamera
                ? 2.0f * distance * (float) Math.tan(FIELD_OF_VIEW * 0.5f) / height
                : (orthographic.top() - orthographic.bottom()) / orthographic.zoom() / height;
        pivot.fma(-horizontal * verticalUnits, right).fma(vertical * verticalUnits, up);
        applyPose();
    }

    /** Dollies a perspective view or changes orthographic magnification. */
    void zoom(float amount) {
        requireFinite(amount, "amount");
        float factor = (float) Math.exp(Math.clamp(amount, -1000.0f, 1000.0f) * 0.0015f);
        if (active == perspective) {
            distance = Math.clamp(distance * factor, MINIMUM_DISTANCE, MAXIMUM_DISTANCE);
        } else {
            orthographic.setZoom(Math.clamp(orthographic.zoom() / factor, 0.001f, 100_000.0f));
        }
        applyPose();
    }

    /** Frames world-space bounds while preserving the current view direction. */
    void frame(BoundingBox bounds) {
        float sizeX = bounds.maximum().x() - bounds.minimum().x();
        float sizeY = bounds.maximum().y() - bounds.minimum().y();
        float sizeZ = bounds.maximum().z() - bounds.minimum().z();
        pivot.set(
                (bounds.minimum().x() + bounds.maximum().x()) * 0.5f,
                (bounds.minimum().y() + bounds.maximum().y()) * 0.5f,
                (bounds.minimum().z() + bounds.maximum().z()) * 0.5f);
        float radius = Math.max(0.05f, (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) * 0.5f);
        float limitingAngle =
                (float) Math.atan(Math.tan(FIELD_OF_VIEW * 0.5f) * Math.min(1.0f, perspective.aspectRatio()));
        distance = Math.clamp(
                radius / (float) Math.sin(limitingAngle) * FRAME_PADDING, MINIMUM_DISTANCE, MAXIMUM_DISTANCE);
        float visibleHeight = Math.max(sizeY, Math.max(sizeX / Math.max(perspective.aspectRatio(), 0.001f), sizeZ));
        orthographic.setZoom(Math.clamp(
                (ORTHOGRAPHIC_HALF_HEIGHT * 2.0f) / Math.max(0.1f, visibleHeight * FRAME_PADDING), 0.001f, 100_000.0f));
        applyPose();
    }

    /** Aligns the camera with a signed world axis while retaining distance and pivot. */
    void align(SceneOrientationGizmoState.Axis axis, SceneOrientationGizmoState.Direction direction) {
        float sign = direction.sign();
        position.set(axis.x() * sign, axis.y() * sign, axis.z() * sign)
                .mul(distance)
                .add(pivot);
        synchronizeFrom(position);
        applyPose();
    }

    /** Switches projection while preserving the current pose and approximate apparent scale. */
    void toggleProjection() {
        if (active == perspective) {
            float visibleHeight = 2.0f * distance * (float) Math.tan(FIELD_OF_VIEW * 0.5f);
            orthographic.setZoom((ORTHOGRAPHIC_HALF_HEIGHT * 2.0f) / visibleHeight);
            active = orthographic;
        } else {
            float visibleHeight = (ORTHOGRAPHIC_HALF_HEIGHT * 2.0f) / orthographic.zoom();
            distance = Math.clamp(
                    visibleHeight / (2.0f * (float) Math.tan(FIELD_OF_VIEW * 0.5f)),
                    MINIMUM_DISTANCE,
                    MAXIMUM_DISTANCE);
            active = perspective;
        }
        applyPose();
    }

    /** Returns immutable renderer-authoritative state for transport and focused tests. */
    SceneViewCameraState state() {
        active.quaternion().getEulerAnglesXYZ(orientationDegrees).mul((float) (180.0 / Math.PI));
        return new SceneViewCameraState(
                active.position(),
                orientationDegrees,
                pivot,
                active == perspective
                        ? SceneViewCameraState.Projection.PERSPECTIVE
                        : SceneViewCameraState.Projection.ORTHOGRAPHIC);
    }

    /** Recomputes spherical state from a position relative to the current pivot. */
    private void synchronizeFrom(Vector3fc cameraPosition) {
        position.set(cameraPosition).sub(pivot);
        distance = position.length();
        azimuth = (float) Math.atan2(position.x(), position.z());
        polar = (float) Math.acos(Math.clamp(position.y() / distance, -1.0f, 1.0f));
    }

    /** Applies the authoritative spherical pose to both projections. */
    private void applyPose() {
        float sinPolar = (float) Math.sin(polar);
        position.set(
                        distance * sinPolar * (float) Math.sin(azimuth),
                        distance * (float) Math.cos(polar),
                        distance * sinPolar * (float) Math.cos(azimuth))
                .add(pivot);
        perspective.setPosition(position);
        perspective.lookAt(pivot);
        orthographic.setPosition(position);
        orthographic.lookAt(pivot);
    }

    /** Initializes both projection cameras with the same pose. */
    private void synchronizeProjectionPose() {
        orthographic.setPosition(perspective.position());
        orthographic.lookAt(pivot);
    }

    /** Rejects non-finite input at the renderer trust boundary. */
    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
