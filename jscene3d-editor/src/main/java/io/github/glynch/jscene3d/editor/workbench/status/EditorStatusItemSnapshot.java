/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.status;

import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import java.util.Objects;

/**
 * Immutable workbench snapshot of one registered status item and its current state.
 *
 * @param contribution registered status-item contribution
 * @param state current status-item presentation state
 */
public record EditorStatusItemSnapshot(EditorStatusItemContribution contribution, EditorStatusItemState state) {
    /** Validates one complete status-item snapshot. */
    public EditorStatusItemSnapshot {
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(state, "state");
    }
}
