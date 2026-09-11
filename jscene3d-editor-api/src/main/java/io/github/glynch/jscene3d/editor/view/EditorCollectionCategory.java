/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.Objects;
import java.util.Optional;

/**
 * One optional navigation category exposed by a collection view.
 *
 * @param id stable non-blank category identity
 * @param label non-blank human-readable label
 * @param icon optional editor icon identity
 */
public record EditorCollectionCategory(String id, String label, Optional<String> icon) {
    /** Copies and validates category metadata. */
    public EditorCollectionCategory {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(icon, "icon");
    }
}
