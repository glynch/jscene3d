/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Trusted runtime composition for versioned JScene3D game projects. */
module io.github.glynch.jscene3d.project.runtime {
    requires transitive io.github.glynch.jscene3d.game;
    requires transitive io.github.glynch.jscene3d.project.importing;
    requires transitive io.github.glynch.jscene3d.project;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.runtime;
    exports io.github.glynch.jscene3d.project.runtime.extension;

    uses io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
}
