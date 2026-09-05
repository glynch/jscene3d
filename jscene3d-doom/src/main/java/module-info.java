/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Renderer-independent classic Doom content decoding and project import. */
module io.github.glynch.jscene3d.doom {
    requires transitive io.github.glynch.jscene3d.wad;
    requires io.github.glynch.jscene3d.project.importing;
    requires io.github.glynch.jscene3d.project.runtime;
    requires io.github.glynch.jscene3d.project.runtime.scene3d;
    requires io.github.glynch.jscene3d.wad.importing;
    requires com.fasterxml.jackson.core;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.doom.diagnostic;
    exports io.github.glynch.jscene3d.doom.map;

    provides io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension with
            io.github.glynch.jscene3d.doom.importing.internal.DoomImportExtension;
    provides io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension with
            io.github.glynch.jscene3d.doom.runtime.internal.DoomRuntimeExtension;
}
