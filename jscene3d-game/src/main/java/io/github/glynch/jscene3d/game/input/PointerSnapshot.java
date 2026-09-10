/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

/** Immutable primary-pointer state in logical viewport coordinates for one input sample.
 *
 * <p>The position may lie outside the viewport when the platform reports an unconstrained cursor.
 * Dimensions describe the logical viewport used by the position and are always positive.
 *
 * @param x finite horizontal position
 * @param y finite vertical position
 * @param viewportWidth positive logical viewport width
 * @param viewportHeight positive logical viewport height
 * @param primaryDown whether the primary pointer button is held
 * @param primaryPressed whether it became pressed during this sample
 * @param primaryReleased whether it became released during this sample
 */
public record PointerSnapshot(
        double x,
        double y,
        int viewportWidth,
        int viewportHeight,
        boolean primaryDown,
        boolean primaryPressed,
        boolean primaryReleased) {

    /** Validates the immutable logical pointer state. */
    public PointerSnapshot {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("pointer position must be finite");
        }
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("pointer viewport dimensions must be positive");
        }
    }

    /** Accumulates transitions while adopting the newer position and held state. */
    PointerSnapshot merge(PointerSnapshot newer) {
        return new PointerSnapshot(
                newer.x,
                newer.y,
                newer.viewportWidth,
                newer.viewportHeight,
                newer.primaryDown,
                primaryPressed || newer.primaryPressed,
                primaryReleased || newer.primaryReleased);
    }

    /** Preserves position and held state while consuming button transitions. */
    PointerSnapshot heldOnly() {
        return new PointerSnapshot(x, y, viewportWidth, viewportHeight, primaryDown, false, false);
    }
}
