/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of one language-support contribution.
 *
 * @param value reverse-domain contribution identity
 */
public record EditorLanguageSupportId(String value) {
    /** Validates one reverse-domain language-support identity. */
    public EditorLanguageSupportId {
        value = requireNamespacedId(value, "value");
    }
}
