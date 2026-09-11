/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.status;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Mutable toolkit-independent handle for one registered status-bar item. */
public interface EditorStatusItem extends EditorRegistration {
    /**
     * Returns the stable status-item identity.
     *
     * @return stable identity
     */
    StatusItemId id();

    /**
     * Atomically replaces the item's presentation state.
     *
     * @param state complete new presentation state
     * @throws IllegalStateException if the item has been closed
     */
    void update(EditorStatusItemState state);
}
