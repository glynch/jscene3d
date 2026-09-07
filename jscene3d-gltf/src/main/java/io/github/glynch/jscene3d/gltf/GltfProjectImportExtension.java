/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import java.util.List;
import java.util.Objects;

/** Registers glTF 2.0 scene publication with the project import system. */
public final class GltfProjectImportExtension implements ProjectImportExtension {
    /** Safe extension descriptor identity implemented by this provider. */
    public static final String EXTENSION_IDENTIFIER = "io.github.glynch.jscene3d.gltf";

    /** Registered project importer identity. */
    public static final String IMPORTER_IDENTIFIER = "io.github.glynch.jscene3d.gltf/scene";

    private static final RegisteredType IMPORTER_TYPE = new RegisteredType(IMPORTER_IDENTIFIER, 1);
    private static final ExtensionDescriptor DESCRIPTOR = new ExtensionDescriptor(
            EXTENSION_IDENTIFIER,
            "0.1.0-SNAPSHOT",
            ">=0.1.0-SNAPSHOT <0.2.0",
            DescriptorPresentation.named("JScene3D glTF Import"),
            List.of(new RegisteredTypeDescriptor(
                    IMPORTER_TYPE,
                    RegisteredTypeScope.IMPORTER,
                    DescriptorPresentation.named("glTF Scene"),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of())));

    /** Creates the stateless trusted extension provider. */
    public GltfProjectImportExtension() {
        // Public construction supports hosts that supply trusted extensions explicitly.
    }

    /**
     * Returns safe metadata for hosts that supply this trusted build-time extension explicitly.
     *
     * @return immutable glTF import descriptor
     */
    public static ExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public String id() {
        return EXTENSION_IDENTIFIER;
    }

    @Override
    public void register(ProjectImportRegistry registry) {
        Objects.requireNonNull(registry, "registry").registerImporter(IMPORTER_TYPE, new GltfProjectImporter());
    }
}
