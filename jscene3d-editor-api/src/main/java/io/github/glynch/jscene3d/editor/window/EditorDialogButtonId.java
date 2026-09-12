/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/** Stable identity returned when a modal-dialog action is selected. */
public record EditorDialogButtonId(String value) {
    /** Validates one dialog action identity. */
    public EditorDialogButtonId {
        value = requireNamespacedId(value, "value");
    }

    @Override
    public String toString() {
        return value;
    }
}
