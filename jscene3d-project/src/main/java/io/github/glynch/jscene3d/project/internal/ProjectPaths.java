/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Internal policy for normalized absolute paths stored by project model values. */
public final class ProjectPaths {
    /** Prevents construction of this utility class. */
    private ProjectPaths() {}

    /**
     * Requires a normalized absolute path.
     *
     * @param path path to validate
     * @param name parameter name used in failure messages
     * @return validated path
     */
    public static Path requireNormalizedAbsolute(Path path, String name) {
        Path validPath = Objects.requireNonNull(path, name);
        if (!validPath.isAbsolute() || !validPath.equals(validPath.normalize())) {
            throw new IllegalArgumentException(name + " must be a normalized absolute path: " + validPath);
        }
        return validPath;
    }

    /**
     * Requires an optional path to contain a normalized absolute path when present.
     *
     * @param value optional path to validate
     * @param name parameter name used in failure messages
     * @return validated optional path
     */
    public static Optional<Path> requireOptionalNormalizedAbsolute(Optional<Path> value, String name) {
        Optional<Path> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(path -> requireNormalizedAbsolute(path, name));
        return validValue;
    }

    /**
     * Returns an immutable copy containing only normalized absolute paths.
     *
     * @param values paths to validate and copy
     * @param name parameter name used in failure messages
     * @return immutable validated paths
     */
    public static List<Path> immutableNormalizedAbsolutePaths(List<Path> values, String name) {
        Objects.requireNonNull(values, name);
        List<Path> copied = new ArrayList<>(values.size());
        for (Path value : values) {
            copied.add(requireNormalizedAbsolute(value, name + " entry"));
        }
        return List.copyOf(copied);
    }
}
