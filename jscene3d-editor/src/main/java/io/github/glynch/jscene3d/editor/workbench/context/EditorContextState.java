/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.context;

import io.github.glynch.jscene3d.editor.context.EditorContextKey;
import io.github.glynch.jscene3d.editor.context.EditorContextValues;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Mutable editor-owned state behind the read-only typed context interface. */
public final class EditorContextState implements EditorContextValues {
    private final Map<EditorContextKey<?>, Object> values = new LinkedHashMap<>();

    /** Creates an empty context state. */
    public EditorContextState() {
        super();
    }

    /**
     * Replaces one typed context value.
     *
     * @param key typed context identity
     * @param value replacement value
     * @param <T> context value type
     * @return whether the effective value changed
     */
    public <T> boolean set(EditorContextKey<T> key, T value) {
        EditorContextKey<T> identity = Objects.requireNonNull(key, "key");
        Object replacement = Objects.requireNonNull(value, "value");
        if (!identity.valueClass().isInstance(replacement)) {
            throw new IllegalArgumentException(
                    "value must be a " + identity.valueClass().getName());
        }
        return !Objects.equals(values.put(identity, replacement), replacement);
    }

    @Override
    public <T> Optional<T> get(EditorContextKey<T> key) {
        EditorContextKey<T> identity = Objects.requireNonNull(key, "key");
        return Optional.ofNullable(values.get(identity)).map(identity.valueClass()::cast);
    }
}
