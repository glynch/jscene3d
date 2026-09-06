/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import java.util.Objects;

/** Registers glTF 2.0 scene publication with the project import system. */
public final class GltfProjectImportExtension implements ProjectImportExtension {
    /** Safe extension descriptor identity implemented by this provider. */
    public static final String EXTENSION_IDENTIFIER = "io.github.glynch.jscene3d.gltf";

    /** Registered project importer identity. */
    public static final String IMPORTER_IDENTIFIER = "io.github.glynch.jscene3d.gltf/scene";

    /** Creates the stateless trusted extension provider. */
    public GltfProjectImportExtension() {
        // Public construction supports hosts that supply trusted extensions explicitly.
    }

    @Override
    public String id() {
        return EXTENSION_IDENTIFIER;
    }

    @Override
    public void register(ProjectImportRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .registerImporter(new RegisteredType(IMPORTER_IDENTIFIER, 1), new GltfProjectImporter());
    }
}
