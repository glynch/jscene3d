/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.util.Objects;

/**
 * Typed diagnostic codes used by shared project-confined path validation.
 *
 * @param fields shared authored-field codes
 * @param portable non-portable path code
 * @param invalid syntactically invalid path code
 * @param absolute absolute path code
 * @param escape project-directory escape code
 * @param missing missing path code
 * @param read unreadable path code
 */
public record PathDiagnosticCodes(
        FieldDiagnosticCodes fields,
        DiagnosticCode portable,
        DiagnosticCode invalid,
        DiagnosticCode absolute,
        DiagnosticCode escape,
        DiagnosticCode missing,
        DiagnosticCode read) {
    /** Creates a complete path-code set. */
    public PathDiagnosticCodes {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(portable, "portable");
        Objects.requireNonNull(invalid, "invalid");
        Objects.requireNonNull(absolute, "absolute");
        Objects.requireNonNull(escape, "escape");
        Objects.requireNonNull(missing, "missing");
        Objects.requireNonNull(read, "read");
    }
}
