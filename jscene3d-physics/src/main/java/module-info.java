/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Renderer-independent three-dimensional collision queries and collider management. */
module io.github.glynch.jscene3d.physics {
    requires transitive org.joml;
    requires static transitive org.jspecify;

    exports io.github.glynch.jscene3d.physics;
    exports io.github.glynch.jscene3d.physics.queries;
    exports io.github.glynch.jscene3d.physics.shapes;
}
