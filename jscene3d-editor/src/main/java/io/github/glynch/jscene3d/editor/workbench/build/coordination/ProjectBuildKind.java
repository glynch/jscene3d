/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

/** Semantic build intent independent of a particular project build system. */
public enum ProjectBuildKind {
    /** Reuses valid build-system output while bringing the project current. */
    INCREMENTAL,
    /** Discards prior build-system output before building the project. */
    CLEAN
}
