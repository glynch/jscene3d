/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns observer registration and publication for mutable workbench layout state. */
final class EditorWorkbenchLayoutObservers {
    private final List<Consumer<List<EditorViewPlacement>>> viewObservers = new ArrayList<>();
    private final List<Consumer<EditorWorkbenchLayoutState>> stateObservers = new ArrayList<>();

    /** Registers a view-placement observer and immediately publishes the current snapshot. */
    EditorRegistration observeViews(Consumer<List<EditorViewPlacement>> observer, List<EditorViewPlacement> current) {
        Consumer<List<EditorViewPlacement>> listener = Objects.requireNonNull(observer, "observer");
        viewObservers.add(listener);
        listener.accept(current);
        return once(() -> viewObservers.remove(listener));
    }

    /** Registers a layout-state observer and immediately publishes the current state. */
    EditorRegistration observeState(Consumer<EditorWorkbenchLayoutState> observer, EditorWorkbenchLayoutState current) {
        Consumer<EditorWorkbenchLayoutState> listener = Objects.requireNonNull(observer, "observer");
        stateObservers.add(listener);
        listener.accept(current);
        return once(() -> stateObservers.remove(listener));
    }

    /** Publishes a stable placement snapshot to all current observers. */
    void publishViews(List<EditorViewPlacement> current) {
        List.copyOf(viewObservers).forEach(observer -> observer.accept(current));
    }

    /** Publishes a stable layout state to all current observers. */
    void publishState(EditorWorkbenchLayoutState current) {
        List.copyOf(stateObservers).forEach(observer -> observer.accept(current));
    }

    /** Discards all observer registrations during layout shutdown. */
    void clear() {
        viewObservers.clear();
        stateObservers.clear();
    }

    private static EditorRegistration once(Runnable removal) {
        return new EditorRegistration() {
            private boolean removed;

            @Override
            public void close() {
                if (!removed) {
                    removed = true;
                    removal.run();
                }
            }
        };
    }
}
