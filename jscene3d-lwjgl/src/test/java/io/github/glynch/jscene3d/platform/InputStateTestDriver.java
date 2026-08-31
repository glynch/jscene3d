/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/** Test-only access to the package-owned input event adapter. */
public final class InputStateTestDriver {
    /** Prevents instantiation of this test utility class. */
    private InputStateTestDriver() {
        throw new AssertionError("InputStateTestDriver cannot be instantiated");
    }

    /** Starts a fresh polling cycle. */
    public static void beginPoll(InputState input) {
        input.beginPoll();
    }

    /** Applies a key press. */
    public static void press(InputState input, Key key) {
        input.updateKey(key, GLFW_PRESS);
    }

    /** Applies a key release. */
    public static void release(InputState input, Key key) {
        input.updateKey(key, GLFW_RELEASE);
    }

    /** Applies a mouse-button press. */
    public static void press(InputState input, MouseButton button) {
        input.updateMouseButton(button, GLFW_PRESS);
    }

    /** Applies a mouse-button release. */
    public static void release(InputState input, MouseButton button) {
        input.updateMouseButton(button, GLFW_RELEASE);
    }

    /** Establishes pointer position without movement. */
    public static void initializePointer(InputState input, double x, double y) {
        input.initializePointer(x, y);
    }

    /** Applies a pointer position update. */
    public static void movePointer(InputState input, double x, double y) {
        input.updatePointer(x, y);
    }

    /** Applies a scroll update. */
    public static void scroll(InputState input, double xOffset, double yOffset) {
        input.updateScroll(xOffset, yOffset);
    }
}
