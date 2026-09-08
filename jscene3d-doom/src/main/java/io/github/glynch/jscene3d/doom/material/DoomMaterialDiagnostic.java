/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.material;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Source-aware problem discovered while decoding Doom map materials.
 *
 * @param severity diagnostic severity
 * @param code stable material diagnostic code
 * @param source source archive path
 * @param location source-relative problem location
 * @param message user-facing explanation
 */
public record DoomMaterialDiagnostic(Severity severity, String code, Path source, String location, String message) {
    /** Diagnostic severity. */
    public enum Severity {
        /** The requested materials cannot be used. */
        ERROR,

        /** The requested materials remain usable but deserve attention. */
        WARNING
    }

    /** Creates a validated material diagnostic. */
    public DoomMaterialDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }
}
