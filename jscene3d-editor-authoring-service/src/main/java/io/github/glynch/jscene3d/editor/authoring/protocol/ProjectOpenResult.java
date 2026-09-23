/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Domain outcome of one project-open request.
 *
 * @param opened whether a session was established
 * @param projectGeneration generation of the retained session, when opened
 * @param project opened project summary, when opened
 * @param diagnostics structured diagnostics produced by project loading
 * @param failureCode service-level rejection code, when loading was not attempted
 */
public record ProjectOpenResult(
        boolean opened,
        @Nullable Long projectGeneration,
        @Nullable ProjectSummary project,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Copies diagnostics and validates success/failure shape. */
    public ProjectOpenResult {
        diagnostics = List.copyOf(diagnostics);
        if ((opened && (projectGeneration == null || project == null))
                || (!opened && (projectGeneration != null || project != null))) {
            throw new IllegalArgumentException("Successful project open requires a generation and summary");
        }
    }
}
