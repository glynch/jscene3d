/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable result of loading one executable project runtime. */
public final class ProjectRuntimeLoadResult {
    private final Optional<ProjectRuntime> runtime;
    private final List<ProjectDiagnostic> diagnostics;

    /** Stores one internally validated result. */
    private ProjectRuntimeLoadResult(Optional<ProjectRuntime> runtime, List<ProjectDiagnostic> diagnostics) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (runtime.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a runtime must be present exactly when diagnostics contain no errors");
        }
    }

    /** Creates a successful result, optionally carrying warnings. */
    static ProjectRuntimeLoadResult success(ProjectRuntime runtime, List<ProjectDiagnostic> diagnostics) {
        return new ProjectRuntimeLoadResult(Optional.of(runtime), diagnostics);
    }

    /** Creates a failed result. */
    static ProjectRuntimeLoadResult failure(List<ProjectDiagnostic> diagnostics) {
        return new ProjectRuntimeLoadResult(Optional.empty(), diagnostics);
    }

    /**
     * Returns the composed runtime when loading succeeded.
     *
     * @return composed runtime, or empty after a terminal diagnostic
     */
    public Optional<ProjectRuntime> runtime() {
        return runtime;
    }

    /**
     * Returns ordered loading, validation, and composition diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a composed runtime is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isOpen() {
        return runtime.isPresent();
    }
}
