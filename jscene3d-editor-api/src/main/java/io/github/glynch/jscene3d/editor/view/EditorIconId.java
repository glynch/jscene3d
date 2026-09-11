/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable semantic identity of an icon rendered by the editor workbench.
 *
 * @param value namespaced icon identity
 */
public record EditorIconId(String value) {
    /** Validates one editor icon identity. */
    public EditorIconId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
