/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Registers visual placements for independently registered commands. */
@FunctionalInterface
public interface EditorCommandPlacementRegistry {
    /**
     * Registers one command placement.
     *
     * @param placement command and target action surface
     * @return removable placement
     */
    EditorRegistration register(EditorCommandPlacement placement);
}
