/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session.internal;

import io.github.glynch.jscene3d.editor.project.session.AuthoringChange;
import io.github.glynch.jscene3d.editor.project.session.AuthoringSubscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Small synchronous publisher used internally by authoring sessions.
 *
 * @param <T> immutable change value type
 */
public final class AuthoringChangeSource<T> implements AuthoringChange<T>, AutoCloseable {
    private final List<Consumer<? super T>> listeners = new ArrayList<>();
    private boolean closed;

    /** Creates an open change source with no listeners. */
    public AuthoringChangeSource() {
        closed = false;
    }

    @Override
    public AuthoringSubscription subscribe(Consumer<? super T> listener) {
        Consumer<? super T> validListener = Objects.requireNonNull(listener, "listener");
        if (closed) {
            throw new IllegalStateException("Authoring change source is closed");
        }
        listeners.add(validListener);
        return new Registration(validListener);
    }

    /** Publishes one immutable value to a stable listener snapshot.
     *
     * @param value change value
     */
    public void emit(T value) {
        if (closed) {
            return;
        }
        for (Consumer<? super T> listener : List.copyOf(listeners)) {
            listener.accept(value);
        }
    }

    /** Removes all listeners and prevents new subscriptions. */
    @Override
    public void close() {
        closed = true;
        listeners.clear();
    }

    /** Idempotent subscription removal. */
    private final class Registration implements AuthoringSubscription {
        private final Consumer<? super T> listener;
        private boolean registered = true;

        private Registration(Consumer<? super T> listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            if (registered) {
                listeners.remove(listener);
                registered = false;
            }
        }
    }
}
