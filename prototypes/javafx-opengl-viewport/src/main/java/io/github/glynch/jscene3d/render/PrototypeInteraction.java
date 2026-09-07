/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Shares the small amount of mutable JavaFX input state consumed by rendered frames. */
final class PrototypeInteraction {
    private boolean spinning = true;
    private float rotationSpeed = 0.7f;
    private float pendingHorizontalDrag;
    private float pendingVerticalDrag;
    private boolean resetRequested;

    /** Returns whether automatic rotation is enabled. */
    synchronized boolean isSpinning() {
        return spinning;
    }

    /** Changes whether automatic rotation is enabled. */
    synchronized void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    /** Returns the current angular speed in radians per second. */
    synchronized float rotationSpeed() {
        return rotationSpeed;
    }

    /** Changes the angular speed used for subsequent frames. */
    synchronized void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /** Accumulates one JavaFX pointer-drag delta for the next rendered frame. */
    synchronized void drag(float horizontal, float vertical) {
        pendingHorizontalDrag += horizontal;
        pendingVerticalDrag += vertical;
    }

    /** Requests that the next frame restore the initial scene orientation. */
    synchronized void requestReset() {
        resetRequested = true;
    }

    /** Removes and returns all input accumulated since the preceding rendered frame. */
    synchronized FrameInput consume() {
        FrameInput input =
                new FrameInput(spinning, rotationSpeed, pendingHorizontalDrag, pendingVerticalDrag, resetRequested);
        pendingHorizontalDrag = 0.0f;
        pendingVerticalDrag = 0.0f;
        resetRequested = false;
        return input;
    }

    /** Immutable interaction snapshot consumed by one frame. */
    record FrameInput(
            boolean spinning, float rotationSpeed, float horizontalDrag, float verticalDrag, boolean resetRequested) {}
}
