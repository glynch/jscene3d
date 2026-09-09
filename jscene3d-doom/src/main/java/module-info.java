/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Renderer-independent classic Doom content decoding and project import. */
module io.github.glynch.jscene3d.doom {
    requires transitive io.github.glynch.jscene3d.wad;
    requires io.github.glynch.jscene3d.wad.importing;
    requires io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.project;
    requires io.github.glynch.jscene3d.project.importing;
    requires io.github.glynch.jscene3d.project.physics3d;
    requires io.github.glynch.jscene3d.project.runtime;
    requires io.github.glynch.jscene3d.project.spatial3d;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.doom.diagnostic;
    exports io.github.glynch.jscene3d.doom.geometry;
    exports io.github.glynch.jscene3d.doom.map;
    exports io.github.glynch.jscene3d.doom.material;
    exports io.github.glynch.jscene3d.doom.runtime;

    provides io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension with
            io.github.glynch.jscene3d.doom.importing.internal.DoomImportExtension;
    provides io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension with
            io.github.glynch.jscene3d.doom.runtime.internal.DoomRuntimeExtension;
}
