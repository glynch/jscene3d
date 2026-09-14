/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

/** Terminal outcome reported by one project-build execution. */
public enum ProjectBuildOutcome {
    /** The requested saved revision built successfully. */
    SUCCEEDED,
    /** The build completed without producing valid output. */
    FAILED,
    /** The build stopped in response to cancellation. */
    CANCELLED
}
