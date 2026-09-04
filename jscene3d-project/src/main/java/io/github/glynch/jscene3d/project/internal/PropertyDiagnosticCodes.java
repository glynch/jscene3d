/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.util.Objects;

/**
 * Typed diagnostic codes used by shared registered-property validation.
 *
 * @param required missing required property code
 * @param unknown undeclared property code
 * @param value invalid property value code
 */
public record PropertyDiagnosticCodes(DiagnosticCode required, DiagnosticCode unknown, DiagnosticCode value) {
    /** Creates a complete property-code set. */
    public PropertyDiagnosticCodes {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(unknown, "unknown");
        Objects.requireNonNull(value, "value");
    }
}
