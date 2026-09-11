/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.concurrent.CompletionStage;

/**
 * Supplies asynchronous semantic collection data without constructing toolkit controls.
 *
 * @param <T> semantic element type
 */
public interface EditorCollectionDataProvider<T> {
    /**
     * Returns one consistent snapshot of the current collection.
     *
     * @return eventual immutable collection snapshot
     */
    CompletionStage<EditorCollectionSnapshot<T>> snapshot();

    /**
     * Projects one semantic element into its toolkit-independent presentation.
     *
     * @param element element previously returned by this provider
     * @return collection-item presentation
     */
    EditorCollectionItem item(T element);

    /**
     * Observes complete collection invalidations.
     *
     * @param listener synchronous invalidation listener
     * @return removable listener registration
     */
    EditorRegistration observeChanges(Runnable listener);
}
