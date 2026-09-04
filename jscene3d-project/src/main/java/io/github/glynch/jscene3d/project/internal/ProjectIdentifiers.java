/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

/** Internal syntax policy for identifiers and portable locators in project data. */
public final class ProjectIdentifiers {
    /** Prevents construction of this utility class. */
    private ProjectIdentifiers() {}

    /**
     * Returns whether a value is a lowercase dotted extension or project identifier.
     *
     * @param value candidate identifier
     * @return {@code true} when the identifier is valid
     */
    public static boolean isProjectId(String value) {
        if (value.isEmpty() || !isAsciiLowercase(value.charAt(0))) {
            return false;
        }
        int segmentStart = 0;
        boolean foundDot = false;
        for (int index = 0; index <= value.length(); index++) {
            if (index == value.length() || value.charAt(index) == '.') {
                if (!isProjectIdSegment(value, segmentStart, index)) {
                    return false;
                }
                foundDot |= index < value.length();
                segmentStart = index + 1;
            }
        }
        return foundDot;
    }

    /**
     * Returns whether a value is a portable lowercase local identifier.
     *
     * @param value candidate identifier
     * @return {@code true} when the identifier is valid
     */
    public static boolean isLocalId(String value) {
        if (value.isEmpty()
                || !isAsciiAlphaNumeric(value.charAt(0))
                || !isAsciiAlphaNumeric(value.charAt(value.length() - 1))) {
            return false;
        }
        for (int index = 1; index < value.length() - 1; index++) {
            char character = value.charAt(index);
            if (!isAsciiAlphaNumeric(character) && character != '-') {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether a value is an extension-qualified registered type identifier.
     *
     * @param value candidate identifier
     * @return {@code true} when the identifier is valid
     */
    public static boolean isRegisteredTypeId(String value) {
        int separator = value.indexOf('/');
        return separator > 0
                && separator == value.lastIndexOf('/')
                && separator < value.length() - 1
                && isProjectId(value.substring(0, separator))
                && isLocalId(value.substring(separator + 1));
    }

    /**
     * Returns whether a value is a forward-slash relative locator without traversal.
     *
     * @param value candidate locator
     * @return {@code true} when the locator is portable and safe
     */
    public static boolean isPortableLocator(String value) {
        if (value.isBlank() || value.indexOf('\\') >= 0 || value.startsWith("/") || value.endsWith("/")) {
            return false;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether one dot-delimited project identifier segment is valid. */
    private static boolean isProjectIdSegment(String value, int start, int end) {
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
