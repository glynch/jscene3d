/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/** Stable semantic identity of one source-code syntax category. */
public record EditorSyntaxTokenId(String value) {
    /** Validates one reverse-domain syntax-token identity. */
    public EditorSyntaxTokenId {
        value = requireNamespacedId(value, "value");
    }
}
