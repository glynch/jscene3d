/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of an editor-owned surface capable of displaying commands.
 *
 * @param value lowercase dotted namespaced identity
 */
public record CommandLocationId(String value) {
    /** Validates one command-location identity. */
    public CommandLocationId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
