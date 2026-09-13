/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/** Stable identity of one contributed workspace file type. */
public record EditorFileTypeId(String value) {
    /** Validates one reverse-domain file-type identity. */
    public EditorFileTypeId {
        value = requireNamespacedId(value, "value");
    }
}
