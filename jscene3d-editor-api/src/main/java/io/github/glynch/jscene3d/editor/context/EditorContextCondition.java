/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.context;

import java.util.Objects;

/**
 * Declarative equality condition controlling whether an editor contribution is available.
 *
 * @param key typed context identity
 * @param expectedValue value required for availability
 * @param <T> context value type
 */
public record EditorContextCondition<T>(EditorContextKey<T> key, T expectedValue) {
    /** Validates one typed equality condition. */
    public EditorContextCondition {
        Objects.requireNonNull(key, "key");
        Object expected = Objects.requireNonNull(expectedValue, "expectedValue");
        if (!key.valueClass().isInstance(expected)) {
            throw new IllegalArgumentException(
                    "expectedValue must be a " + key.valueClass().getName());
        }
    }

    /** Creates a condition requiring a Boolean context key to be true. */
    public static EditorContextCondition<Boolean> isTrue(EditorContextKey<Boolean> key) {
        return new EditorContextCondition<>(Objects.requireNonNull(key, "key"), true);
    }

    /** Returns whether the current context value equals the required value. */
    public boolean matches(EditorContextValues values) {
        return Objects.requireNonNull(values, "values")
                .get(key)
                .filter(expectedValue::equals)
                .isPresent();
    }
}
