/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of an editor view presentation kind.
 *
 * @param value lowercase dotted namespaced identity
 */
public record ViewKindId(String value) {
    /** Validates one view-kind identity. */
    public ViewKindId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
