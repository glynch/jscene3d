/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Genre-independent game lifecycle, semantic input, and scene-physics integration. */
module io.github.glynch.jscene3d.game {
    requires transitive io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.physics;
    requires transitive io.github.glynch.jscene3d.lwjgl;
    requires transitive org.joml;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.game;
    exports io.github.glynch.jscene3d.game.input;
    exports io.github.glynch.jscene3d.game.physics;
}
