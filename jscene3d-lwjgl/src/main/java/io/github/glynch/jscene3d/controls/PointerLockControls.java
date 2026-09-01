/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Window;
import java.util.Objects;

/**
 * Captured-pointer yaw and pitch control for an unparented camera.
 *
 * <p>Call {@link #lock()} in response to an intentional user action and {@link #update()} once
 * after each {@link Window#pollEvents()} call. The control consumes only relative pointer movement;
 * movement, collision, and other game rules remain caller responsibilities. Losing window focus
 * releases pointer lock through the owning window.
 *
 * <p>The camera and window remain caller-owned. Closing the control restores the normal cursor
 * mode but does not close either object. This control is mutable, not thread-safe, and has the
 * same thread affinity as its window.
 */
public final class PointerLockControls implements AutoCloseable {
    private static final float DEFAULT_SENSITIVITY = 0.002f;
    private static final float POLAR_EPSILON = 1.0e-4f;

    private final Camera camera;
    private final Window window;
    private final PointerLockState state = new PointerLockState();

    private boolean enabled = true;
    private boolean rawMouseMotionPreferred = true;
    private boolean closed;
    private float sensitivity = DEFAULT_SENSITIVITY;
    private float minimumPitch = -PI_OVER_TWO + POLAR_EPSILON;
    private float maximumPitch = PI_OVER_TWO - POLAR_EPSILON;
    private float savedYaw;
    private float savedPitch;

    /**
     * Creates unlocked controls and saves the camera's initial yaw and pitch.
     *
     * <p>Any initial camera roll is removed because pointer-lock orientation is defined entirely by
     * world-Y yaw and local-X pitch.
     *
     * @param camera caller-owned camera to orient
     * @param window caller-owned input window
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code camera} is parented
     */
    public PointerLockControls(Camera camera, Window window) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.window = Objects.requireNonNull(window, "window");
        requireUnparentedCamera(false);
        state.synchronize(camera.quaternion());
        state.apply(camera);
        saveState();
    }

    /**
     * Returns whether calls to {@link #update()} may change the camera.
     *
     * @return {@code true} by default
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables camera orientation updates without changing pointer ownership.
     *
     * @param enabled whether updates may change the camera
     */
    public void setEnabled(boolean enabled) {
        requireOpen();
        this.enabled = enabled;
    }

    /**
     * Returns whether this control currently owns unconstrained relative pointer input.
     *
     * @return {@code true} while the owning window's cursor is disabled
     */
    public boolean isLocked() {
        requireOpen();
        return window.cursorMode() == CursorMode.DISABLED;
    }

    /**
     * Disables the native cursor and enables raw motion when preferred and supported.
     *
     * @throws IllegalStateException if the control or window is closed, the camera is parented,
     *     or the method runs on the wrong thread
     */
    public void lock() {
        requireOpen();
        requireUnparentedCamera(true);
        window.setCursorMode(CursorMode.DISABLED);
        if (rawMouseMotionPreferred && window.isRawMouseMotionSupported()) {
            window.setRawMouseMotionEnabled(true);
        }
    }

    /** Restores the normal native cursor without changing camera orientation. */
    public void unlock() {
        requireOpen();
        window.setCursorMode(CursorMode.NORMAL);
    }

    /**
     * Returns pointer sensitivity in radians per reported pointer unit.
     *
     * @return the finite non-negative sensitivity
     */
    public float sensitivity() {
        return sensitivity;
    }

    /**
     * Sets pointer sensitivity in radians per reported pointer unit.
     *
     * @param sensitivity finite non-negative sensitivity
     * @throws IllegalArgumentException if {@code sensitivity} is invalid
     * @throws IllegalStateException if this control is closed
     */
    public void setSensitivity(float sensitivity) {
        requireOpen();
        this.sensitivity = Preconditions.requireNonNegative(sensitivity, "sensitivity");
    }

    /**
     * Returns the minimum permitted pitch angle in radians.
     *
     * @return the finite lower pitch limit
     */
    public float minimumPitch() {
        return minimumPitch;
    }

    /**
     * Returns the maximum permitted pitch angle in radians.
     *
     * @return the finite upper pitch limit
     */
    public float maximumPitch() {
        return maximumPitch;
    }

    /**
     * Sets the inclusive camera-pitch interval inside the vertical poles.
     *
     * @param minimumPitch finite lower angle greater than negative pi over two
     * @param maximumPitch finite upper angle less than positive pi over two
     * @throws IllegalArgumentException if a value or their ordering is invalid
     * @throws IllegalStateException if this control is closed
     */
    public void setPitchLimits(float minimumPitch, float maximumPitch) {
        requireOpen();
        float validMinimum = Preconditions.requireInRange(
                minimumPitch, "minimumPitch", -PI_OVER_TWO + POLAR_EPSILON, PI_OVER_TWO - POLAR_EPSILON);
        float validMaximum = Preconditions.requireInRange(
                maximumPitch, "maximumPitch", -PI_OVER_TWO + POLAR_EPSILON, PI_OVER_TWO - POLAR_EPSILON);
        Preconditions.requireOrdered(validMinimum, "minimumPitch", validMaximum, "maximumPitch");
        this.minimumPitch = validMinimum;
        this.maximumPitch = validMaximum;
        setAngles(state.yaw(), Math.clamp(state.pitch(), validMinimum, validMaximum));
    }

    /**
     * Returns whether lock attempts enable raw motion on supporting platforms.
     *
     * @return {@code true} by default
     */
    public boolean isRawMouseMotionPreferred() {
        return rawMouseMotionPreferred;
    }

    /**
     * Chooses whether lock attempts enable raw mouse motion on supporting platforms.
     *
     * <p>Changing this preference while locked applies it immediately.
     *
     * @param preferred whether unaccelerated raw motion is preferred
     * @throws IllegalStateException if this control is closed
     */
    public void setRawMouseMotionPreferred(boolean preferred) {
        requireOpen();
        rawMouseMotionPreferred = preferred;
        if (window.cursorMode() != CursorMode.DISABLED) {
            return;
        }
        boolean enableRawMotion = preferred && window.isRawMouseMotionSupported();
        window.setRawMouseMotionEnabled(enableRawMotion);
    }

    /**
     * Returns the current normalized yaw angle in radians.
     *
     * @return world-Y yaw in the negative-pi through positive-pi interval
     */
    public float yaw() {
        return state.yaw();
    }

    /**
     * Returns the current pitch angle in radians.
     *
     * @return local-X pitch inside the configured limits
     */
    public float pitch() {
        return state.pitch();
    }

    /**
     * Replaces the controlled yaw and pitch immediately.
     *
     * @param yaw finite world-Y yaw in radians
     * @param pitch finite local-X pitch inside the configured limits
     * @throws IllegalArgumentException if an angle is invalid
     * @throws IllegalStateException if the control is closed or the camera is parented
     */
    public void setAngles(float yaw, float pitch) {
        requireOpen();
        requireUnparentedCamera(true);
        float validYaw = Preconditions.requireFinite(yaw, "yaw");
        float validPitch = Preconditions.requireInRange(pitch, "pitch", minimumPitch, maximumPitch);
        state.setAngles(validYaw, validPitch);
        state.apply(camera);
    }

    /** Saves the current controlled angles for {@link #reset()}. */
    public void saveState() {
        requireOpen();
        requireUnparentedCamera(true);
        savedYaw = state.yaw();
        savedPitch = state.pitch();
    }

    /** Restores the most recently saved yaw and pitch. */
    public void reset() {
        setAngles(savedYaw, savedPitch);
    }

    /**
     * Applies relative pointer movement accumulated during the latest event poll.
     *
     * @return {@code true} when the camera orientation changed
     * @throws IllegalStateException if the enabled control cannot use its camera or window
     */
    public boolean update() {
        requireOpen();
        if (!enabled || window.cursorMode() != CursorMode.DISABLED) {
            return false;
        }
        requireUnparentedCamera(true);
        InputState input = window.input();
        if (!state.rotate(input.pointerDeltaX(), input.pointerDeltaY(), sensitivity, minimumPitch, maximumPitch)) {
            return false;
        }
        state.apply(camera);
        return true;
    }

    /**
     * Restores the normal cursor and ends this control's lifecycle.
     *
     * <p>Repeated calls do nothing.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (!window.isClosed()) {
            window.setCursorMode(CursorMode.NORMAL);
        }
        closed = true;
    }

    /** Requires that this control has not been closed. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("PointerLockControls is closed");
        }
    }

    /** Requires the supported unparented-camera state with the appropriate failure category. */
    private void requireUnparentedCamera(boolean stateFailure) {
        if (camera.parent() != null) {
            String message = "PointerLockControls requires an unparented camera";
            if (stateFailure) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
    }
}
