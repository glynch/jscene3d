/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Owns the single selection shared by all editor browsing surfaces. */
final class EditorSelectionModel {
    private final List<Consumer<Optional<EditorSelection>>> listeners = new ArrayList<>();
    private Optional<EditorSelection> selection = Optional.empty();

    /** Returns the current selection. */
    Optional<EditorSelection> selection() {
        return selection;
    }

    /** Replaces the current selection and notifies observers when its stable value changed. */
    void select(EditorSelection selected) {
        update(Optional.of(Objects.requireNonNull(selected, "selected")));
    }

    /** Clears any selection before project-owned data is replaced. */
    void clear() {
        update(Optional.empty());
    }

    /** Observes selection changes and immediately receives the current state. */
    Runnable subscribe(Consumer<Optional<EditorSelection>> listener) {
        Consumer<Optional<EditorSelection>> validListener = Objects.requireNonNull(listener, "listener");
        listeners.add(validListener);
        validListener.accept(selection);
        return () -> listeners.remove(validListener);
    }

    /** Publishes one stable transition to a defensive listener snapshot. */
    private void update(Optional<EditorSelection> updated) {
        if (selection.equals(updated)) {
            return;
        }
        selection = updated;
        List.copyOf(listeners).forEach(listener -> listener.accept(selection));
    }
}
