/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** A mouse button recognized by the version 0.1 input interface. */
public enum MouseButton {
    /** The primary, normally left, mouse button. */
    LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT),
    /** The secondary, normally right, mouse button. */
    RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
    /** The middle mouse button. */
    MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE),
    /** Mouse button 4. */
    BUTTON_4(GLFW.GLFW_MOUSE_BUTTON_4),
    /** Mouse button 5. */
    BUTTON_5(GLFW.GLFW_MOUSE_BUTTON_5),
    /** Mouse button 6. */
    BUTTON_6(GLFW.GLFW_MOUSE_BUTTON_6),
    /** Mouse button 7. */
    BUTTON_7(GLFW.GLFW_MOUSE_BUTTON_7),
    /** Mouse button 8. */
    BUTTON_8(GLFW.GLFW_MOUSE_BUTTON_8);

    private static final MouseButton[] BY_PLATFORM_CODE = createLookup();

    private final int platformCode;

    /** Associates a supported mouse button with its GLFW code. */
    MouseButton(int platformCode) {
        this.platformCode = platformCode;
    }

    /** Returns the exclusive array bound needed to index every GLFW mouse-button code. */
    static int platformCodeLimit() {
        return GLFW.GLFW_MOUSE_BUTTON_LAST + 1;
    }

    /** Returns the corresponding GLFW mouse-button code. */
    int platformCode() {
        return platformCode;
    }

    /** Returns the supported button for a GLFW code, or {@code null} when unsupported. */
    static @Nullable MouseButton fromPlatformCode(int platformCode) {
        return platformCode >= 0 && platformCode < BY_PLATFORM_CODE.length ? BY_PLATFORM_CODE[platformCode] : null;
    }

    /** Builds constant-time GLFW-code lookup storage. */
    private static MouseButton[] createLookup() {
        MouseButton[] lookup = new MouseButton[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
        for (MouseButton button : values()) {
            lookup[button.platformCode] = button;
        }
        return lookup;
    }
}
