/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

/** Mutable numeric screen-content capability intended for explicit behavior binding. */
public interface ScreenNumber {
    /**
     * Returns the currently displayed non-negative value.
     *
     * @return displayed value
     */
    int value();

    /**
     * Replaces the displayed value.
     *
     * @param value non-negative value to display
     */
    void setValue(int value);
}
