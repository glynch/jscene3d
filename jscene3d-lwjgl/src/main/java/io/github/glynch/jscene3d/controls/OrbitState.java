/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Package-private spherical camera state separated from native input adaptation. */
final class OrbitState {
    private static final float POLAR_EPSILON = 1.0e-4f;
    private static final float SCROLL_SCALE = 0.1f;
    private static final float POINTER_DOLLY_SCALE = 0.01f;
    private static final float MOTION_EPSILON = 1.0e-7f;

    private final Vector3f target = new Vector3f();
    private final Vector3f pendingPan = new Vector3f();
    private final Vector3f scratchPosition = new Vector3f();
    private final Vector3f viewDirection = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();

    private float distance;
    private float azimuth;
    private float polarAngle;
    private float pendingAzimuth;
    private float pendingPolar;
    private float zoomMultiplier = 1.0f;

    /** Returns the stable live read-only target view. */
    Vector3fc target() {
        return target;
    }

    /** Replaces the world-space orbit target. */
    void setTarget(float x, float y, float z) {
        target.set(x, y, z);
    }

    /** Derives the spherical coordinates from the camera's current position. */
    void synchronize(Vector3fc cameraPosition) {
        float offsetX = cameraPosition.x() - target.x;
        float offsetY = cameraPosition.y() - target.y;
        float offsetZ = cameraPosition.z() - target.z;
        float largestComponent = Math.max(Math.max(Math.abs(offsetX), Math.abs(offsetY)), Math.abs(offsetZ));
        if (!Float.isFinite(largestComponent)) {
            throw new IllegalStateException("Orbit camera and target must produce a finite direction");
        }
        if (largestComponent == 0.0f) {
            throw new IllegalStateException("Orbit camera position must differ from its target");
        }

        float scaledX = offsetX / largestComponent;
        float scaledY = offsetY / largestComponent;
        float scaledZ = offsetZ / largestComponent;
        distance = largestComponent * (float) Math.sqrt(scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ);
        if (!Float.isFinite(distance)) {
            throw new IllegalStateException("Orbit camera distance must be finite");
        }
        azimuth = (float) Math.atan2(offsetX, offsetZ);
        polarAngle = (float) Math.acos(Math.clamp(offsetY / distance, -1.0f, 1.0f));
    }

    /** Queues an orbit expressed as pointer movement. */
    void orbit(double pointerDeltaX, double pointerDeltaY, int windowHeight, float rotationSpeed) {
        float radiansPerWindowHeight = TWO_PI / windowHeight * rotationSpeed;
        rotateLeft((float) pointerDeltaX * radiansPerWindowHeight);
        pendingPolar += (float) pointerDeltaY * radiansPerWindowHeight;
    }

    /** Queues a leftward azimuth rotation in radians. */
    void rotateLeft(float radians) {
        pendingAzimuth -= radians;
    }

    /** Queues an upward polar rotation in radians. */
    void rotateUp(float radians) {
        pendingPolar -= radians;
    }

    /** Queues a pan expressed in logical window pixels. */
    void pan(
            double pointerDeltaX,
            double pointerDeltaY,
            int windowWidth,
            int windowHeight,
            Camera camera,
            float panSpeed,
            boolean screenSpacePanning) {
        if ((pointerDeltaX == 0.0 && pointerDeltaY == 0.0) || panSpeed == 0.0f) {
            return;
        }

        calculatePosition(scratchPosition);
        viewDirection.set(target).sub(scratchPosition).normalize();
        viewDirection.cross(0.0f, 1.0f, 0.0f, right);
        if (right.lengthSquared() == 0.0f) {
            viewDirection.cross(0.0f, 0.0f, 1.0f, right);
        }
        right.normalize();
        if (screenSpacePanning) {
            right.cross(viewDirection, cameraUp).normalize();
        } else {
            cameraUp.set(0.0f, 1.0f, 0.0f).cross(right).normalize();
        }

        float verticalUnitsPerPixel =
                switch (camera) {
                    case PerspectiveCamera perspectiveCamera ->
                        2.0f * distance * (float) Math.tan(perspectiveCamera.fieldOfView() * 0.5f) / windowHeight;
                    case OrthographicCamera orthographicCamera ->
                        (orthographicCamera.top() - orthographicCamera.bottom())
                                / orthographicCamera.zoom()
                                / windowHeight;
                };
        float horizontalUnitsPerPixel =
                switch (camera) {
                    case PerspectiveCamera ignored -> verticalUnitsPerPixel;
                    case OrthographicCamera orthographicCamera ->
                        (orthographicCamera.right() - orthographicCamera.left())
                                / orthographicCamera.zoom()
                                / windowWidth;
                };

        pendingPan.fma((float) -pointerDeltaX * horizontalUnitsPerPixel * panSpeed, right);
        pendingPan.fma((float) pointerDeltaY * verticalUnitsPerPixel * panSpeed, cameraUp);
    }

    /** Queues scroll-wheel dolly input. */
    void dolly(double scrollDeltaY, float zoomSpeed) {
        zoomMultiplier *= (float) Math.exp(-scrollDeltaY * zoomSpeed * SCROLL_SCALE);
    }

    /** Queues middle-button vertical drag as dolly input. */
    void dollyFromPointer(double pointerDeltaY, float zoomSpeed) {
        zoomMultiplier *= (float) Math.exp(pointerDeltaY * zoomSpeed * POINTER_DOLLY_SCALE);
    }

    /** Queues motion toward the target by a factor greater than one. */
    void dollyIn(float factor) {
        zoomMultiplier /= factor;
    }

    /** Queues motion away from the target by a factor greater than one. */
    void dollyOut(float factor) {
        zoomMultiplier *= factor;
    }

    /** Returns whether a queued operation remains to be applied. */
    boolean hasPendingMotion() {
        return pendingAzimuth != 0.0f
                || pendingPolar != 0.0f
                || pendingPan.lengthSquared() != 0.0f
                || zoomMultiplier != 1.0f;
    }

    /** Returns whether the synchronized camera state lies outside configured limits. */
    boolean violatesLimits(Camera camera, OrbitLimits limits) {
        float safeMinimumPolarAngle = Math.clamp(limits.minimumPolarAngle(), POLAR_EPSILON, PI - POLAR_EPSILON);
        float safeMaximumPolarAngle = Math.clamp(limits.maximumPolarAngle(), POLAR_EPSILON, PI - POLAR_EPSILON);
        if (clampAzimuth(azimuth, limits) != azimuth
                || polarAngle < safeMinimumPolarAngle
                || polarAngle > safeMaximumPolarAngle) {
            return true;
        }
        return switch (camera) {
            case PerspectiveCamera ignored ->
                distance < limits.minimumDistance() || distance > limits.maximumDistance();
            case OrthographicCamera orthographicCamera ->
                orthographicCamera.zoom() < limits.minimumZoom() || orthographicCamera.zoom() > limits.maximumZoom();
        };
    }

    /** Clears all queued motion without changing the current target. */
    void clearPendingMotion() {
        pendingAzimuth = 0.0f;
        pendingPolar = 0.0f;
        pendingPan.zero();
        zoomMultiplier = 1.0f;
    }

    /** Applies queued motion and configured limits to the supplied camera. */
    void apply(Camera camera, OrbitLimits limits, float dampingFraction) {
        azimuth = clampAzimuth(azimuth + pendingAzimuth * dampingFraction, limits);
        float safeMinimumPolarAngle = Math.clamp(limits.minimumPolarAngle(), POLAR_EPSILON, PI - POLAR_EPSILON);
        float safeMaximumPolarAngle = Math.clamp(limits.maximumPolarAngle(), POLAR_EPSILON, PI - POLAR_EPSILON);
        polarAngle =
                Math.clamp(polarAngle + pendingPolar * dampingFraction, safeMinimumPolarAngle, safeMaximumPolarAngle);
        target.fma(dampingFraction, pendingPan);

        switch (camera) {
            case PerspectiveCamera ignored ->
                distance = (float) Math.clamp(
                        (double) distance * zoomMultiplier, limits.minimumDistance(), limits.maximumDistance());
            case OrthographicCamera orthographicCamera -> {
                float zoom = (float) Math.clamp(
                        (double) orthographicCamera.zoom() / zoomMultiplier,
                        limits.minimumZoom(),
                        limits.maximumZoom());
                orthographicCamera.setZoom(zoom);
            }
        }
        zoomMultiplier = 1.0f;

        calculatePosition(scratchPosition);
        camera.setPosition(scratchPosition);
        camera.lookAt(target);
        retainUnappliedMotion(1.0f - dampingFraction);
    }

    /** Returns the synchronized camera distance. */
    float distance() {
        return distance;
    }

    /** Returns the synchronized azimuth angle in radians. */
    float azimuthAngle() {
        return azimuth;
    }

    /** Returns the synchronized polar angle in radians. */
    float polarAngle() {
        return polarAngle;
    }

    /** Retains the damped remainder and removes insignificant residual motion. */
    private void retainUnappliedMotion(float retainedFraction) {
        pendingAzimuth *= retainedFraction;
        pendingPolar *= retainedFraction;
        pendingPan.mul(retainedFraction);
        if (Math.abs(pendingAzimuth) < MOTION_EPSILON) {
            pendingAzimuth = 0.0f;
        }
        if (Math.abs(pendingPolar) < MOTION_EPSILON) {
            pendingPolar = 0.0f;
        }
        if (pendingPan.lengthSquared() < MOTION_EPSILON * MOTION_EPSILON) {
            pendingPan.zero();
        }
    }

    /** Clamps an azimuth while supporting configured intervals that cross the pi seam. */
    private static float clampAzimuth(float angle, OrbitLimits limits) {
        float minimum = limits.minimumAzimuthAngle();
        float maximum = limits.maximumAzimuthAngle();
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum)) {
            return angle;
        }
        if (minimum < -PI) {
            minimum += TWO_PI;
        } else if (minimum > PI) {
            minimum -= TWO_PI;
        }
        if (maximum < -PI) {
            maximum += TWO_PI;
        } else if (maximum > PI) {
            maximum -= TWO_PI;
        }
        if (minimum <= maximum) {
            return Math.clamp(angle, minimum, maximum);
        }
        return angle > (minimum + maximum) * 0.5f ? Math.max(angle, minimum) : Math.min(angle, maximum);
    }

    /** Writes the Cartesian camera position represented by current spherical state. */
    private Vector3f calculatePosition(Vector3f destination) {
        float sinPolar = (float) Math.sin(polarAngle);
        return destination.set(
                target.x + distance * sinPolar * (float) Math.sin(azimuth),
                target.y + distance * (float) Math.cos(polarAngle),
                target.z + distance * sinPolar * (float) Math.cos(azimuth));
    }
}
