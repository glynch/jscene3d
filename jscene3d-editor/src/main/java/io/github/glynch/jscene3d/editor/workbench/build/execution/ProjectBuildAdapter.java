/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.execution;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;

/** Starts build-system-specific executions for adapter-independent project requests. */
@FunctionalInterface
public interface ProjectBuildAdapter {
    /**
     * Starts one project build.
     *
     * @param request exact saved revision and semantic build intent
     * @return cancellable asynchronous execution
     */
    ProjectBuildExecution start(ProjectBuildRequest request);
}
