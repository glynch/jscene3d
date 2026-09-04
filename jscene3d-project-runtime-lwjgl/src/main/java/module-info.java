/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Built-in JScene3D project types rendered through the LWJGL backend. */
module io.github.glynch.jscene3d.project.runtime.lwjgl {
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires transitive io.github.glynch.jscene3d.project.runtime;
    requires org.joml;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.runtime.lwjgl;
}
