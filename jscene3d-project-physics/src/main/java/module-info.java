/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Descriptor-backed three-dimensional collision components for composed JScene3D worlds. */
module io.github.glynch.jscene3d.project.physics3d {
    requires io.github.glynch.jscene3d.physics;
    requires transitive io.github.glynch.jscene3d.project.spatial3d;
    requires transitive org.joml;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.physics3d;
}
