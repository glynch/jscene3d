/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Renderer-independent three-dimensional collision objects, queries, and kinematic movement. */
module io.github.glynch.jscene3d.physics {
    requires transitive org.joml;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.physics;
    exports io.github.glynch.jscene3d.physics.debug;
    exports io.github.glynch.jscene3d.physics.movement;
    exports io.github.glynch.jscene3d.physics.queries;
    exports io.github.glynch.jscene3d.physics.shapes;
}
