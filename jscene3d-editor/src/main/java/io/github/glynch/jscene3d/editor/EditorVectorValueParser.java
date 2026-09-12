/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Parses the compact numeric-vector representation accepted by the first Inspector editor. */
final class EditorVectorValueParser {
    private EditorVectorValueParser() {
        throw new AssertionError("EditorVectorValueParser cannot be instantiated");
    }

    /** Parses an exact-length bracketed numeric vector. */
    static ProjectValue.ArrayValue parse(String text, int size) {
        String value = Objects.requireNonNull(text, "text").trim();
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (value.length() < 2 || value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']') {
            throw invalid(size);
        }
        String body = value.substring(1, value.length() - 1);
        String[] parts = body.split(",", -1);
        if (parts.length != size) {
            throw invalid(size);
        }
        List<ProjectValue> numbers = new ArrayList<>(size);
        for (String part : parts) {
            try {
                numbers.add(new ProjectValue.NumberValue(new BigDecimal(part.trim())));
            } catch (NumberFormatException exception) {
                throw invalid(size, exception);
            }
        }
        return new ProjectValue.ArrayValue(numbers);
    }

    private static IllegalArgumentException invalid(int size) {
        return new IllegalArgumentException("value must be a bracketed vector containing " + size + " numbers");
    }

    private static IllegalArgumentException invalid(int size, NumberFormatException cause) {
        return new IllegalArgumentException("value must be a bracketed vector containing " + size + " numbers", cause);
    }
}
