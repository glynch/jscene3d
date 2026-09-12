/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.context;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable, typed identity for one observable editor-context value.
 *
 * @param value lowercase dotted context identity
 * @param valueClass Java value type exposed to conditions
 * @param <T> context value type
 */
public record EditorContextKey<T>(String value, Class<T> valueClass) {
    private static final Pattern VALID_SEGMENT = Pattern.compile("[a-z](?!.*--)(?!.*-$)[a-z0-9-]*");

    /** Validates one stable typed context identity. */
    public EditorContextKey {
        if (!isValid(Objects.requireNonNull(value, "value"))) {
            throw new IllegalArgumentException("context key must be a lowercase dotted identity: " + value);
        }
        Objects.requireNonNull(valueClass, "valueClass");
    }

    private static boolean isValid(String candidate) {
        String[] segments = candidate.split("\\.", -1);
        return segments.length >= 2 && Arrays.stream(segments).allMatch(VALID_SEGMENT.asMatchPredicate());
    }
}
