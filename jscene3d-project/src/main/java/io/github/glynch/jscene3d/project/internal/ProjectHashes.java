/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

/** Recognizes content-hash representations used by project descriptors. */
public final class ProjectHashes {
    private ProjectHashes() {
        throw new AssertionError("No instances");
    }

    /**
     * Returns whether a value contains exactly 64 ASCII hexadecimal digits.
     *
     * @param value candidate SHA-256 text
     * @return {@code true} when the value is a SHA-256 hexadecimal representation
     */
    public static boolean isSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isAsciiDigit(character)
                    && (character < 'a' || character > 'f')
                    && (character < 'A' || character > 'F')) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether a character is an ASCII decimal digit. */
    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
