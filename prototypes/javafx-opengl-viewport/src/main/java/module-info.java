/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Throwaway named module proving the JavaFX/OpenGLFX editor-host boundary. */
module io.github.glynch.jscene3d.prototype.javafx {
    requires io.github.glynch.jscene3d.lwjgl;
    requires grapl.natives.core.macos;
    requires grapl.natives.gl.macos;
    requires javafx.controls;
    requires openglfx;
    requires openglfx.lwjgl;
    requires openglfx.natives.core.macos;
    requires org.lwjgl.natives;
    requires org.lwjgl.opengl;
    requires org.lwjgl.opengl.natives;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.prototype.javafx to
            javafx.graphics;
}
