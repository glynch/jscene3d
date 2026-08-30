/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
module io.github.glynch.jscene3d.lwjgl {
    requires transitive io.github.glynch.jscene3d.core;
    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.controls;
    exports io.github.glynch.jscene3d.platform;
    exports io.github.glynch.jscene3d.render;
}
