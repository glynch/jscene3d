/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import java.util.Arrays;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

/** Pollable state for one runtime-assigned standard gamepad. */
public final class GamepadState implements AutoCloseable {
    private static final int SLOT_COUNT = GLFW.GLFW_JOYSTICK_LAST - GLFW.GLFW_JOYSTICK_1 + 1;

    private final int joystick;
    private final GLFWGamepadState nativeState = GLFWGamepadState.calloc();
    private final boolean[] buttonsDown = new boolean[GamepadButton.values().length];
    private final boolean[] buttonsPressed = new boolean[GamepadButton.values().length];
    private final boolean[] buttonsReleased = new boolean[GamepadButton.values().length];
    private final float[] axes = new float[GamepadAxis.values().length];
    private boolean connected;
    private boolean closed;

    /** Selects one runtime gamepad slot, independent of authored bindings.
     *
     * @param slot zero-based GLFW joystick slot
     */
    public GamepadState(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IllegalArgumentException("slot must be in [0, " + SLOT_COUNT + "): " + slot);
        }
        joystick = GLFW.GLFW_JOYSTICK_1 + slot;
    }

    /** Polls the selected device through GLFW's standard-gamepad mapping. */
    public void poll() {
        requireOpen();
        GlfwRuntime.requireActiveOwnerThread();
        Arrays.fill(buttonsPressed, false);
        Arrays.fill(buttonsReleased, false);
        connected = GLFW.glfwJoystickIsGamepad(joystick) && GLFW.glfwGetGamepadState(joystick, nativeState);
        for (GamepadButton button : GamepadButton.values()) {
            int index = button.ordinal();
            boolean wasDown = buttonsDown[index];
            boolean isDown = connected && nativeState.buttons(button.platformCode()) == GLFW.GLFW_PRESS;
            buttonsDown[index] = isDown;
            buttonsPressed[index] = !wasDown && isDown;
            buttonsReleased[index] = wasDown && !isDown;
        }
        for (GamepadAxis axis : GamepadAxis.values()) {
            axes[axis.ordinal()] = connected ? nativeState.axes(axis.platformCode()) : 0.0F;
        }
    }

    /** Returns whether the assigned slot currently contains a standard-mapped gamepad.
     *
     * @return whether the latest poll found a standard gamepad
     */
    public boolean isConnected() {
        requireOpen();
        return connected;
    }

    /** Returns whether one gamepad button is held.
     *
     * @param button standard gamepad button
     * @return whether the button is held
     */
    public boolean isButtonDown(GamepadButton button) {
        requireOpen();
        return buttonsDown[Objects.requireNonNull(button, "button").ordinal()];
    }

    /** Returns whether one gamepad button became held during the latest poll.
     *
     * @param button standard gamepad button
     * @return whether the button was pressed
     */
    public boolean wasButtonPressed(GamepadButton button) {
        requireOpen();
        return buttonsPressed[Objects.requireNonNull(button, "button").ordinal()];
    }

    /** Returns whether one gamepad button ceased being held during the latest poll.
     *
     * @param button standard gamepad button
     * @return whether the button was released
     */
    public boolean wasButtonReleased(GamepadButton button) {
        requireOpen();
        return buttonsReleased[Objects.requireNonNull(button, "button").ordinal()];
    }

    /** Returns one standard gamepad axis in GLFW's normalized range.
     *
     * @param axis standard gamepad axis
     * @return latest normalized axis value
     */
    public float axis(GamepadAxis axis) {
        requireOpen();
        return axes[Objects.requireNonNull(axis, "axis").ordinal()];
    }

    /** Releases the native polling buffer. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            nativeState.free();
        }
    }

    /** Rejects queries after native storage has been released. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("gamepad state is closed");
        }
    }
}
