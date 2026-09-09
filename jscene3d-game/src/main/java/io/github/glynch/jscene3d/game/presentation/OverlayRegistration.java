/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

/** Removable registration of one ordered screen overlay in a presentation world module. */
@FunctionalInterface
public interface OverlayRegistration extends AutoCloseable {
    /** Removes the overlay. Repeated closure is harmless. */
    @Override
    void close();
}
