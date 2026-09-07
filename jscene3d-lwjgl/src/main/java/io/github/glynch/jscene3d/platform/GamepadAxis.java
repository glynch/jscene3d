/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.lwjgl.glfw.GLFW;

/** An axis in GLFW's portable standard-gamepad layout. */
public enum GamepadAxis {
    /** Left stick horizontal axis. */
    LEFT_STICK_X(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X),
    /** Left stick vertical axis. */
    LEFT_STICK_Y(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y),
    /** Right stick horizontal axis. */
    RIGHT_STICK_X(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X),
    /** Right stick vertical axis. */
    RIGHT_STICK_Y(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y),
    /** Left trigger axis. */
    LEFT_TRIGGER(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER),
    /** Right trigger axis. */
    RIGHT_TRIGGER(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER);

    private final int platformCode;

    GamepadAxis(int platformCode) {
        this.platformCode = platformCode;
    }

    /** Returns the corresponding GLFW standard-gamepad index. */
    int platformCode() {
        return platformCode;
    }
}
