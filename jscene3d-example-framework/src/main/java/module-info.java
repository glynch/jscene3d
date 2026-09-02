/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Shared native hosting, browsing, and capture support for JScene3D example suites. */
module io.github.glynch.jscene3d.examples.framework {
    requires transitive io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires transitive io.github.glynch.jscene3d.gui;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.examples.framework;
}
