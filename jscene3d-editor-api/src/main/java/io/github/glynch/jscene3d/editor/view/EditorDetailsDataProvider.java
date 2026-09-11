/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Optional;
import java.util.function.Consumer;

/** Supplies complete immutable states to a standard details view. */
public interface EditorDetailsDataProvider {
    /**
     * Returns the current details, if the view has anything to present.
     *
     * @return current details, or empty when nothing can be presented
     */
    Optional<EditorDetails> details();

    /**
     * Observes changes and immediately receives the current details state.
     *
     * @param listener synchronous details-state listener
     * @return removable listener registration
     */
    EditorRegistration observe(Consumer<Optional<EditorDetails>> listener);
}
