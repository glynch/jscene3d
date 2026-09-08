/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

import java.util.Objects;

/**
 * Structured problem discovered while constructing static map geometry.
 *
 * @param severity diagnostic severity
 * @param code stable diagnostic code
 * @param location source-relative problem location
 * @param message user-facing explanation
 */
public record DoomGeometryDiagnostic(Severity severity, String code, String location, String message) {
    /** Geometry diagnostic severity. */
    public enum Severity {
        /** The geometry cannot be presented safely. */
        ERROR,
        /** Geometry can be presented with a known omission or approximation. */
        WARNING
    }

    /** Creates a diagnostic. */
    public DoomGeometryDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }
}
