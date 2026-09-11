/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.List;
import java.util.Objects;

/**
 * Atomic state returned by a collection data provider.
 *
 * @param rootLabel non-blank label for the collection's root location
 * @param elements semantic elements in presentation order
 * @param searchable whether the workbench should enable local search
 * @param emptyMessage non-blank message displayed when the current view is empty
 * @param <T> semantic element type
 */
public record EditorCollectionSnapshot<T>(String rootLabel, List<T> elements, boolean searchable, String emptyMessage) {
    /** Copies collections and validates snapshot metadata. */
    public EditorCollectionSnapshot {
        if (Objects.requireNonNull(rootLabel, "rootLabel").isBlank()) {
            throw new IllegalArgumentException("rootLabel must not be blank");
        }
        elements = List.copyOf(elements);
        if (Objects.requireNonNull(emptyMessage, "emptyMessage").isBlank()) {
            throw new IllegalArgumentException("emptyMessage must not be blank");
        }
    }
}
