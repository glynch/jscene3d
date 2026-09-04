/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Stable source-aware problem discovered while opening a WAD archive.
 *
 * @param severity diagnostic severity
 * @param code stable machine-readable code
 * @param source normalized absolute source path
 * @param location structural location within the archive
 * @param message actionable human-readable description
 */
public record WadDiagnostic(Severity severity, String code, Path source, String location, String message) {
    /** Diagnostic severity. */
    public enum Severity {
        /** The archive cannot be used. */
        ERROR,

        /** The archive remains usable but deserves attention. */
        WARNING
    }

    /** Creates a validated diagnostic. */
    public WadDiagnostic {
        Objects.requireNonNull(severity, "severity");
        code = Preconditions.requireNonBlank(code, "code");
        source = Preconditions.requireAbsoluteNormalized(source, "source");
        Objects.requireNonNull(location, "location");
        message = Preconditions.requireNonBlank(message, "message");
    }
}
