/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.diagnostic;

import io.github.glynch.jscene3d.doom.internal.Preconditions;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Stable source-aware problem discovered while decoding Doom content.
 *
 * @param severity diagnostic severity
 * @param code feature-owned stable code and fallback message
 * @param source normalized absolute WAD source path
 * @param location structural location within the Doom content
 * @param details immutable language-neutral values associated with the failure
 */
public record DoomDiagnostic(
        Severity severity, DoomDiagnosticCode code, Path source, String location, Map<String, String> details) {
    /** Diagnostic severity. */
    public enum Severity {
        /** The requested content cannot be used. */
        ERROR,

        /** The requested content remains usable but deserves attention. */
        WARNING
    }

    /** Creates a validated diagnostic. */
    public DoomDiagnostic {
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
