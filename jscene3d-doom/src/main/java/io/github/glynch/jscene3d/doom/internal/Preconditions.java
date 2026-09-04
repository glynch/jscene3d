/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.internal;

import java.nio.file.Path;
import java.util.Objects;

/** Shared argument and persistent-value validation for the Doom artifact. */
public final class Preconditions {
    /** Prevents instantiation of this validation policy. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Returns text after requiring at least one non-whitespace character.
     *
     * @param value text to validate
     * @param name argument name used in failures
     * @return validated text
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /**
     * Returns one normalized absolute path.
     *
     * @param value path to validate
     * @param name argument name used in failures
     * @return validated path
     */
    public static Path requireAbsoluteNormalized(Path value, String name) {
        Path validValue = Objects.requireNonNull(value, name);
        if (!validValue.isAbsolute() || !validValue.equals(validValue.normalize())) {
            throw new IllegalArgumentException(name + " must be a normalized absolute path: " + validValue);
        }
        return validValue;
    }
}
