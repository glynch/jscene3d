/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.core.Angles.PI;
import static io.github.glynch.jscene3d.core.Angles.TWO_PI;

import io.github.glynch.jscene3d.core.Camera;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Interactive orbit, pan, and dolly control for an unparented camera.
 *
 * <p>The primary mouse button orbits, the secondary mouse button pans, and the middle button or
 * vertical scrolling dollies. Holding Shift, Control, or Command changes primary-button dragging
 * to panning and changes arrow-key panning to rotation. Call {@link #update()} once after each
 * {@link Window#pollEvents()} call.
 *
 * <p>Perspective and orthographic cameras are supported. The camera and window remain
 * caller-owned. This control is mutable, not thread-safe, and has the same thread affinity as its
 * window.
 */
public final class OrbitControls {
    private static final float DEFAULT_SPEED = 1.0f;
    private static final float DEFAULT_KEY_PAN_SPEED = 7.0f;
    private static final float DEFAULT_DAMPING_FACTOR = 0.05f;
    private static final float DEFAULT_AUTO_ROTATION_SPEED = 2.0f;
    private static final float DEFAULT_SECONDS_PER_UPDATE = 1.0f / 60.0f;

    private final Camera camera;
    private final Window window;
    private final OrbitState orbitState = new OrbitState();
    private final OrbitLimits limits = new OrbitLimits();
    private final Vector3f savedTarget = new Vector3f();
    private final Vector3f savedPosition = new Vector3f();

    private boolean enabled = true;
    private boolean rotationEnabled = true;
    private boolean panningEnabled = true;
    private boolean zoomEnabled = true;
    private boolean dampingEnabled;
    private boolean autoRotationEnabled;
    private boolean screenSpacePanning = true;
    private float rotationSpeed = DEFAULT_SPEED;
    private float panSpeed = DEFAULT_SPEED;
    private float zoomSpeed = DEFAULT_SPEED;
    private float keyPanSpeed = DEFAULT_KEY_PAN_SPEED;
    private float keyRotationSpeed = DEFAULT_SPEED;
    private float dampingFactor = DEFAULT_DAMPING_FACTOR;
    private float autoRotationSpeed = DEFAULT_AUTO_ROTATION_SPEED;
    private float savedZoom = 1.0f;

    /**
     * Creates controls centered on the world origin and saves the initial camera state.
     *
     * @param camera caller-owned perspective or orthographic camera to control
     * @param window caller-owned input window
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code camera} is parented
     * @throws IllegalStateException if the camera is at the world origin
     */
    public OrbitControls(Camera camera, Window window) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.window = Objects.requireNonNull(window, "window");
        requireUnparentedCamera(false);
        orbitState.synchronize(camera.position());
        saveState();
        camera.lookAt(orbitState.target());
    }

    /** Returns the stable live read-only world-space orbit target. */
    public Vector3fc target() {
        return orbitState.target();
    }

    /** Sets the world-space orbit target for the next update. */
    public void setTarget(float x, float y, float z) {
        orbitState.setTarget(
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                Preconditions.requireFinite(z, "z"));
    }

    /** Copies the world-space orbit target for the next update. */
    public void setTarget(Vector3fc target) {
        Vector3fc validTarget = Objects.requireNonNull(target, "target");
        setTarget(validTarget.x(), validTarget.y(), validTarget.z());
    }

    /** Returns whether controls process input and pending motion. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Enables or disables input and pending-motion processing. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Returns whether orbit rotation is enabled. */
    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    /** Enables or disables mouse, keyboard, programmatic, and automatic rotation. */
    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }

    /** Returns whether panning is enabled. */
    public boolean isPanningEnabled() {
        return panningEnabled;
    }

    /** Enables or disables mouse, keyboard, and programmatic panning. */
    public void setPanningEnabled(boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
    }

    /** Returns whether perspective dolly or orthographic zoom is enabled. */
    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    /** Enables or disables mouse and programmatic dolly or zoom. */
    public void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }

    /** Returns the minimum perspective-camera target distance. */
    public float minimumDistance() {
        return limits.minimumDistance();
    }

    /** Returns the maximum perspective-camera target distance. */
    public float maximumDistance() {
        return limits.maximumDistance();
    }

    /** Atomically sets the perspective-camera target-distance interval. */
    public void setDistanceLimits(float minimumDistance, float maximumDistance) {
        float validMinimum = Preconditions.requirePositive(minimumDistance, "minimumDistance");
        float validMaximum = Preconditions.requirePositive(maximumDistance, "maximumDistance");
        Preconditions.requireOrdered(validMinimum, "minimumDistance", validMaximum, "maximumDistance");
        limits.setDistance(validMinimum, validMaximum);
    }

    /** Returns the minimum orthographic-camera zoom. */
    public float minimumZoom() {
        return limits.minimumZoom();
    }

    /** Returns the maximum orthographic-camera zoom. */
    public float maximumZoom() {
        return limits.maximumZoom();
    }

    /** Atomically sets the orthographic-camera zoom interval. */
    public void setZoomLimits(float minimumZoom, float maximumZoom) {
        float validMinimum = Preconditions.requirePositive(minimumZoom, "minimumZoom");
        float validMaximum = Preconditions.requirePositive(maximumZoom, "maximumZoom");
        Preconditions.requireOrdered(validMinimum, "minimumZoom", validMaximum, "maximumZoom");
        limits.setZoom(validMinimum, validMaximum);
    }

    /** Returns the minimum polar angle in radians. */
    public float minimumPolarAngle() {
        return limits.minimumPolarAngle();
    }

    /** Returns the maximum polar angle in radians. */
    public float maximumPolarAngle() {
        return limits.maximumPolarAngle();
    }

    /** Atomically sets the polar-angle interval between zero and pi radians. */
    public void setPolarAngleLimits(float minimumPolarAngle, float maximumPolarAngle) {
        float validMinimum = Preconditions.requireInRange(minimumPolarAngle, "minimumPolarAngle", 0.0f, PI);
        float validMaximum = Preconditions.requireInRange(maximumPolarAngle, "maximumPolarAngle", 0.0f, PI);
        Preconditions.requireOrdered(validMinimum, "minimumPolarAngle", validMaximum, "maximumPolarAngle");
        limits.setPolarAngle(validMinimum, validMaximum);
    }

    /** Returns the minimum azimuth angle in radians. */
    public float minimumAzimuthAngle() {
        return limits.minimumAzimuthAngle();
    }

    /** Returns the maximum azimuth angle in radians. */
    public float maximumAzimuthAngle() {
        return limits.maximumAzimuthAngle();
    }

    /**
     * Atomically sets an azimuth-angle interval in radians.
     *
     * <p>Each endpoint must be between negative and positive two pi, and the interval must span
     * less than one complete turn. Intervals crossing the negative/positive pi seam are supported.
     */
    public void setAzimuthAngleLimits(float minimumAzimuthAngle, float maximumAzimuthAngle) {
        float validMinimum = Preconditions.requireInRange(minimumAzimuthAngle, "minimumAzimuthAngle", -TWO_PI, TWO_PI);
        float validMaximum = Preconditions.requireInRange(maximumAzimuthAngle, "maximumAzimuthAngle", -TWO_PI, TWO_PI);
        Preconditions.requireOrdered(validMinimum, "minimumAzimuthAngle", validMaximum, "maximumAzimuthAngle");
        Preconditions.requireSpanLessThan(
                validMinimum, "minimumAzimuthAngle", validMaximum, "maximumAzimuthAngle", TWO_PI);
        limits.setAzimuthAngle(validMinimum, validMaximum);
    }

    /** Returns the mouse rotation multiplier. */
    public float rotationSpeed() {
        return rotationSpeed;
    }

    /** Sets the finite non-negative mouse rotation multiplier. */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = Preconditions.requireNonNegative(rotationSpeed, "rotationSpeed");
    }

    /** Returns the mouse and keyboard panning multiplier. */
    public float panSpeed() {
        return panSpeed;
    }

    /** Sets the finite non-negative panning multiplier. */
    public void setPanSpeed(float panSpeed) {
        this.panSpeed = Preconditions.requireNonNegative(panSpeed, "panSpeed");
    }

    /** Returns the dolly and zoom multiplier. */
    public float zoomSpeed() {
        return zoomSpeed;
    }

    /** Sets the finite non-negative dolly and zoom multiplier. */
    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = Preconditions.requireNonNegative(zoomSpeed, "zoomSpeed");
    }

    /** Returns the arrow-key panning distance in logical pixels. */
    public float keyPanSpeed() {
        return keyPanSpeed;
    }

    /** Sets the finite non-negative arrow-key panning distance in logical pixels. */
    public void setKeyPanSpeed(float keyPanSpeed) {
        this.keyPanSpeed = Preconditions.requireNonNegative(keyPanSpeed, "keyPanSpeed");
    }

    /** Returns the modified-arrow-key rotation multiplier. */
    public float keyRotationSpeed() {
        return keyRotationSpeed;
    }

    /** Sets the finite non-negative modified-arrow-key rotation multiplier. */
    public void setKeyRotationSpeed(float keyRotationSpeed) {
        this.keyRotationSpeed = Preconditions.requireNonNegative(keyRotationSpeed, "keyRotationSpeed");
    }

    /** Returns whether rotation and panning use damping. */
    public boolean isDampingEnabled() {
        return dampingEnabled;
    }

    /** Enables or disables rotation and panning damping. */
    public void setDampingEnabled(boolean dampingEnabled) {
        this.dampingEnabled = dampingEnabled;
    }

    /** Returns the per-60-Hz-update damping fraction. */
    public float dampingFactor() {
        return dampingFactor;
    }

    /** Sets the damping fraction greater than zero and no greater than one. */
    public void setDampingFactor(float dampingFactor) {
        float validFactor = Preconditions.requirePositive(dampingFactor, "dampingFactor");
        this.dampingFactor = Preconditions.requireInRange(validFactor, "dampingFactor", 0.0f, 1.0f);
    }

    /** Returns whether the camera automatically orbits while idle. */
    public boolean isAutoRotationEnabled() {
        return autoRotationEnabled;
    }

    /** Enables or disables automatic idle rotation. */
    public void setAutoRotationEnabled(boolean autoRotationEnabled) {
        this.autoRotationEnabled = autoRotationEnabled;
    }

    /** Returns the automatic-rotation multiplier; {@code 2} completes one orbit in 30 seconds. */
    public float autoRotationSpeed() {
        return autoRotationSpeed;
    }

    /** Sets the finite automatic-rotation multiplier; negative values reverse direction. */
    public void setAutoRotationSpeed(float autoRotationSpeed) {
        this.autoRotationSpeed = Preconditions.requireFinite(autoRotationSpeed, "autoRotationSpeed");
    }

    /** Returns whether panning follows the camera's screen plane. */
    public boolean isScreenSpacePanning() {
        return screenSpacePanning;
    }

    /** Selects screen-plane panning or panning perpendicular to world up. */
    public void setScreenSpacePanning(boolean screenSpacePanning) {
        this.screenSpacePanning = screenSpacePanning;
    }

    /** Returns the current azimuth angle in radians. */
    public float azimuthAngle() {
        synchronize();
        return orbitState.azimuthAngle();
    }

    /** Returns the current polar angle in radians. */
    public float polarAngle() {
        synchronize();
        return orbitState.polarAngle();
    }

    /** Returns the current distance between the camera and target. */
    public float distance() {
        synchronize();
        return orbitState.distance();
    }

    /** Immediately rotates left by the supplied non-negative radians. */
    public void rotateLeft(float radians) {
        if (!rotationEnabled) {
            return;
        }
        orbitState.rotateLeft(Preconditions.requireNonNegative(radians, "radians"));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /** Immediately rotates upward by the supplied non-negative radians. */
    public void rotateUp(float radians) {
        if (!rotationEnabled) {
            return;
        }
        orbitState.rotateUp(Preconditions.requireNonNegative(radians, "radians"));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /** Immediately pans by a logical-pixel offset using the configured pan mode. */
    public void pan(float horizontalPixels, float verticalPixels) {
        float validHorizontal = Preconditions.requireFinite(horizontalPixels, "horizontalPixels");
        float validVertical = Preconditions.requireFinite(verticalPixels, "verticalPixels");
        if (!panningEnabled) {
            return;
        }
        synchronize();
        orbitState.pan(
                validHorizontal,
                validVertical,
                Math.max(window.width(), 1),
                Math.max(window.height(), 1),
                camera,
                panSpeed,
                screenSpacePanning);
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /** Immediately moves toward the target or increases orthographic zoom by a factor. */
    public void dollyIn(float factor) {
        if (!zoomEnabled) {
            return;
        }
        orbitState.dollyIn(Preconditions.requireGreaterThan(factor, "factor", 1.0f));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /** Immediately moves away from the target or decreases orthographic zoom by a factor. */
    public void dollyOut(float factor) {
        if (!zoomEnabled) {
            return;
        }
        orbitState.dollyOut(Preconditions.requireGreaterThan(factor, "factor", 1.0f));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /** Saves the current target, camera position, and orthographic zoom for {@link #reset()}. */
    public void saveState() {
        requireUnparentedCamera(true);
        synchronize();
        savedTarget.set(orbitState.target());
        savedPosition.set(camera.position());
        if (camera instanceof OrthographicCamera orthographicCamera) {
            savedZoom = orthographicCamera.zoom();
        }
    }

    /** Restores the most recently saved target, camera position, and orthographic zoom. */
    public void reset() {
        requireUnparentedCamera(true);
        orbitState.clearPendingMotion();
        orbitState.setTarget(savedTarget.x, savedTarget.y, savedTarget.z);
        camera.setPosition(savedPosition);
        if (camera instanceof OrthographicCamera orthographicCamera) {
            orthographicCamera.setZoom(savedZoom);
        }
        synchronize();
        camera.lookAt(orbitState.target());
    }

    /** Applies input and pending motion using a nominal 60-Hz elapsed time. */
    public boolean update() {
        return update(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Applies input and pending motion using an elapsed duration in seconds.
     *
     * @param elapsedSeconds finite non-negative time since the previous update
     * @return {@code true} if the camera position, orientation, or zoom changed
     */
    public boolean update(float elapsedSeconds) {
        float validElapsedSeconds = Preconditions.requireNonNegative(elapsedSeconds, "elapsedSeconds");
        if (!enabled) {
            return false;
        }
        requireUnparentedCamera(true);
        synchronize();

        InputState input = window.input();
        boolean modifierDown = isModifierDown(input);
        boolean userInteracting = processPointerInput(input, modifierDown);
        userInteracting |= processKeyboardInput(input, modifierDown);
        if (zoomEnabled && input.scrollDeltaY() != 0.0) {
            orbitState.dolly(input.scrollDeltaY(), zoomSpeed);
            userInteracting = true;
        }
        if (autoRotationEnabled && rotationEnabled && !userInteracting) {
            orbitState.rotateLeft(TWO_PI / 60.0f * autoRotationSpeed * validElapsedSeconds);
        }
        return applyQueuedMotion(validElapsedSeconds);
    }

    private boolean processPointerInput(InputState input, boolean modifierDown) {
        double deltaX = input.pointerDeltaX();
        double deltaY = input.pointerDeltaY();
        int width = Math.max(window.width(), 1);
        int height = Math.max(window.height(), 1);
        if (isPointerDollyInput(input)) {
            applyPointerDolly(deltaY);
            return true;
        }
        if (isPointerPanInput(input, modifierDown)) {
            applyPointerPan(deltaX, deltaY, width, height);
            return true;
        }
        if (isPointerRotationInput(input)) {
            applyPointerRotation(deltaX, deltaY, height);
            return true;
        }
        return false;
    }

    /** Returns whether the current mouse-button state selects pointer dolly. */
    private boolean isPointerDollyInput(InputState input) {
        return zoomEnabled && input.isMouseButtonDown(MouseButton.MIDDLE);
    }

    /** Returns whether the current mouse-button and modifier state selects pointer panning. */
    private boolean isPointerPanInput(InputState input, boolean modifierDown) {
        return panningEnabled
                && (input.isMouseButtonDown(MouseButton.RIGHT)
                        || (modifierDown && input.isMouseButtonDown(MouseButton.LEFT)));
    }

    /** Returns whether the current mouse-button state selects pointer rotation. */
    private boolean isPointerRotationInput(InputState input) {
        return rotationEnabled && input.isMouseButtonDown(MouseButton.LEFT);
    }

    /** Applies vertical pointer movement as dolly input when movement occurred. */
    private void applyPointerDolly(double deltaY) {
        if (deltaY != 0.0) {
            orbitState.dollyFromPointer(deltaY, zoomSpeed);
        }
    }

    /** Applies pointer movement as pan input when movement occurred. */
    private void applyPointerPan(double deltaX, double deltaY, int width, int height) {
        if (deltaX != 0.0 || deltaY != 0.0) {
            orbitState.pan(deltaX, deltaY, width, height, camera, panSpeed, screenSpacePanning);
        }
    }

    /** Applies pointer movement as rotation input when movement occurred. */
    private void applyPointerRotation(double deltaX, double deltaY, int height) {
        if (deltaX != 0.0 || deltaY != 0.0) {
            orbitState.orbit(deltaX, deltaY, height, rotationSpeed);
        }
    }

    private boolean processKeyboardInput(InputState input, boolean modifierDown) {
        float horizontal = axis(input, Key.LEFT, Key.RIGHT);
        float vertical = axis(input, Key.UP, Key.DOWN);
        if (horizontal == 0.0f && vertical == 0.0f) {
            return false;
        }
        int width = Math.max(window.width(), 1);
        int height = Math.max(window.height(), 1);
        if (modifierDown && rotationEnabled) {
            float radiansPerHeight = TWO_PI / height * keyRotationSpeed;
            orbitState.rotateLeft(horizontal * radiansPerHeight);
            orbitState.rotateUp(vertical * radiansPerHeight);
            return true;
        }
        if (panningEnabled) {
            orbitState.pan(
                    horizontal * keyPanSpeed,
                    vertical * keyPanSpeed,
                    width,
                    height,
                    camera,
                    panSpeed,
                    screenSpacePanning);
            return true;
        }
        return false;
    }

    private boolean applyQueuedMotion(float elapsedSeconds) {
        requireUnparentedCamera(true);
        float oldPositionX = camera.position().x();
        float oldPositionY = camera.position().y();
        float oldPositionZ = camera.position().z();
        Quaternionfc oldQuaternion = camera.quaternion();
        float oldQuaternionX = oldQuaternion.x();
        float oldQuaternionY = oldQuaternion.y();
        float oldQuaternionZ = oldQuaternion.z();
        float oldQuaternionW = oldQuaternion.w();
        float oldZoom = camera instanceof OrthographicCamera orthographicCamera ? orthographicCamera.zoom() : 1.0f;

        synchronize();
        if (!orbitState.hasPendingMotion() && !orbitState.violatesLimits(camera, limits)) {
            camera.lookAt(orbitState.target());
            return !camera.quaternion().equals(oldQuaternionX, oldQuaternionY, oldQuaternionZ, oldQuaternionW);
        }
        float dampingFraction =
                dampingEnabled ? 1.0f - (float) Math.pow(1.0f - dampingFactor, elapsedSeconds * 60.0f) : 1.0f;
        orbitState.apply(camera, limits, dampingFraction);

        return !camera.position().equals(oldPositionX, oldPositionY, oldPositionZ)
                || !camera.quaternion().equals(oldQuaternionX, oldQuaternionY, oldQuaternionZ, oldQuaternionW)
                || (camera instanceof OrthographicCamera orthographicCamera && orthographicCamera.zoom() != oldZoom);
    }

    private void synchronize() {
        orbitState.synchronize(camera.position());
    }

    private void requireUnparentedCamera(boolean stateFailure) {
        if (camera.parent() != null) {
            String message = "OrbitControls requires an unparented camera";
            if (stateFailure) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isModifierDown(InputState input) {
        return input.isKeyDown(Key.LEFT_SHIFT)
                || input.isKeyDown(Key.RIGHT_SHIFT)
                || input.isKeyDown(Key.LEFT_CONTROL)
                || input.isKeyDown(Key.RIGHT_CONTROL)
                || input.isKeyDown(Key.LEFT_SUPER)
                || input.isKeyDown(Key.RIGHT_SUPER);
    }

    private static float axis(InputState input, Key positive, Key negative) {
        return (input.isKeyDown(positive) ? 1.0f : 0.0f) - (input.isKeyDown(negative) ? 1.0f : 0.0f);
    }
}
