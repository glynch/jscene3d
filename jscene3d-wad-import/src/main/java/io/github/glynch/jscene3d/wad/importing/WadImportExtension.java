/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import io.github.glynch.jscene3d.wad.importing.internal.WadProjectImporter;
import java.util.Objects;

/** Registers opaque WAD archive inspection and import with the project import system. */
public final class WadImportExtension implements ProjectImportExtension {
    /** Stable extension identity declared by the bundled extension descriptor. */
    public static final String EXTENSION_IDENTIFIER = "io.github.glynch.jscene3d.wad";

    /** Stable registered type identity for the WAD archive importer. */
    public static final String IMPORTER_IDENTIFIER = "io.github.glynch.jscene3d.wad/archive";

    /** Stable registered type identity for WAD source assets. */
    public static final String SOURCE_TYPE_IDENTIFIER = "io.github.glynch.jscene3d.wad/source";

    /** Creates the stateless WAD import extension. */
    public WadImportExtension() {
        // Public construction supports hosts that supply trusted extensions explicitly.
    }

    @Override
    public String id() {
        return EXTENSION_IDENTIFIER;
    }

    @Override
    public void register(ProjectImportRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .registerImporter(new RegisteredType(IMPORTER_IDENTIFIER, 1), new WadProjectImporter());
    }
}
