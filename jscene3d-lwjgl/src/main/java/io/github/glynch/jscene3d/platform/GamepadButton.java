/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.lwjgl.glfw.GLFW;

/** A button in GLFW's portable standard-gamepad layout. */
public enum GamepadButton {
    /** Bottom face button. */
    BUTTON_SOUTH(GLFW.GLFW_GAMEPAD_BUTTON_A),
    /** Right face button. */
    BUTTON_EAST(GLFW.GLFW_GAMEPAD_BUTTON_B),
    /** Left face button. */
    BUTTON_WEST(GLFW.GLFW_GAMEPAD_BUTTON_X),
    /** Top face button. */
    BUTTON_NORTH(GLFW.GLFW_GAMEPAD_BUTTON_Y),
    /** Left shoulder button. */
    LEFT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER),
    /** Right shoulder button. */
    RIGHT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER),
    /** Back or select button. */
    BACK(GLFW.GLFW_GAMEPAD_BUTTON_BACK),
    /** Start button. */
    START(GLFW.GLFW_GAMEPAD_BUTTON_START),
    /** Guide button. */
    GUIDE(GLFW.GLFW_GAMEPAD_BUTTON_GUIDE),
    /** Left stick button. */
    LEFT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB),
    /** Right stick button. */
    RIGHT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB),
    /** Direction-pad up button. */
    DPAD_UP(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP),
    /** Direction-pad right button. */
    DPAD_RIGHT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT),
    /** Direction-pad down button. */
    DPAD_DOWN(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN),
    /** Direction-pad left button. */
    DPAD_LEFT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT);

    private final int platformCode;

    GamepadButton(int platformCode) {
        this.platformCode = platformCode;
    }

    /** Returns the corresponding GLFW standard-gamepad index. */
    int platformCode() {
        return platformCode;
    }
}
