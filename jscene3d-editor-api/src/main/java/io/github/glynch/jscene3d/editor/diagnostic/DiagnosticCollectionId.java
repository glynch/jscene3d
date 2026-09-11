/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of one extension-owned diagnostic collection.
 *
 * @param value lowercase dotted namespaced identity
 */
public record DiagnosticCollectionId(String value) {
    /** Validates one diagnostic-collection identity. */
    public DiagnosticCollectionId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
