/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.execution;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import java.util.concurrent.CompletionStage;

/** One cancellable asynchronous build-system execution. */
public interface ProjectBuildExecution {
    /**
     * Returns the terminal outcome notification.
     *
     * @return asynchronous terminal outcome
     */
    CompletionStage<ProjectBuildOutcome> completion();

    /** Requests cancellation of the complete build process. */
    void cancel();
}
