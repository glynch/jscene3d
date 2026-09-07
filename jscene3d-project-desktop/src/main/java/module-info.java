/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Standard native desktop host for composed JScene3D game projects. */
module io.github.glynch.jscene3d.project.desktop {
    requires io.github.glynch.jscene3d.game;
    requires io.github.glynch.jscene3d.lwjgl;
    requires transitive io.github.glynch.jscene3d.project;
    requires io.github.glynch.jscene3d.project.importing;
    requires io.github.glynch.jscene3d.project.physics3d;
    requires transitive io.github.glynch.jscene3d.project.runtime;
    requires io.github.glynch.jscene3d.project.spatial3d;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.desktop;
}
