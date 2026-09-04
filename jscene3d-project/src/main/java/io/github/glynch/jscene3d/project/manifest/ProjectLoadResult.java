/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of loading and validating one project. */
public final class ProjectLoadResult {
    private final Optional<GameProject> project;
    private final List<ProjectDiagnostic> diagnostics;

    /** Copies one loader result. */
    ProjectLoadResult(Optional<GameProject> project, List<ProjectDiagnostic> diagnostics) {
        this.project = Objects.requireNonNull(project, "project");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (project.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a project must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the validated project when loading succeeded.
     *
     * @return present project for a valid result
     */
    public Optional<GameProject> project() {
        return project;
    }

    /**
     * Returns ordered errors and warnings produced while loading.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a validated project is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isValid() {
        return project.isPresent();
    }
}
