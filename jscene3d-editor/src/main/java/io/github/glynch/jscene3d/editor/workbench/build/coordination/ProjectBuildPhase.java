/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

/** Current lifecycle and freshness phase of one open project's saved content. */
public enum ProjectBuildPhase {
    /** No build result has established freshness during this project session. */
    UNKNOWN,
    /** The newest saved build-relevant revision has built successfully. */
    CURRENT,
    /** Saved build-relevant input is newer than the accepted successful result. */
    STALE,
    /** A build request exists but its adapter execution has not started. */
    QUEUED,
    /** The selected adapter is building one saved revision. */
    BUILDING,
    /** The latest applicable build completed unsuccessfully. */
    FAILED
}
