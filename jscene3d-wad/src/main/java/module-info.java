/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Renderer-independent access to validated WAD archives and explicit archive layers. */
module io.github.glynch.jscene3d.wad {
    requires transitive io.github.glynch.jscene3d.core;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.wad;
}
