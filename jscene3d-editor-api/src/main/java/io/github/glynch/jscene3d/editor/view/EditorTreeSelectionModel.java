/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Toolkit-independent selection shared between a logical tree view and the workbench.
 *
 * @param <T> semantic tree-element type
 */
public interface EditorTreeSelectionModel<T> {
    /**
     * Returns the currently selected semantic element.
     *
     * @return current selection, or empty when nothing in this tree is selected
     */
    Optional<T> selection();

    /**
     * Replaces the selection in response to user interaction in the rendered tree.
     *
     * @param selection selected semantic element, or empty to clear selection
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
