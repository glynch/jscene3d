/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.Objects;

/**
 * Toolkit-independent icon presentation with accessible explanatory text.
 *
 * @param id stable semantic icon identity
 * @param tooltip non-blank explanation shown by the workbench and exposed to assistive technology
 */
public record EditorIcon(EditorIconId id, String tooltip) {
    /** Validates one icon presentation. */
    public EditorIcon {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(tooltip, "tooltip").isBlank()) {
            throw new IllegalArgumentException("tooltip must not be blank");
        }
    }
}
