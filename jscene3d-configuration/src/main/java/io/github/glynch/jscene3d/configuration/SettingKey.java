/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable, typed identity used by feature code to read one declared setting.
 *
 * @param value lowercase dotted setting identity
 * @param valueClass Java value type exposed to callers
 * @param <T> setting value type
 */
public record SettingKey<T>(String value, Class<T> valueClass) {
    private static final Pattern VALID_SEGMENT = Pattern.compile("[a-z](?!.*--)(?!.*-$)[a-z0-9-]*");

    /** Validates one stable typed identity. */
    public SettingKey {
        if (!isValid(Objects.requireNonNull(value, "value"))) {
            throw new IllegalArgumentException("setting key must be a lowercase dotted identity: " + value);
        }
        Objects.requireNonNull(valueClass, "valueClass");
    }

    private static boolean isValid(String candidate) {
        String[] segments = candidate.split("\\.", -1);
        return segments.length >= 2 && Arrays.stream(segments).allMatch(VALID_SEGMENT.asMatchPredicate());
    }
}
