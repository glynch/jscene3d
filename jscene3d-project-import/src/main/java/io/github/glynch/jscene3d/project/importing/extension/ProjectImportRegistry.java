/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.extension;

import io.github.glynch.jscene3d.project.extension.RegisteredType;

/** Construction-time registry binding safe importer descriptors to trusted implementations. */
public interface ProjectImportRegistry {
    /**
     * Registers the implementation for one exact importer type.
     *
     * @param type exact descriptor-declared importer identity and version
     * @param importer trusted source importer
     */
    void registerImporter(RegisteredType type, ProjectImporter importer);
}
