/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.testing;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Controllable build-process adapter shared by build integration tests. */
public final class ControllableProjectBuildAdapter implements ProjectBuildAdapter {
    private final List<Execution> executions = new ArrayList<>();

    @Override
    public ProjectBuildExecution start(ProjectBuildRequest request) {
        Execution execution = new Execution(request);
        executions.add(execution);
        return execution;
    }

    /** Returns build requests in execution order. */
    public List<ProjectBuildRequest> requests() {
        return executions.stream().map(Execution::request).toList();
    }

    /** Reports whether the latest build received cancellation. */
    public boolean activeBuildWasCancelled() {
        return executions.getLast().cancelled;
    }

    /** Completes the latest build with the requested outcome. */
    public void completeActive(ProjectBuildOutcome outcome) {
        executions
                .getLast()
                .completion
                .complete(new ProjectBuildResult(outcome, List.of("fixture-build"), "", "", Duration.ZERO));
    }

    /** Completes the latest build exceptionally. */
    public void failActive(Throwable failure) {
        executions.getLast().completion.completeExceptionally(failure);
    }

    private static final class Execution implements ProjectBuildExecution {
        private final ProjectBuildRequest request;
        private final CompletableFuture<ProjectBuildResult> completion = new CompletableFuture<>();
        private boolean cancelled;

        private Execution(ProjectBuildRequest request) {
            this.request = request;
        }

        private ProjectBuildRequest request() {
            return request;
        }

        @Override
        public CompletableFuture<ProjectBuildResult> completion() {
            return completion;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
