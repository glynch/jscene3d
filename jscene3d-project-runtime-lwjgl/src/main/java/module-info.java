/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** LWJGL render-host adapter for native 3d project runtimes. */
module io.github.glynch.jscene3d.project.runtime.lwjgl {
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires transitive io.github.glynch.jscene3d.project.runtime.scene3d;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.runtime.lwjgl;
}
