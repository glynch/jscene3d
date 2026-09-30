/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** macOS IOSurface-backed rendering for JScene3D. */
module io.github.glynch.jscene3d.iosurface.macos {
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.iosurface.macos;
}
