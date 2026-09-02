/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

/**
 * Host-interface ownership that suppresses corresponding game input for one sampled frame.
 *
 * @param keyboard whether host UI owns keyboard input
 * @param pointer whether host UI owns pointer buttons and movement
 */
public record InputCapture(boolean keyboard, boolean pointer) {
    /** No host interface owns game input. */
    public static final InputCapture NONE = new InputCapture(false, false);

    /** Host interface owns both keyboard and pointer input. */
    public static final InputCapture ALL = new InputCapture(true, true);
}
