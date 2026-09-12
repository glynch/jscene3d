/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lifecycle;

import java.util.function.Consumer;

/**
 * A typed stream of synchronous editor events.
 *
 * <p>Publishers own event delivery while subscribers own and close the returned registration.
 *
 * @param <T> event payload type
 */
@FunctionalInterface
public interface EditorEvent<T> {
    /**
     * Subscribes a listener until its registration is closed.
     *
     * @param listener listener invoked for each event
     * @return removable subscription
     */
    EditorRegistration subscribe(Consumer<? super T> listener);
}
