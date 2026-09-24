/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import java.util.function.Consumer;

/** Synchronous notification stream owned by one authoring session.
 *
 * @param <T> immutable change value type
 */
@FunctionalInterface
public interface AuthoringChange<T> {
    /**
     * Subscribes a listener until the returned subscription is closed.
     *
     * @param listener listener invoked synchronously after a change
     * @return removable subscription
     */
    AuthoringSubscription subscribe(Consumer<? super T> listener);
}
