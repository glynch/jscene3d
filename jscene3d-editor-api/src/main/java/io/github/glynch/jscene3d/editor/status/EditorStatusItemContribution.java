/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.status;

import java.util.Objects;

/**
 * Stable placement metadata for one extension-owned status item.
 *
 * @param id stable status-item identity
 * @param alignment status-bar region
 * @param priority descending presentation priority within the region
 */
public record EditorStatusItemContribution(StatusItemId id, StatusBarAlignment alignment, int priority) {
    /** Validates one status-item contribution. */
    public EditorStatusItemContribution {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(alignment, "alignment");
    }
}
