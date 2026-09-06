/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;

/** Terminal structured failure to prepare a reusable definition for spawning. */
public final class EntityPreparationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Ordered preparation diagnostics containing at least one error. */
    private final transient List<ProjectDiagnostic> diagnostics;

    /**
     * Creates a preparation failure from ordered structured diagnostics.
     *
     * @param diagnostics non-empty diagnostics containing at least one error
     */
    public EntityPreparationException(List<ProjectDiagnostic> diagnostics) {
        super("entity-definition preparation failed");
        this.diagnostics = List.copyOf(diagnostics);
        if (this.diagnostics.isEmpty()
                || this.diagnostics.stream()
                        .noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)) {
            throw new IllegalArgumentException("diagnostics must contain at least one error");
        }
    }

    /**
     * Returns the terminal preparation diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }
}
