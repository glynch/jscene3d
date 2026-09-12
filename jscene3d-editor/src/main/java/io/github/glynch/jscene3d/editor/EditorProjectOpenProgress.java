/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.nio.file.Path;

/** Presents the complete user-visible lifecycle of one project-opening operation. */
public interface EditorProjectOpenProgress extends EditorProjectLoadProgress {
    /**
     * Begins presentation for a normalized project directory.
     *
     * @param projectDirectory project directory being opened
     */
    void opening(Path projectDirectory);

    /** Finishes the presentation without deciding whether opening succeeded or failed. */
    void finish();
}
