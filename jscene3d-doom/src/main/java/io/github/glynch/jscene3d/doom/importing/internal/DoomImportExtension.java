/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import java.util.Objects;

/** Registers classic Doom map inspection and import with the project import system. */
public final class DoomImportExtension implements ProjectImportExtension {
    static final String EXTENSION_IDENTIFIER = "io.github.glynch.jscene3d.doom";
    static final String IMPORTER_IDENTIFIER = "io.github.glynch.jscene3d.doom/maps";
    static final String MAP_RESOURCE_TYPE_IDENTIFIER = "io.github.glynch.jscene3d.doom/map";
    static final int TYPE_VERSION = 1;

    /** Creates the stateless service-discovered Doom import extension. */
    public DoomImportExtension() {
        // Public construction is required by ServiceLoader on the class path.
    }

    @Override
    public String id() {
        return EXTENSION_IDENTIFIER;
    }

    @Override
    public void register(ProjectImportRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .registerImporter(new RegisteredType(IMPORTER_IDENTIFIER, TYPE_VERSION), new DoomProjectImporter());
    }
}
