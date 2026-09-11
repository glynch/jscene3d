/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.activity;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of an editor Activity Bar contribution.
 *
 * @param value lowercase dotted namespaced identity
 */
public record ActivityId(String value) {
    /** Validates one activity identity. */
    public ActivityId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
