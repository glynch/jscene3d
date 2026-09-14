/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.execution;

import java.util.concurrent.CompletionStage;

/** One cancellable asynchronous build-system execution. */
public interface ProjectBuildExecution {
    /**
     * Returns the complete terminal build result.
     *
     * @return asynchronous terminal result
     */
    CompletionStage<ProjectBuildResult> completion();

    /** Requests cancellation of the complete build process. */
    void cancel();
}
