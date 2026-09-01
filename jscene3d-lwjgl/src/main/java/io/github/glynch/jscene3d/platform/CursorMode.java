/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.lwjgl.glfw.GLFW;

/** Determines whether the native cursor is visible and constrained by a window boundary. */
public enum CursorMode {
    /** Displays the ordinary platform cursor and reports its position inside the window. */
    NORMAL(GLFW.GLFW_CURSOR_NORMAL),

    /** Hides and virtually recenters the cursor so unconstrained relative motion can be read. */
    DISABLED(GLFW.GLFW_CURSOR_DISABLED);

    private final int platformValue;

    /** Retains the corresponding GLFW input-mode value. */
    CursorMode(int platformValue) {
        this.platformValue = platformValue;
    }

    /** Returns the package-owned GLFW input-mode value. */
    int platformValue() {
        return platformValue;
    }
}
