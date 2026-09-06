/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import java.nio.file.Path;
import java.util.List;

/** Read-only source summary used by the project importer. */
record GltfProjectInspection(List<String> sceneNames, List<Path> dependencies) {
    GltfProjectInspection {
        sceneNames = List.copyOf(sceneNames);
        dependencies = List.copyOf(dependencies);
    }
}
