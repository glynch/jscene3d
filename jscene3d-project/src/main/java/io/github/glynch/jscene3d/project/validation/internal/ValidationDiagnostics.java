/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Mutable source-local accumulator hidden behind the validation facade. */
final class ValidationDiagnostics {
    private final URI source;
    private final List<ProjectDiagnostic> values = new ArrayList<>();

    /** Stores the absolute source shared by emitted diagnostics. */
    ValidationDiagnostics(URI source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    /** Adds one structured error with ordered localization arguments. */
    void error(DiagnosticCode code, String location, String technicalDetail, Object... arguments) {
        values.add(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                source,
                location,
                List.of(arguments),
                Map.of("technicalDetail", technicalDetail)));
    }

    /** Appends diagnostics returned by a type-owned rule. */
    void addAll(List<ProjectDiagnostic> diagnostics) {
        values.addAll(diagnostics);
    }

    /** Returns an immutable snapshot in insertion order. */
    List<ProjectDiagnostic> values() {
        return List.copyOf(values);
    }
}
