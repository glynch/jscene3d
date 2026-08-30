/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

/** Selects whether buffer swaps synchronize with the display's refresh cycle. */
public enum VerticalSync {
    /**
     * Synchronizes buffer swaps with the display's refresh cycle.
     *
     * <p>This is the default and normally prevents visible tearing.
     */
    ENABLED,

    /**
     * Allows buffer swaps without waiting for the display's refresh cycle.
     *
     * <p>This may increase the observed frame rate at the cost of visible tearing.
     */
    DISABLED
}
