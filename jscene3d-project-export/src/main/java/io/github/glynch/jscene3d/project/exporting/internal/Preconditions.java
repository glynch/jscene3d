/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Shared public-input checks owned by the project-export artifact. */
public final class Preconditions {
    private static final Pattern LAUNCHER_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /** Prevents construction. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires non-blank text.
     *
     * @param value text to validate
     * @param name request-property name used in failures
     * @return the validated text
     */
    public static String requireNonBlank(String value, String name) {
        String valid = Objects.requireNonNull(value, name);
        if (valid.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return valid;
    }

    /**
     * Requires a portable launcher filename without path separators.
     *
     * @param value launcher filename to validate
     * @return the validated launcher filename
     */
    public static String requireLauncherName(String value) {
        String valid = requireNonBlank(value, "launcherName");
        if (!LAUNCHER_NAME.matcher(valid).matches() || valid.equals(".") || valid.equals("..")) {
            throw new IllegalArgumentException("launcherName must contain only portable filename characters: " + valid);
        }
        return valid;
    }

    /**
     * Converts one required path to normalized absolute form.
     *
     * @param value path to normalize
     * @param name request-property name used in failures
     * @return normalized absolute path
     */
    public static Path normalizeAbsolute(Path value, String name) {
        return Objects.requireNonNull(value, name).toAbsolutePath().normalize();
    }

    /**
     * Copies paths after converting each to normalized absolute form.
     *
     * @param values paths to copy
     * @param name request-property name used in failures
     * @return immutable normalized path list
     */
    public static List<Path> immutablePaths(List<Path> values, String name) {
        Objects.requireNonNull(values, name);
        List<Path> copied = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            copied.add(normalizeAbsolute(values.get(index), name + '[' + index + ']'));
        }
        return List.copyOf(copied);
    }

    /**
     * Copies command arguments while rejecting characters which could split generated scripts.
     *
     * @param values command arguments to copy
     * @param name request-property name used in failures
     * @return immutable validated command-argument list
     */
    public static List<String> immutableCommandArguments(List<String> values, String name) {
        Objects.requireNonNull(values, name);
        List<String> copied = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String value = Objects.requireNonNull(values.get(index), name + '[' + index + ']');
            if (value.indexOf('\0') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException(name + '[' + index + "] contains a prohibited control character");
            }
            copied.add(value);
        }
        return List.copyOf(copied);
    }
}
