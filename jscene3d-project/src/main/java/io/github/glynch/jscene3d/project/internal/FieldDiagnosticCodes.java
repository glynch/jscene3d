/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.util.Objects;

/**
 * Typed diagnostic codes used by shared authored-field validation.
 *
 * @param required missing required value code
 * @param blank blank optional value code
 * @param identifier invalid portable identifier code
 * @param type invalid registered-type identifier code
 */
public record FieldDiagnosticCodes(
        DiagnosticCode required, DiagnosticCode blank, DiagnosticCode identifier, DiagnosticCode type) {
    /** Creates a complete field-code set. */
    public FieldDiagnosticCodes {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(blank, "blank");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(type, "type");
    }
}
