/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Native JavaFX visual editor for JScene3D projects. */
module io.github.glynch.jscene3d.editor {
    requires io.github.glynch.jscene3d.editor.api;
    requires io.github.glynch.jscene3d.game;
    requires io.github.glynch.jscene3d.lwjgl;
    requires io.github.glynch.jscene3d.project.importing;
    requires io.github.glynch.jscene3d.project.physics3d;
    requires io.github.glynch.jscene3d.project.spatial3d;
    requires grapl.natives.core.macos;
    requires grapl.natives.gl.macos;
    requires java.logging;
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires openglfx;
    requires openglfx.lwjgl;
    requires openglfx.natives.core.macos;
    requires org.lwjgl.natives;
    requires org.lwjgl.opengl;
    requires org.lwjgl.opengl.natives;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor to
            javafx.graphics;
}
