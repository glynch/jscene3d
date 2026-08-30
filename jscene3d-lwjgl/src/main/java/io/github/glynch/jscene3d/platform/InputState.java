/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Stable, read-only view of one window's keyboard, mouse, pointer, and scrolling state.
 *
 * <p>Held state and pointer position persist between event polls. Press, release, movement, and
 * scrolling values describe only the latest call to {@link Window#pollEvents()}. Reading a value
 * does not consume it, and no query allocates.
 */
public final class InputState {
    private final boolean[] keysDown = new boolean[Key.platformCodeLimit()];
    private final boolean[] keysPressed = new boolean[Key.platformCodeLimit()];
    private final boolean[] keysReleased = new boolean[Key.platformCodeLimit()];
    private final boolean[] mouseButtonsDown = new boolean[MouseButton.platformCodeLimit()];
    private final boolean[] mouseButtonsPressed = new boolean[MouseButton.platformCodeLimit()];
    private final boolean[] mouseButtonsReleased = new boolean[MouseButton.platformCodeLimit()];

    private double pointerX;
    private double pointerY;
    private double pointerDeltaX;
    private double pointerDeltaY;
    private double scrollDeltaX;
    private double scrollDeltaY;

    InputState() {}

    /**
     * Returns whether a key is currently held.
     *
     * @param key the key to query
     * @return {@code true} while held
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean isKeyDown(Key key) {
        return keysDown[Objects.requireNonNull(key, "key").platformCode()];
    }

    /**
     * Returns whether a key changed to pressed during the latest event poll.
     *
     * @param key the key to query
     * @return {@code true} for the latest polling cycle's press transition
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean wasKeyPressed(Key key) {
        return keysPressed[Objects.requireNonNull(key, "key").platformCode()];
    }

    /**
     * Returns whether a key changed to released during the latest event poll.
     *
     * @param key the key to query
     * @return {@code true} for the latest polling cycle's release transition
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean wasKeyReleased(Key key) {
        return keysReleased[Objects.requireNonNull(key, "key").platformCode()];
    }

    /**
     * Returns whether a mouse button is currently held.
     *
     * @param button the button to query
     * @return {@code true} while held
     * @throws NullPointerException if {@code button} is {@code null}
     */
    public boolean isMouseButtonDown(MouseButton button) {
        return mouseButtonsDown[Objects.requireNonNull(button, "button").platformCode()];
    }

    /**
     * Returns whether a mouse button changed to pressed during the latest event poll.
     *
     * @param button the button to query
     * @return {@code true} for the latest polling cycle's press transition
     * @throws NullPointerException if {@code button} is {@code null}
     */
    public boolean wasMouseButtonPressed(MouseButton button) {
        return mouseButtonsPressed[Objects.requireNonNull(button, "button").platformCode()];
    }

    /**
     * Returns whether a mouse button changed to released during the latest event poll.
     *
     * @param button the button to query
     * @return {@code true} for the latest polling cycle's release transition
     * @throws NullPointerException if {@code button} is {@code null}
     */
    public boolean wasMouseButtonReleased(MouseButton button) {
        return mouseButtonsReleased[Objects.requireNonNull(button, "button").platformCode()];
    }

    /**
     * Returns the current horizontal pointer position.
     *
     * @return the position in logical window coordinates
     */
    public double pointerX() {
        return pointerX;
    }

    /**
     * Returns the current vertical pointer position.
     *
     * @return the position in logical window coordinates
     */
    public double pointerY() {
        return pointerY;
    }

    /**
     * Returns horizontal pointer movement accumulated during the latest event poll.
     *
     * @return the horizontal movement in logical window coordinates
     */
    public double pointerDeltaX() {
        return pointerDeltaX;
    }

    /**
     * Returns vertical pointer movement accumulated during the latest event poll.
     *
     * @return the vertical movement in logical window coordinates
     */
    public double pointerDeltaY() {
        return pointerDeltaY;
    }

    /**
     * Returns horizontal scrolling accumulated during the latest event poll.
     *
     * @return the horizontal scroll offset
     */
    public double scrollDeltaX() {
        return scrollDeltaX;
    }

    /**
     * Returns vertical scrolling accumulated during the latest event poll.
     *
     * @return the vertical scroll offset
     */
    public double scrollDeltaY() {
        return scrollDeltaY;
    }

    void beginPoll() {
        Arrays.fill(keysPressed, false);
        Arrays.fill(keysReleased, false);
        Arrays.fill(mouseButtonsPressed, false);
        Arrays.fill(mouseButtonsReleased, false);
        pointerDeltaX = 0.0;
        pointerDeltaY = 0.0;
        scrollDeltaX = 0.0;
        scrollDeltaY = 0.0;
    }

    void initializePointer(double x, double y) {
        pointerX = x;
        pointerY = y;
    }

    void updateKey(@Nullable Key key, int action) {
        if (key == null || action == GLFW.GLFW_REPEAT) {
            return;
        }
        int index = key.platformCode();
        if (action == GLFW.GLFW_PRESS) {
            keysDown[index] = true;
            keysPressed[index] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            keysDown[index] = false;
            keysReleased[index] = true;
        }
    }

    void updateMouseButton(@Nullable MouseButton button, int action) {
        if (button == null) {
            return;
        }
        int index = button.platformCode();
        if (action == GLFW.GLFW_PRESS) {
            mouseButtonsDown[index] = true;
            mouseButtonsPressed[index] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            mouseButtonsDown[index] = false;
            mouseButtonsReleased[index] = true;
        }
    }

    void updatePointer(double x, double y) {
        pointerDeltaX += x - pointerX;
        pointerDeltaY += y - pointerY;
        pointerX = x;
        pointerY = y;
    }

    void updateScroll(double xOffset, double yOffset) {
        scrollDeltaX += xOffset;
        scrollDeltaY += yOffset;
    }

    void releaseHeldButtons() {
        releaseHeld(keysDown, keysReleased);
        releaseHeld(mouseButtonsDown, mouseButtonsReleased);
    }

    private static void releaseHeld(boolean[] held, boolean[] released) {
        for (int index = 0; index < held.length; index++) {
            if (held[index]) {
                held[index] = false;
                released[index] = true;
            }
        }
    }
}
