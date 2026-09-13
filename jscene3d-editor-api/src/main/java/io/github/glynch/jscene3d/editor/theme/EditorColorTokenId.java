/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable semantic identity of one editor color.
 *
 * @param value reverse-domain color-token identity
 */
public record EditorColorTokenId(String value) {
    /** Validates one reverse-domain color-token identity. */
    public EditorColorTokenId {
        value = requireNamespacedId(value, "value");
    }
}
