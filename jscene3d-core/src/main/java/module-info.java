/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Core scene graph, geometry, material, math-facing, and camera APIs. */
module io.github.glynch.jscene3d.core {
    requires transitive org.joml;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.animation;
    exports io.github.glynch.jscene3d.cameras;
    exports io.github.glynch.jscene3d.geometries;
    exports io.github.glynch.jscene3d.helpers;
    exports io.github.glynch.jscene3d.lights;
    exports io.github.glynch.jscene3d.materials;
    exports io.github.glynch.jscene3d.math;
    exports io.github.glynch.jscene3d.objects;
    exports io.github.glynch.jscene3d.raycasting;
    exports io.github.glynch.jscene3d.scenes;
    exports io.github.glynch.jscene3d.textures;
}
