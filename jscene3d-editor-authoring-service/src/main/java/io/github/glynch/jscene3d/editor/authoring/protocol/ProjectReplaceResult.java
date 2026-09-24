/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Domain outcome of one atomic project-replacement request.
 *
 * @param outcome {@link #REPLACED}, {@link #CANDIDATE_REJECTED}, or {@link #CONFLICT}
 * @param projectGeneration generation of the replacement session, when installed
 * @param project replacement project summary, when installed
 * @param diagnostics structured diagnostics produced while loading the candidate
 * @param failureCode service-level conflict code, when candidate loading was not committed
 */
public record ProjectReplaceResult(
        String outcome,
        @Nullable Long projectGeneration,
        @Nullable ProjectSummary project,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Outcome used when the candidate becomes the active project. */
    public static final String REPLACED = "replaced";

    /** Outcome used when normal project loading rejects the candidate. */
    public static final String CANDIDATE_REJECTED = "candidateRejected";

    /** Outcome used when the caller no longer owns the expected active generation. */
    public static final String CONFLICT = "conflict";

    /** Copies diagnostics and validates the outcome-specific response shape. */
    public ProjectReplaceResult {
        Objects.requireNonNull(outcome, "outcome");
        diagnostics = List.copyOf(diagnostics);
        boolean success = REPLACED.equals(outcome);
        boolean rejected = CANDIDATE_REJECTED.equals(outcome);
        boolean conflict = CONFLICT.equals(outcome);
        if (!success && !rejected && !conflict) {
            throw new IllegalArgumentException("Unknown project replacement outcome: " + outcome);
        }
        if (success && (projectGeneration == null || project == null || failureCode != null)) {
            throw new IllegalArgumentException("Successful replacement requires a generation and summary");
        }
        if (!success && (projectGeneration != null || project != null)) {
            throw new IllegalArgumentException("Uncommitted replacement cannot expose a replacement project");
        }
        if ((rejected && failureCode != null) || (conflict && failureCode == null)) {
            throw new IllegalArgumentException("Replacement failure details do not match its outcome");
        }
    }
}
