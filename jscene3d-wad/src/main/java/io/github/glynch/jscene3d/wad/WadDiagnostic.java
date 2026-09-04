/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Stable source-aware problem discovered while opening a WAD archive.
 *
 * @param severity diagnostic severity
 * @param code feature-owned stable code and fallback message
 * @param source normalized absolute source path
 * @param location structural location within the archive
 * @param details immutable language-neutral values associated with the failure
 */
public record WadDiagnostic(
        Severity severity, WadDiagnosticCode code, Path source, String location, Map<String, String> details) {
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
        Objects.requireNonNull(code, "code");
        source = Preconditions.requireAbsoluteNormalized(source, "source");
        Objects.requireNonNull(location, "location");
        details = Map.copyOf(details);
    }

    /**
     * Returns the English fallback text supplied by the diagnostic code.
     *
     * @return non-blank fallback message
     */
    public String message() {
        return code.defaultMessage();
    }
}
