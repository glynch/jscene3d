/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.execution;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Complete terminal result of one project build-system execution. */
public record ProjectBuildResult(
        ProjectBuildOutcome outcome,
        List<String> command,
        String standardOutput,
        String standardError,
        Duration duration) {
    /** Validates and isolates the captured result. */
    public ProjectBuildResult {
        Objects.requireNonNull(outcome, "outcome");
        command = List.copyOf(Objects.requireNonNull(command, "command"));
        Objects.requireNonNull(standardOutput, "standardOutput");
        Objects.requireNonNull(standardError, "standardError");
        Objects.requireNonNull(duration, "duration");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
    }
}
