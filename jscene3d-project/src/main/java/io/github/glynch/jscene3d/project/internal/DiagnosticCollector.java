/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
     * @param code stable diagnostic code
     * @param message human-readable message
     * @param location JSON Pointer location
     */
    public void error(String code, String message, String location) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source, location));
    }

    /**
     * Adds a warning.
     *
     * @param code stable diagnostic code
     * @param message human-readable message
     * @param location JSON Pointer location
     */
    public void warning(String code, String message, String location) {
        diagnostics.add(new ProjectDiagnostic(ProjectDiagnostic.Severity.WARNING, code, message, source, location));
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
