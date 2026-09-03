/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.nio.file.Path;
import java.util.Objects;

/** One structured project-loading message suitable for tools and graphical interfaces.
 *
 * @param severity diagnostic severity
 * @param code stable machine-readable diagnostic code
 * @param message human-readable explanation
 * @param source normalized absolute source file
 * @param location JSON Pointer location, or an empty string for the complete file
 */
public record ProjectDiagnostic(Severity severity, String code, String message, Path source, String location) {
    /** Validates diagnostic values. */
    public ProjectDiagnostic {
        Objects.requireNonNull(severity, "severity");
        requireText(code, "code");
        requireText(message, "message");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
    }

    /** Project-loading diagnostic severity. */
    public enum Severity {
        /** Project loading cannot produce a valid descriptor. */
        ERROR,
        /** Loading can continue, but the project needs attention. */
        WARNING
    }

    /** Requires a non-blank diagnostic string. */
    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
