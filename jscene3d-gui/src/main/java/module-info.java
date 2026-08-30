/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Optional themed controls, monitors, and overlays for JScene3D. */
module io.github.glynch.jscene3d.gui {
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires org.lwjgl;
    requires org.lwjgl.stb;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.gui;
}
