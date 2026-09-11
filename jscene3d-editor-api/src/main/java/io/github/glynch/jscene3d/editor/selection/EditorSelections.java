/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Optional;
import java.util.function.Consumer;

/** Shared selection facility available to every activated editor extension. */
public interface EditorSelections {
    /** Returns the current selection. */
    Optional<EditorSelection> current();

    /** Replaces the current selection. */
    void select(EditorSelection selection);

    /** Clears the current selection. */
    void clear();

    /** Observes selection changes and immediately receives the current state. */
    EditorRegistration observe(Consumer<Optional<EditorSelection>> listener);
}
