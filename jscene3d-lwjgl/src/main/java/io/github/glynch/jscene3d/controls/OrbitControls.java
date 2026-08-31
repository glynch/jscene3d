/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
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
    private int viewportWidth;
    private int viewportHeight;

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

    /**
     * Returns the stable live read-only world-space orbit target.
     *
     * @return target view retained for the lifetime of these controls
     */
    public Vector3fc target() {
        return orbitState.target();
    }

    /**
     * Sets the world-space orbit target for the next update.
     *
     * @param x target X coordinate
     * @param y target Y coordinate
     * @param z target Z coordinate
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    public void setTarget(float x, float y, float z) {
        orbitState.setTarget(
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                Preconditions.requireFinite(z, "z"));
    }

    /**
     * Copies the world-space orbit target for the next update.
     *
     * @param target target to copy
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if any target coordinate is not finite
     */
    public void setTarget(Vector3fc target) {
        Vector3fc validTarget = Objects.requireNonNull(target, "target");
        setTarget(validTarget.x(), validTarget.y(), validTarget.z());
    }

    /**
     * Returns whether controls process input and pending motion.
     *
     * @return {@code true} by default
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables input and pending-motion processing.
     *
     * @param enabled whether calls to {@link #update()} may move the camera
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether orbit rotation is enabled.
     *
     * @return {@code true} by default
     */
    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    /**
     * Enables or disables mouse, keyboard, programmatic, and automatic rotation.
     *
     * @param rotationEnabled whether rotation operations are enabled
     */
    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }

    /**
     * Returns whether panning is enabled.
     *
     * @return {@code true} by default
     */
    public boolean isPanningEnabled() {
        return panningEnabled;
    }

    /**
     * Enables or disables mouse, keyboard, and programmatic panning.
     *
     * @param panningEnabled whether panning operations are enabled
     */
    public void setPanningEnabled(boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
    }

    /**
     * Returns whether perspective dolly or orthographic zoom is enabled.
     *
     * @return {@code true} by default
     */
    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    /**
     * Enables or disables mouse and programmatic dolly or zoom.
     *
     * @param zoomEnabled whether dolly and zoom operations are enabled
     */
    public void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }

    /**
     * Returns the minimum perspective-camera target distance.
     *
     * @return the positive lower distance limit, {@code 0.01} by default
     */
    public float minimumDistance() {
        return limits.minimumDistance();
    }

    /**
     * Returns the maximum perspective-camera target distance.
     *
     * @return the positive upper distance limit, effectively unbounded by default
     */
    public float maximumDistance() {
        return limits.maximumDistance();
    }

    /**
     * Atomically sets the perspective-camera target-distance interval.
     *
     * <p>The interval is enforced by the next movement or update. It does not affect orthographic
     * zoom.
     *
     * @param minimumDistance finite positive lower limit
     * @param maximumDistance finite positive upper limit not less than the lower limit
     * @throws IllegalArgumentException if either value or their ordering is invalid
     */
    public void setDistanceLimits(float minimumDistance, float maximumDistance) {
        float validMinimum = Preconditions.requirePositive(minimumDistance, "minimumDistance");
        float validMaximum = Preconditions.requirePositive(maximumDistance, "maximumDistance");
        Preconditions.requireOrdered(validMinimum, "minimumDistance", validMaximum, "maximumDistance");
        limits.setDistance(validMinimum, validMaximum);
    }

    /**
     * Returns the minimum orthographic-camera zoom.
     *
     * @return the positive lower zoom limit, {@code 0.01} by default
     */
    public float minimumZoom() {
        return limits.minimumZoom();
    }

    /**
     * Returns the maximum orthographic-camera zoom.
     *
     * @return the positive upper zoom limit, effectively unbounded by default
     */
    public float maximumZoom() {
        return limits.maximumZoom();
    }

    /**
     * Atomically sets the orthographic-camera zoom interval.
     *
     * <p>The interval is enforced by the next movement or update. It does not affect perspective
     * distance.
     *
     * @param minimumZoom finite positive lower limit
     * @param maximumZoom finite positive upper limit not less than the lower limit
     * @throws IllegalArgumentException if either value or their ordering is invalid
     */
    public void setZoomLimits(float minimumZoom, float maximumZoom) {
        float validMinimum = Preconditions.requirePositive(minimumZoom, "minimumZoom");
        float validMaximum = Preconditions.requirePositive(maximumZoom, "maximumZoom");
        Preconditions.requireOrdered(validMinimum, "minimumZoom", validMaximum, "maximumZoom");
        limits.setZoom(validMinimum, validMaximum);
    }

    /**
     * Returns the minimum polar angle measured down from world positive Y.
     *
     * @return the lower limit in radians, zero by default
     */
    public float minimumPolarAngle() {
        return limits.minimumPolarAngle();
    }

    /**
     * Returns the maximum polar angle measured down from world positive Y.
     *
     * @return the upper limit in radians, pi by default
     */
    public float maximumPolarAngle() {
        return limits.maximumPolarAngle();
    }

    /**
     * Atomically sets the polar-angle interval between zero and pi radians.
     *
     * <p>A small internal epsilon prevents the camera from occupying a singular pole exactly.
     *
     * @param minimumPolarAngle finite lower limit in the inclusive range {@code [0, pi]}
     * @param maximumPolarAngle finite upper limit in the inclusive range {@code [0, pi]}
     * @throws IllegalArgumentException if either value or their ordering is invalid
     */
    public void setPolarAngleLimits(float minimumPolarAngle, float maximumPolarAngle) {
        float validMinimum = Preconditions.requireInRange(minimumPolarAngle, "minimumPolarAngle", 0.0f, PI);
        float validMaximum = Preconditions.requireInRange(maximumPolarAngle, "maximumPolarAngle", 0.0f, PI);
        Preconditions.requireOrdered(validMinimum, "minimumPolarAngle", validMaximum, "maximumPolarAngle");
        limits.setPolarAngle(validMinimum, validMaximum);
    }

    /**
     * Returns the minimum azimuth angle around world Y.
     *
     * @return the lower limit in radians, negative infinity when unbounded
     */
    public float minimumAzimuthAngle() {
        return limits.minimumAzimuthAngle();
    }

    /**
     * Returns the maximum azimuth angle around world Y.
     *
     * @return the upper limit in radians, positive infinity when unbounded
     */
    public float maximumAzimuthAngle() {
        return limits.maximumAzimuthAngle();
    }

    /**
     * Atomically sets an azimuth-angle interval in radians.
     *
     * <p>Each endpoint must be between negative and positive two pi, and the interval must span
     * less than one complete turn. Intervals crossing the negative/positive pi seam are supported.
     *
     * @param minimumAzimuthAngle finite lower endpoint in radians
     * @param maximumAzimuthAngle finite upper endpoint in radians
     * @throws IllegalArgumentException if either endpoint, their ordering, or their span is invalid
     */
    public void setAzimuthAngleLimits(float minimumAzimuthAngle, float maximumAzimuthAngle) {
        float validMinimum = Preconditions.requireInRange(minimumAzimuthAngle, "minimumAzimuthAngle", -TWO_PI, TWO_PI);
        float validMaximum = Preconditions.requireInRange(maximumAzimuthAngle, "maximumAzimuthAngle", -TWO_PI, TWO_PI);
        Preconditions.requireOrdered(validMinimum, "minimumAzimuthAngle", validMaximum, "maximumAzimuthAngle");
        Preconditions.requireSpanLessThan(
                validMinimum, "minimumAzimuthAngle", validMaximum, "maximumAzimuthAngle", TWO_PI);
        limits.setAzimuthAngle(validMinimum, validMaximum);
    }

    /**
     * Returns the mouse rotation multiplier.
     *
     * @return the non-negative multiplier, {@code 1} by default
     */
    public float rotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Sets the mouse rotation multiplier.
     *
     * @param rotationSpeed finite non-negative multiplier
     * @throws IllegalArgumentException if {@code rotationSpeed} is invalid
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = Preconditions.requireNonNegative(rotationSpeed, "rotationSpeed");
    }

    /**
     * Returns the mouse and keyboard panning multiplier.
     *
     * @return the non-negative multiplier, {@code 1} by default
     */
    public float panSpeed() {
        return panSpeed;
    }

    /**
     * Sets the mouse and keyboard panning multiplier.
     *
     * @param panSpeed finite non-negative multiplier
     * @throws IllegalArgumentException if {@code panSpeed} is invalid
     */
    public void setPanSpeed(float panSpeed) {
        this.panSpeed = Preconditions.requireNonNegative(panSpeed, "panSpeed");
    }

    /**
     * Returns the mouse dolly and zoom multiplier.
     *
     * @return the non-negative multiplier, {@code 1} by default
     */
    public float zoomSpeed() {
        return zoomSpeed;
    }

    /**
     * Sets the mouse dolly and zoom multiplier.
     *
     * @param zoomSpeed finite non-negative multiplier
     * @throws IllegalArgumentException if {@code zoomSpeed} is invalid
     */
    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = Preconditions.requireNonNegative(zoomSpeed, "zoomSpeed");
    }

    /**
     * Returns the arrow-key panning distance.
     *
     * @return logical pixels per update, {@code 7} by default
     */
    public float keyPanSpeed() {
        return keyPanSpeed;
    }

    /**
     * Sets the arrow-key panning distance.
     *
     * @param keyPanSpeed finite non-negative logical pixels per update
     * @throws IllegalArgumentException if {@code keyPanSpeed} is invalid
     */
    public void setKeyPanSpeed(float keyPanSpeed) {
        this.keyPanSpeed = Preconditions.requireNonNegative(keyPanSpeed, "keyPanSpeed");
    }

    /**
     * Returns the modified-arrow-key rotation multiplier.
     *
     * @return the non-negative multiplier, {@code 1} by default
     */
    public float keyRotationSpeed() {
        return keyRotationSpeed;
    }

    /**
     * Sets the modified-arrow-key rotation multiplier.
     *
     * @param keyRotationSpeed finite non-negative multiplier
     * @throws IllegalArgumentException if {@code keyRotationSpeed} is invalid
     */
    public void setKeyRotationSpeed(float keyRotationSpeed) {
        this.keyRotationSpeed = Preconditions.requireNonNegative(keyRotationSpeed, "keyRotationSpeed");
    }

    /**
     * Returns whether rotation and panning use damping.
     *
     * @return {@code false} by default
     */
    public boolean isDampingEnabled() {
        return dampingEnabled;
    }

    /**
     * Enables or disables rotation and panning damping.
     *
     * @param dampingEnabled whether motion should be applied gradually
     */
    public void setDampingEnabled(boolean dampingEnabled) {
        this.dampingEnabled = dampingEnabled;
    }

    /**
     * Returns the damping fraction normalized to a 60-Hz update.
     *
     * @return the fraction in {@code (0, 1]}, {@code 0.05} by default
     */
    public float dampingFactor() {
        return dampingFactor;
    }

    /**
     * Sets the damping fraction normalized to a 60-Hz update.
     *
     * @param dampingFactor finite factor in {@code (0, 1]}
     * @throws IllegalArgumentException if {@code dampingFactor} is invalid
     */
    public void setDampingFactor(float dampingFactor) {
        float validFactor = Preconditions.requirePositive(dampingFactor, "dampingFactor");
        this.dampingFactor = Preconditions.requireInRange(validFactor, "dampingFactor", 0.0f, 1.0f);
    }

    /**
     * Returns whether the camera automatically orbits while idle.
     *
     * @return {@code false} by default
     */
    public boolean isAutoRotationEnabled() {
        return autoRotationEnabled;
    }

    /**
     * Enables or disables automatic idle rotation.
     *
     * @param autoRotationEnabled whether idle updates should orbit automatically
     */
    public void setAutoRotationEnabled(boolean autoRotationEnabled) {
        this.autoRotationEnabled = autoRotationEnabled;
    }

    /**
     * Returns the automatic-rotation multiplier.
     *
     * @return multiplier for which {@code 2} completes one orbit in 30 seconds
     */
    public float autoRotationSpeed() {
        return autoRotationSpeed;
    }

    /**
     * Sets the automatic-rotation multiplier; negative values reverse direction.
     *
     * @param autoRotationSpeed finite multiplier
     * @throws IllegalArgumentException if {@code autoRotationSpeed} is not finite
     */
    public void setAutoRotationSpeed(float autoRotationSpeed) {
        this.autoRotationSpeed = Preconditions.requireFinite(autoRotationSpeed, "autoRotationSpeed");
    }

    /**
     * Returns whether panning follows the camera's screen plane.
     *
     * @return {@code true} by default
     */
    public boolean isScreenSpacePanning() {
        return screenSpacePanning;
    }

    /**
     * Selects screen-plane panning or panning perpendicular to world up.
     *
     * @param screenSpacePanning {@code true} for the camera screen plane; {@code false} for the
     *     world-up plane
     */
    public void setScreenSpacePanning(boolean screenSpacePanning) {
        this.screenSpacePanning = screenSpacePanning;
    }

    /**
     * Sets the logical viewport size used to scale pointer and keyboard movement.
     *
     * <p>Controls use the complete logical window by default. Hosts that reserve part of a window
     * for other content should set the remaining example viewport whenever it changes.
     *
     * @param width positive logical viewport width
     * @param height positive logical viewport height
     * @throws IllegalArgumentException if either dimension is not positive
     */
    public void setViewportSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive: " + width + " x " + height);
        }
        viewportWidth = width;
        viewportHeight = height;
    }

    /** Restores movement scaling against the complete current logical window. */
    public void resetViewportSize() {
        viewportWidth = 0;
        viewportHeight = 0;
    }

    /**
     * Returns the current azimuth angle around world Y.
     *
     * @return synchronized angle in radians
     * @throws IllegalStateException if the camera coincides with the target
     */
    public float azimuthAngle() {
        synchronize();
        return orbitState.azimuthAngle();
    }

    /**
     * Returns the current polar angle measured down from world positive Y.
     *
     * @return synchronized angle in radians
     * @throws IllegalStateException if the camera coincides with the target
     */
    public float polarAngle() {
        synchronize();
        return orbitState.polarAngle();
    }

    /**
     * Returns the current distance between the camera and target.
     *
     * @return synchronized positive distance
     * @throws IllegalStateException if the camera coincides with the target
     */
    public float distance() {
        synchronize();
        return orbitState.distance();
    }

    /**
     * Applies or begins a leftward rotation by the supplied angle.
     *
     * <p>When damping is enabled, subsequent updates apply the retained remainder. The operation
     * does nothing when rotation is disabled.
     *
     * @param radians finite non-negative angle
     * @throws IllegalArgumentException if {@code radians} is invalid
     * @throws IllegalStateException if the camera state cannot be controlled
     */
    public void rotateLeft(float radians) {
        if (!rotationEnabled) {
            return;
        }
        orbitState.rotateLeft(Preconditions.requireNonNegative(radians, "radians"));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Applies or begins an upward rotation by the supplied angle.
     *
     * <p>When damping is enabled, subsequent updates apply the retained remainder. The operation
     * does nothing when rotation is disabled.
     *
     * @param radians finite non-negative angle
     * @throws IllegalArgumentException if {@code radians} is invalid
     * @throws IllegalStateException if the camera state cannot be controlled
     */
    public void rotateUp(float radians) {
        if (!rotationEnabled) {
            return;
        }
        orbitState.rotateUp(Preconditions.requireNonNegative(radians, "radians"));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Applies or begins a pan using logical-window-pixel offsets and the configured pan mode.
     *
     * <p>Positive horizontal input moves the camera left; positive vertical input moves it up.
     * The operation does nothing when panning is disabled.
     *
     * @param horizontalPixels finite horizontal input
     * @param verticalPixels finite vertical input
     * @throws IllegalArgumentException if either input is not finite
     * @throws IllegalStateException if the camera or window state cannot be controlled
     */
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
                effectiveViewportWidth(),
                effectiveViewportHeight(),
                camera,
                panSpeed,
                screenSpacePanning);
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Moves a perspective camera toward the target or increases orthographic zoom.
     *
     * <p>The operation does nothing when zoom is disabled.
     *
     * @param factor finite factor greater than one
     * @throws IllegalArgumentException if {@code factor} is invalid
     * @throws IllegalStateException if the camera state cannot be controlled
     */
    public void dollyIn(float factor) {
        if (!zoomEnabled) {
            return;
        }
        orbitState.dollyIn(Preconditions.requireGreaterThan(factor, "factor", 1.0f));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Moves a perspective camera away from the target or decreases orthographic zoom.
     *
     * <p>The operation does nothing when zoom is disabled.
     *
     * @param factor finite factor greater than one
     * @throws IllegalArgumentException if {@code factor} is invalid
     * @throws IllegalStateException if the camera state cannot be controlled
     */
    public void dollyOut(float factor) {
        if (!zoomEnabled) {
            return;
        }
        orbitState.dollyOut(Preconditions.requireGreaterThan(factor, "factor", 1.0f));
        applyQueuedMotion(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Saves the current target, camera position, and orthographic zoom for {@link #reset()}.
     *
     * <p>Perspective projection properties and camera orientation are not saved; reset derives
     * orientation by aiming the restored position at the restored target.
     *
     * @throws IllegalStateException if the camera is parented, coincides with the target, or has
     *     invalid position state
     */
    public void saveState() {
        requireUnparentedCamera(true);
        synchronize();
        savedTarget.set(orbitState.target());
        savedPosition.set(camera.position());
        if (camera instanceof OrthographicCamera orthographicCamera) {
            savedZoom = orthographicCamera.zoom();
        }
    }

    /**
     * Restores the most recently saved target, camera position, and orthographic zoom.
     *
     * <p>The constructor saves the initial state, and {@link #saveState()} can replace it. Reset
     * also discards pending damped motion.
     *
     * @throws IllegalStateException if the camera is parented or the saved state cannot be applied
     */
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

    /**
     * Applies input and pending motion using a nominal 60-Hz elapsed time.
     *
     * @return {@code true} if the camera position, orientation, or zoom changed
     * @throws IllegalStateException if the enabled control cannot use the current camera or window
     *     state
     */
    public boolean update() {
        return update(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Applies input and pending motion using an elapsed duration in seconds.
     *
     * @param elapsedSeconds finite non-negative time since the previous update
     * @return {@code true} if the camera position, orientation, or zoom changed
     * @throws IllegalArgumentException if {@code elapsedSeconds} is invalid
     * @throws IllegalStateException if the enabled control cannot use the current camera or window
     *     state
     */
    public boolean update(float elapsedSeconds) {
        return update(elapsedSeconds, true);
    }

    /**
     * Applies keyboard input and pending motion without reading pointer buttons, movement, or
     * scrolling.
     *
     * <p>This is intended for frames in which an overlaid control panel has claimed pointer
     * input. Automatic rotation and damping continue normally.
     *
     * @return {@code true} if the camera position, orientation, or zoom changed
     * @throws IllegalStateException if the enabled control cannot use the current camera or window
     *     state
     */
    public boolean updateWithoutPointerInput() {
        return updateWithoutPointerInput(DEFAULT_SECONDS_PER_UPDATE);
    }

    /**
     * Applies keyboard input and pending motion without reading pointer input, using an elapsed
     * duration in seconds.
     *
     * @param elapsedSeconds finite non-negative time since the previous update
     * @return {@code true} if the camera position, orientation, or zoom changed
     * @throws IllegalArgumentException if {@code elapsedSeconds} is invalid
     * @throws IllegalStateException if the enabled control cannot use the current camera or window
     *     state
     */
    public boolean updateWithoutPointerInput(float elapsedSeconds) {
        return update(elapsedSeconds, false);
    }

    /** Applies input categories selected by the caller and all pending motion. */
    private boolean update(float elapsedSeconds, boolean pointerInputEnabled) {
        float validElapsedSeconds = Preconditions.requireNonNegative(elapsedSeconds, "elapsedSeconds");
        if (!enabled) {
            return false;
        }
        requireUnparentedCamera(true);
        synchronize();

        InputState input = window.input();
        boolean modifierDown = isModifierDown(input);
        boolean userInteracting = pointerInputEnabled && processPointerInput(input, modifierDown);
        userInteracting |= processKeyboardInput(input, modifierDown);
        if (pointerInputEnabled && zoomEnabled && input.scrollDeltaY() != 0.0) {
            orbitState.dolly(input.scrollDeltaY(), zoomSpeed);
            userInteracting = true;
        }
        if (autoRotationEnabled && rotationEnabled && !userInteracting) {
            orbitState.rotateLeft(TWO_PI / 60.0f * autoRotationSpeed * validElapsedSeconds);
        }
        return applyQueuedMotion(validElapsedSeconds);
    }

    /** Routes current pointer state to the highest-precedence enabled pointer operation. */
    private boolean processPointerInput(InputState input, boolean modifierDown) {
        double deltaX = input.pointerDeltaX();
        double deltaY = input.pointerDeltaY();
        int width = effectiveViewportWidth();
        int height = effectiveViewportHeight();
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

    /** Applies held arrow keys as panning or modified rotation input. */
    private boolean processKeyboardInput(InputState input, boolean modifierDown) {
        float horizontal = axis(input, Key.LEFT, Key.RIGHT);
        float vertical = axis(input, Key.UP, Key.DOWN);
        if (horizontal == 0.0f && vertical == 0.0f) {
            return false;
        }
        int width = effectiveViewportWidth();
        int height = effectiveViewportHeight();
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

    /** Applies queued state with limits and damping, then reports observable camera changes. */
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

    /** Synchronizes spherical state from the caller-owned camera's current position. */
    private void synchronize() {
        orbitState.synchronize(camera.position());
    }

    /** Requires the supported unparented-camera state with the appropriate failure category. */
    private void requireUnparentedCamera(boolean stateFailure) {
        if (camera.parent() != null) {
            String message = "OrbitControls requires an unparented camera";
            if (stateFailure) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
    }

    /** Returns the explicit logical viewport width or the current complete window width. */
    private int effectiveViewportWidth() {
        return viewportWidth == 0 ? Math.max(window.width(), 1) : viewportWidth;
    }

    /** Returns the explicit logical viewport height or the current complete window height. */
    private int effectiveViewportHeight() {
        return viewportHeight == 0 ? Math.max(window.height(), 1) : viewportHeight;
    }

    /** Returns whether a Shift, Control, or Super modifier is held. */
    private static boolean isModifierDown(InputState input) {
        return input.isKeyDown(Key.LEFT_SHIFT)
                || input.isKeyDown(Key.RIGHT_SHIFT)
                || input.isKeyDown(Key.LEFT_CONTROL)
                || input.isKeyDown(Key.RIGHT_CONTROL)
                || input.isKeyDown(Key.LEFT_SUPER)
                || input.isKeyDown(Key.RIGHT_SUPER);
    }

    /** Converts a pair of opposing held keys to a signed unit axis. */
    private static float axis(InputState input, Key positive, Key negative) {
        return (input.isKeyDown(positive) ? 1.0f : 0.0f) - (input.isKeyDown(negative) ? 1.0f : 0.0f);
    }
}
