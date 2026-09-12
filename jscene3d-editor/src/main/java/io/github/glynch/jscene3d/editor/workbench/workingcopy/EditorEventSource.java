/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Owns listener mutation and event delivery without exposing an emit operation to subscribers. */
public final class EditorEventSource<T> implements EditorEvent<T> {
    private final List<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public EditorRegistration subscribe(Consumer<? super T> listener) {
        Consumer<? super T> subscriber = Objects.requireNonNull(listener, "listener");
        listeners.add(subscriber);
        AtomicBoolean registered = new AtomicBoolean(true);
        return () -> {
            if (registered.compareAndSet(true, false)) {
                listeners.remove(subscriber);
            }
        };
    }

    /** Delivers one non-null event to a stable listener snapshot. */
    public void emit(T event) {
        T payload = Objects.requireNonNull(event, "event");
        listeners.forEach(listener -> listener.accept(payload));
    }
}
