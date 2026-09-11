/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.internal;

import java.util.Objects;

/** Internal syntax policy for editor contribution identities. */
public final class EditorIdentifiers {
    private EditorIdentifiers() {}

    /**
     * Returns a validated lowercase dotted namespaced identity.
     *
     * @param value candidate identity
     * @param name argument name used in validation failures
     * @return validated identity
     */
    public static String requireNamespacedId(String value, String name) {
        String required = Objects.requireNonNull(value, name);
        if (!isNamespacedId(required)) {
            throw new IllegalArgumentException(name + " must be a lowercase dotted namespaced identity");
        }
        return required;
    }

    /** Returns whether a value is a lowercase dotted namespaced identity. */
    private static boolean isNamespacedId(String value) {
        if (value.isEmpty() || !isAsciiLowercase(value.charAt(0))) {
            return false;
        }
        int segmentStart = 0;
        boolean foundDot = false;
        for (int index = 0; index <= value.length(); index++) {
            if (index == value.length() || value.charAt(index) == '.') {
                if (!isSegment(value, segmentStart, index)) {
                    return false;
                }
                foundDot |= index < value.length();
                segmentStart = index + 1;
            }
        }
        return foundDot;
    }

    /** Returns whether one dot-delimited segment is portable. */
    private static boolean isSegment(String value, int start, int end) {
        if (start >= end || !isAsciiAlphaNumeric(value.charAt(start)) || !isAsciiAlphaNumeric(value.charAt(end - 1))) {
            return false;
        }
        for (int index = start + 1; index < end - 1; index++) {
            char character = value.charAt(index);
            if (!isAsciiAlphaNumeric(character) && character != '-') {
                return false;
            }
        }
        return true;
    }

    /** Returns whether a character is an ASCII lowercase letter. */
    private static boolean isAsciiLowercase(char character) {
        return character >= 'a' && character <= 'z';
    }

    /** Returns whether a character is an ASCII lowercase letter or decimal digit. */
    private static boolean isAsciiAlphaNumeric(char character) {
        return isAsciiLowercase(character) || (character >= '0' && character <= '9');
    }
}
