/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.project.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Collects ordered diagnostics for one manifest source. */
final class DiagnosticCollector {
    private final Path source;
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();

    /** Stores the normalized manifest source. */
    DiagnosticCollector(Path source) {
        this.source = source;
    }

    /** Adds an error. */
    void error(String code, String message, String location) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source, location));
    }

    /** Adds a warning. */
    void warning(String code, String message, String location) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.WARNING, code, message, source, location));
    }

    /** Returns whether any error prevents a validated project. */
    boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }

    /** Copies all diagnostics. */
    List<ProjectDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }
}
