/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

/** Immutable two-dimensional semantic input value.
 *
 * @param x horizontal value in the inclusive range minus one to one
 * @param y vertical value in the inclusive range minus one to one
 */
public record InputVector2(float x, float y) {
    /** Shared zero input. */
    public static final InputVector2 ZERO = new InputVector2(0.0F, 0.0F);

    /** Validates one bounded finite value. */
    public InputVector2 {
        requireUnit(x, "x");
        requireUnit(y, "y");
    }

    /** Rejects values that cannot be represented by a semantic axis. */
    private static void requireUnit(float value, String name) {
        if (!Float.isFinite(value) || value < -1.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and in [-1, 1]: " + value);
        }
    }
}
