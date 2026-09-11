/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.icon;

import java.util.Objects;

/** Resolved vector glyph and semantic colour role supplied by an icon theme. */
record JavaFxIconGlyph(String path, Tone tone) {
    JavaFxIconGlyph {
        if (Objects.requireNonNull(path, "path").isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        Objects.requireNonNull(tone, "tone");
    }

    enum Tone {
        PRIMARY,
        READ_ONLY,
        DISABLED
    }
}
