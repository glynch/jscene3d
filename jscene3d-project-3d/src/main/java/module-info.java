/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Descriptor-backed three-dimensional components for composed JScene3D worlds. */
module io.github.glynch.jscene3d.project.spatial3d {
    requires io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires transitive io.github.glynch.jscene3d.project.runtime;
    requires transitive org.joml;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.spatial3d;
}
