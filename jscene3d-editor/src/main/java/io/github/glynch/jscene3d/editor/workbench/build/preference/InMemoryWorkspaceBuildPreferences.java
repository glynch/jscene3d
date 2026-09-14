/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.preference;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Non-persistent workspace build preferences used by isolated workbenches and tests. */
public final class InMemoryWorkspaceBuildPreferences implements WorkspaceBuildPreferences {
    private final Map<Path, Boolean> automaticBuilds;

    /** Creates empty preferences which default automatic building to enabled. */
    public InMemoryWorkspaceBuildPreferences() {
        automaticBuilds = new HashMap<>();
    }

    @Override
    public boolean automaticBuild(Path workspaceRoot) {
        return automaticBuilds.getOrDefault(normalize(workspaceRoot), true);
    }

    @Override
    public void saveAutomaticBuild(Path workspaceRoot, boolean enabled) {
        automaticBuilds.put(normalize(workspaceRoot), enabled);
    }

    private static Path normalize(Path workspaceRoot) {
        return Objects.requireNonNull(workspaceRoot, "workspaceRoot")
                .toAbsolutePath()
                .normalize();
    }
}
