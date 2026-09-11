/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Toolkit-independent selection shared between a logical collection view and the workbench.
 *
 * @param <T> semantic element type
 */
public interface EditorCollectionSelectionModel<T> {
    /**
     * Returns the currently selected semantic element, when it belongs to this collection.
     *
     * @return current selection, or empty when this collection has no selection
     */
    Optional<T> selection();

    /**
     * Replaces the selection in response to user interaction in the rendered collection.
     *
     * @param selection new selection, or empty to clear it
     */
    void select(Optional<T> selection);

    /**
     * Observes selection changes originating from this or another editor surface.
     *
     * <p>The listener immediately receives the current selection.
     *
     * @param listener synchronous selection listener
     * @return removable listener registration
     */
    EditorRegistration observe(Consumer<Optional<T>> listener);
}
