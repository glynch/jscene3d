/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Product renderer process for native JScene3D Editor viewports. */
module io.github.glynch.jscene3d.editor.renderer {
    requires com.fasterxml.jackson.databind;
    requires io.github.glynch.jscene3d.core;
    requires io.github.glynch.jscene3d.editor.authoring;
    requires io.github.glynch.jscene3d.iosurface.macos;
    requires io.github.glynch.jscene3d.game;
    requires io.github.glynch.jscene3d.project.desktop;
    requires io.github.glynch.jscene3d.project.runtime;
    requires io.github.glynch.jscene3d.project.spatial3d;
    requires org.lwjgl.opengl;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.renderer.protocol;
}
