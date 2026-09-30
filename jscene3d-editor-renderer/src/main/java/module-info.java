/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Product renderer process for native JScene3D Editor viewports. */
module io.github.glynch.jscene3d.editor.renderer {
    requires io.github.glynch.jscene3d.iosurface.macos;
    requires org.lwjgl.opengl;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.renderer.protocol;
}
