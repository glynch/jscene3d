/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.internal;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Shared argument and persistent-value validation for the WAD artifact. */
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

    /**
     * Returns one non-negative integer.
     *
     * @param value integer to validate
     * @param name argument name used in failures
     * @return validated integer
     */
    public static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    /**
     * Returns one non-negative long.
     *
     * @param value long value to validate
     * @param name argument name used in failures
     * @return validated long value
     */
    public static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    /**
     * Returns a lowercase 64-character hexadecimal SHA-256 value.
     *
     * @param value fingerprint to validate
     * @param name argument name used in failures
     * @return normalized fingerprint
     */
    public static String requireSha256(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.length() != 64 || !isHexadecimal(validValue)) {
            throw new IllegalArgumentException(name + " must be a 64-character hexadecimal SHA-256 value");
        }
        return validValue.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a normalized printable ASCII WAD name of at most eight characters.
     *
     * @param value name to validate
     * @param name argument name used in failures
     * @return uppercase normalized name
     */
    public static String requireLumpName(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.length() > 8) {
            throw new IllegalArgumentException(name + " must contain at most eight characters");
        }
        for (int index = 0; index < validValue.length(); index++) {
            char character = validValue.charAt(index);
            if (character < 0x20 || character > 0x7e) {
                throw new IllegalArgumentException(name + " must contain only printable ASCII characters");
            }
        }
        return validValue.toUpperCase(Locale.ROOT);
    }

    /** Reports whether every character is an ASCII hexadecimal digit. */
    private static boolean isHexadecimal(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean digit = character >= '0' && character <= '9';
            boolean lower = character >= 'a' && character <= 'f';
            boolean upper = character >= 'A' && character <= 'F';
            if (!digit && !lower && !upper) {
                return false;
            }
        }
        return true;
    }
}
