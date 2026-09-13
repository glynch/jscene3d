/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorTreeSelectionModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Observable semantic selection owned by the Workspace Explorer. */
final class ExplorerSelectionModel implements EditorTreeSelectionModel<WorkspaceExplorerEntry> {
    private final List<Consumer<Optional<WorkspaceExplorerEntry>>> observers = new ArrayList<>();
    private Optional<WorkspaceExplorerEntry> selection = Optional.empty();

    @Override
    public Optional<WorkspaceExplorerEntry> selection() {
        return selection;
    }

    @Override
    public void select(Optional<WorkspaceExplorerEntry> selected) {
        Optional<WorkspaceExplorerEntry> replacement = Objects.requireNonNull(selected, "selection");
        if (!selection.equals(replacement)) {
            selection = replacement;
            List.copyOf(observers).forEach(observer -> observer.accept(selection));
        }
    }

    @Override
    public EditorRegistration observe(Consumer<Optional<WorkspaceExplorerEntry>> listener) {
        Consumer<Optional<WorkspaceExplorerEntry>> observer = Objects.requireNonNull(listener, "listener");
        observers.add(observer);
        observer.accept(selection);
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (active.compareAndSet(true, false)) {
                observers.remove(observer);
            }
        };
    }
}
