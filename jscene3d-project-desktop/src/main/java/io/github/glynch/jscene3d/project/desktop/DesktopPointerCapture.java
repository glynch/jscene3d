/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.input.InputCapture;

/** Maintains generic desktop pointer ownership for projects using relative mouse input. */
final class DesktopPointerCapture {
    private static final InputCapture POINTER_CAPTURE = new InputCapture(false, true);

    private final boolean enabled;
    private boolean captured;

    /** Enables capture only when the project's authored input map requires relative movement. */
    DesktopPointerCapture(boolean enabled) {
        this.enabled = enabled;
    }

    /** Resolves one polled input state into game-input ownership and native cursor transitions. */
    Update update(boolean focused, boolean escapePressed, boolean primaryPressed) {
        if (!enabled) {
            return Update.UNCHANGED;
        }
        if (captured && (!focused || escapePressed)) {
            captured = false;
            return new Update(POINTER_CAPTURE, false, true);
        }
        if (!captured && focused && primaryPressed) {
            captured = true;
            return new Update(POINTER_CAPTURE, true, false);
        }
        return new Update(captured ? InputCapture.NONE : POINTER_CAPTURE, false, false);
    }

    /** One frame's input-capture value and requested native cursor transition. */
    record Update(InputCapture inputCapture, boolean capturePointer, boolean releasePointer) {
        private static final Update UNCHANGED = new Update(InputCapture.NONE, false, false);
    }
}
