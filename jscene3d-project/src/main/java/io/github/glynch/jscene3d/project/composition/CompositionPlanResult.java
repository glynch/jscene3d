/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable safe-planning outcome.
 *
 * @param plan complete plan exactly when no error diagnostics occurred
 * @param diagnostics ordered project diagnostics
 */
public record CompositionPlanResult(Optional<CompositionPlan> plan, List<ProjectDiagnostic> diagnostics) {
    /** Enforces the all-or-nothing first-generation planning contract. */
    public CompositionPlanResult {
        Objects.requireNonNull(plan, "plan");
        diagnostics = List.copyOf(diagnostics);
        boolean hasErrors =
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (plan.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a plan must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns whether a coherent plan is available.
     *
     * @return {@code true} when planning succeeded
     */
    public boolean isPlanned() {
        return plan.isPresent();
    }
}
