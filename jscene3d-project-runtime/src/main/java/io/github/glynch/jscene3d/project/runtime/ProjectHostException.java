/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.io.Serial;
import java.util.List;

/** Terminal project-host failure retaining ordered structured diagnostics when available. */
public final class ProjectHostException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<ProjectDiagnostic> diagnostics;

    /**
     * Creates a failure without structured diagnostics.
     *
     * @param message actionable failure description
     */
    public ProjectHostException(String message) {
        super(message);
        diagnostics = List.of();
    }

    /**
     * Creates a failure retaining immutable structured diagnostics.
     *
     * @param message actionable failure description
     * @param diagnostics ordered diagnostics
     */
    public ProjectHostException(String message, List<ProjectDiagnostic> diagnostics) {
        super(message + ": " + List.copyOf(diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Creates a failure caused by application preparation or provider discovery.
     *
     * @param message actionable failure description
     * @param cause originating failure
     */
    public ProjectHostException(String message, Throwable cause) {
        super(message, cause);
        diagnostics = List.of();
    }

    /**
     * Returns ordered structured diagnostics supplied by project subsystems.
     *
     * @return immutable diagnostics, possibly empty
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }
}
