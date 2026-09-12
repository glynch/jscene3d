/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.context;

import java.util.Optional;

/** Read-only typed values used to evaluate declarative editor contribution conditions. */
@FunctionalInterface
public interface EditorContextValues {
    /**
     * Returns the current value of one context key.
     *
     * @param key typed context identity
     * @param <T> context value type
     * @return current value, or empty when the key has not been set
     */
    <T> Optional<T> get(EditorContextKey<T> key);
}
