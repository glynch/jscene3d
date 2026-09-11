/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable semantic kind of an editor selection.
 *
 * @param value namespaced selection-kind identity
 */
public record EditorSelectionKindId(String value) {
    /** Validates one selection-kind identity. */
    public EditorSelectionKindId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
