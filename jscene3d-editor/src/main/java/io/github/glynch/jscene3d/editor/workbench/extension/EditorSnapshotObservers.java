/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns synchronous observers for one immutable snapshot type. */
final class EditorSnapshotObservers<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();

    EditorRegistration observe(Consumer<T> observer, T initialSnapshot) {
        Consumer<T> listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        listener.accept(Objects.requireNonNull(initialSnapshot, "initialSnapshot"));
        return EditorRegistrationOnce.of(() -> observers.remove(listener));
    }

    void publish(T snapshot) {
        T current = Objects.requireNonNull(snapshot, "snapshot");
        List.copyOf(observers).forEach(observer -> observer.accept(current));
    }

    void clear() {
        observers.clear();
    }
}
