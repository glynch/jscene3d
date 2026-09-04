/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Collects ordered diagnostics for one manifest source. */
public final class DiagnosticCollector {
    private final URI source;
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();

    /**
     * Stores the normalized manifest source.
     *
     * @param source normalized absolute source path
     */
    public DiagnosticCollector(Path source) {
        this(source.toUri());
    }

    /**
     * Stores an absolute descriptor source.
     *
     * @param source absolute source URI
     */
    public DiagnosticCollector(URI source) {
        this.source = source;
    }

    /**
     * Adds an error.
     *
     * @param code feature-owned diagnostic code
     * @param location JSON Pointer location
     */
    public void error(DiagnosticCode code, String location) {
        error(code, location, Map.of());
    }

    /**
     * Adds an error with language-neutral details.
     *
     * @param code feature-owned diagnostic code
     * @param location JSON Pointer location
     * @param details values associated with the failure
     */
    public void error(DiagnosticCode code, String location, Map<String, String> details) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, source, location, details));
    }

    /**
     * Adds an error while preserving an implementation-supplied technical explanation.
     *
     * @param code feature-owned diagnostic code
     * @param technicalDetail non-localized technical context
     * @param location JSON Pointer location
     */
    public void error(DiagnosticCode code, String technicalDetail, String location) {
        error(code, location, Map.of("technicalDetail", technicalDetail));
    }

    /**
     * Adds a warning.
     *
     * @param code feature-owned diagnostic code
     * @param location JSON Pointer location
     */
    public void warning(DiagnosticCode code, String location) {
        warning(code, location, Map.of());
    }

    /**
     * Adds a warning with language-neutral details.
     *
     * @param code feature-owned diagnostic code
     * @param location JSON Pointer location
     * @param details values associated with the warning
     */
    public void warning(DiagnosticCode code, String location, Map<String, String> details) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.WARNING, code, source, location, details));
    }

    /**
     * Adds a warning while preserving an implementation-supplied technical explanation.
     *
     * @param code feature-owned diagnostic code
     * @param technicalDetail non-localized technical context
     * @param location JSON Pointer location
     */
    public void warning(DiagnosticCode code, String technicalDetail, String location) {
        warning(code, location, Map.of("technicalDetail", technicalDetail));
    }

    /**
     * Appends existing diagnostics in their supplied order.
     *
     * @param values diagnostics to append
     */
    public void addAll(List<ProjectDiagnostic> values) {
        diagnostics.addAll(List.copyOf(values));
    }

    /**
     * Returns whether any error prevents a validated project.
     *
     * @return {@code true} when an error has been collected
     */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }

    /**
     * Copies all diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }
}
