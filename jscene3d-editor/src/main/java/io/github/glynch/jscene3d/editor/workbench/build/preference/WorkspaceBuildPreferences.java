/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.preference;

import java.nio.file.Path;

/** Stores developer-specific build choices independently for each workspace. */
public interface WorkspaceBuildPreferences {
    /**
     * Returns whether automatic building is enabled for a workspace.
     *
     * @param workspaceRoot normalized project workspace root
     * @return stored choice, defaulting to {@code true}
     */
    boolean automaticBuild(Path workspaceRoot);

    /**
     * Persists whether automatic building is enabled for a workspace.
     *
     * @param workspaceRoot normalized project workspace root
     * @param enabled selected automatic-build state
     */
    void saveAutomaticBuild(Path workspaceRoot, boolean enabled);
}
