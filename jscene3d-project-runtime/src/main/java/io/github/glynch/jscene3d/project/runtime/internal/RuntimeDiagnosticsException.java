/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;

/** Internal terminal failure already represented as structured project diagnostics. */
public final class RuntimeDiagnosticsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Diagnostics returned to the runtime loader. */
    private final List<ProjectDiagnostic> diagnostics;

    /**
     * Creates one terminal structured failure.
     *
     * @param diagnostics non-empty diagnostics containing at least one error
     */
    public RuntimeDiagnosticsException(List<ProjectDiagnostic> diagnostics) {
        super("runtime composition produced structured diagnostics");
        this.diagnostics = List.copyOf(diagnostics);
        if (this.diagnostics.isEmpty()
                || this.diagnostics.stream()
                        .noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)) {
            throw new IllegalArgumentException("diagnostics must contain at least one error");
        }
    }

    /**
     * Returns the terminal diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }
}
