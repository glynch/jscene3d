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
        if (isDisabled()) {
            return Update.UNCHANGED;
        }
        if (shouldReleaseCapture(focused, escapePressed)) {
            return releaseCapture();
        }
        if (shouldAcquireCapture(focused, primaryPressed)) {
            return acquireCapture();
        }
        return maintainCurrentState();
    }

    /** Returns whether pointer capture is unnecessary for the authored input map. */
    private boolean isDisabled() {
        return !enabled;
    }

    /** Returns whether focus or explicit release input should relinquish current capture. */
    private boolean shouldReleaseCapture(boolean focused, boolean escapePressed) {
        return captured && (!focused || escapePressed);
    }

    /** Returns whether the current primary press should acquire an uncaptured pointer. */
    private boolean shouldAcquireCapture(boolean focused, boolean primaryPressed) {
        return !captured && focused && primaryPressed;
    }

    /** Relinquishes native pointer ownership while suppressing project pointer input. */
    private Update releaseCapture() {
        captured = false;
        return new Update(POINTER_CAPTURE, false, true);
    }

    /** Acquires native pointer ownership while consuming the initiating primary press. */
    private Update acquireCapture() {
        captured = true;
        return new Update(POINTER_CAPTURE, true, false);
    }

    /** Preserves current ownership and exposes input only while the pointer is captured. */
    private Update maintainCurrentState() {
        return new Update(captured ? InputCapture.NONE : POINTER_CAPTURE, false, false);
    }

    /** One frame's input-capture value and requested native cursor transition. */
    record Update(InputCapture inputCapture, boolean capturePointer, boolean releasePointer) {
        private static final Update UNCHANGED = new Update(InputCapture.NONE, false, false);
    }
}
