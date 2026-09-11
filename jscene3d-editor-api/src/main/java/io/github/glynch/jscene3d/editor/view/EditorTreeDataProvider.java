/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Supplies asynchronous semantic tree data without constructing toolkit controls.
 *
 * @param <T> semantic element type
 */
public interface EditorTreeDataProvider<T> {
    /**
     * Returns the current root elements in presentation order.
     *
     * @return eventual immutable root elements
     */
    CompletionStage<List<T>> roots();

    /**
     * Returns the current children of one collapsible element in presentation order.
     *
     * @param parent parent element previously returned by this provider
     * @return eventual immutable child elements
     */
    CompletionStage<List<T>> children(T parent);

    /**
     * Projects one semantic element into its toolkit-independent presentation.
     *
     * @param element element previously returned by this provider
     * @return tree-item presentation
     */
    EditorTreeItem item(T element);

    /**
     * Observes invalidated elements; an empty value invalidates the complete tree.
     *
     * @param listener synchronous invalidation listener
     * @return removable listener registration
     */
    EditorRegistration observeChanges(Consumer<Optional<T>> listener);
}
