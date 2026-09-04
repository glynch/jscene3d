/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Project import adapter for opaque WAD archives and lumps. */
module io.github.glynch.jscene3d.wad.importing {
    requires transitive io.github.glynch.jscene3d.project.importing;
    requires io.github.glynch.jscene3d.wad;
    requires com.fasterxml.jackson.core;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.wad.importing;

    provides io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension with
            io.github.glynch.jscene3d.wad.importing.WadImportExtension;
}
