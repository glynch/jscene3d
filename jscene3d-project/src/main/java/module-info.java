/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Headless loading and validation of versioned JScene3D game projects. */
module io.github.glynch.jscene3d.project {
    requires transitive io.github.glynch.jscene3d.core;
    requires com.fasterxml.jackson.databind;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.diagnostic;
    exports io.github.glynch.jscene3d.project.extension;
    exports io.github.glynch.jscene3d.project.imports;
    exports io.github.glynch.jscene3d.project.manifest;
    exports io.github.glynch.jscene3d.project.resource;
    exports io.github.glynch.jscene3d.project.scene;
    exports io.github.glynch.jscene3d.project.value;

    opens io.github.glynch.jscene3d.project.extension.internal to
            com.fasterxml.jackson.databind;
    opens io.github.glynch.jscene3d.project.imports.internal to
            com.fasterxml.jackson.databind;
    opens io.github.glynch.jscene3d.project.manifest.internal to
            com.fasterxml.jackson.databind;
    opens io.github.glynch.jscene3d.project.resource.internal to
            com.fasterxml.jackson.databind;
    opens io.github.glynch.jscene3d.project.scene.internal to
            com.fasterxml.jackson.databind;
}
