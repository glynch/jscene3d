/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

import java.util.Objects;

/** Stable language identity passed to the source editor and future language adapters. */
public record EditorLanguageId(String value) {
    /** Validates one non-blank language identity. */
    public EditorLanguageId {
        if (Objects.requireNonNull(value, "value").isBlank()) {
            throw new IllegalArgumentException("language identity must not be blank");
        }
    }
}
