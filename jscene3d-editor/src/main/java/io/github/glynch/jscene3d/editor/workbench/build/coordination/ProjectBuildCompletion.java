/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable terminal event for one project-build request.
 *
 * @param request completed build request
 * @param result normal adapter result, when execution completed normally
 * @param failure exceptional adapter failure, when execution did not complete normally
 */
public record ProjectBuildCompletion(
        ProjectBuildRequest request, Optional<ProjectBuildResult> result, Optional<Throwable> failure) {
    /** Validates that exactly one terminal representation is present. */
    public ProjectBuildCompletion {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(failure, "failure");
        if (result.isPresent() == failure.isPresent()) {
            throw new IllegalArgumentException("exactly one of result or failure must be present");
        }
    }

    /**
     * Creates a completion backed by a normal adapter result.
     *
     * @param request completed build request
     * @param result terminal adapter result
     * @return normal build completion
     */
    public static ProjectBuildCompletion completed(ProjectBuildRequest request, ProjectBuildResult result) {
        return new ProjectBuildCompletion(
                Objects.requireNonNull(request, "request"),
                Optional.of(Objects.requireNonNull(result, "result")),
                Optional.empty());
    }

    /**
     * Creates a completion backed by an exceptional adapter failure.
     *
     * @param request failed build request
     * @param failure exceptional adapter failure
     * @return exceptional build completion
     */
    public static ProjectBuildCompletion failed(ProjectBuildRequest request, Throwable failure) {
        return new ProjectBuildCompletion(
                Objects.requireNonNull(request, "request"),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }
}
