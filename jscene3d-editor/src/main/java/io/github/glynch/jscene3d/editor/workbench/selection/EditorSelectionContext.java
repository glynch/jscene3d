/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.selection;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Workbench-owned implementation of the selection shared by activated extensions. */
public final class EditorSelectionContext implements EditorSelections {
    private final List<Consumer<Optional<EditorSelection>>> listeners = new ArrayList<>();
    private Optional<EditorSelection> selection = Optional.empty();

    @Override
    public Optional<EditorSelection> current() {
        return selection;
    }

    @Override
    public void select(EditorSelection selected) {
        update(Optional.of(Objects.requireNonNull(selected, "selected")));
    }

    @Override
    public void clear() {
        update(Optional.empty());
    }

    @Override
    public EditorRegistration observe(Consumer<Optional<EditorSelection>> listener) {
        Consumer<Optional<EditorSelection>> observer = Objects.requireNonNull(listener, "listener");
        listeners.add(observer);
        observer.accept(selection);
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                listeners.remove(observer);
            }
        };
    }

    private void update(Optional<EditorSelection> updated) {
        if (selection.equals(updated)) {
            return;
        }
        selection = updated;
        List.copyOf(listeners).forEach(listener -> listener.accept(selection));
    }
}
