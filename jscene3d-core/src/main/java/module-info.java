/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Core scene graph, geometry, material, math-facing, and camera APIs. */
module io.github.glynch.jscene3d.core {
    requires transitive org.joml;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.core;
}
