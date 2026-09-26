/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Diagnostic construction shared by built-in type-owned property rules. */
public final class DomainRuleDiagnostics {
    /** Prevents construction of this stateless helper. */
    private DomainRuleDiagnostics() {
        throw new AssertionError("DomainRuleDiagnostics cannot be instantiated");
    }

    /**
     * Creates one type-owned domain error.
     *
     * @param source source document containing the invalid property
     * @param location JSON pointer for the invalid property
     * @param propertyId invalid property identifier
     * @param message stable technical explanation of the domain rule
     * @return the resulting error diagnostic
     */
    public static ProjectDiagnostic error(URI source, String location, String propertyId, String message) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                PropertyValidationDiagnosticCode.DOMAIN,
                source,
                location,
                List.of(propertyId, message),
                Map.of("technicalDetail", message));
    }
}
