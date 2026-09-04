/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.extension;

import io.github.glynch.jscene3d.project.importing.ArtifactContentWriter;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.io.IOException;

/** Engine-owned staging context exposed to a format adapter during preparation. */
public interface ImportPreparationContext extends ImportInspectionContext {
    /**
     * Returns the structurally validated import definition being prepared.
     *
     * @return structurally validated import definition being prepared
     */
    ImportDefinition definition();

    /**
     * Writes one complete named artifact directly into engine-owned staging storage.
     *
     * @param descriptor artifact identity and metadata
     * @param content content writer invoked exactly once before this method returns
     * @throws IOException if content cannot be generated or staged
     * @throws IllegalArgumentException if the identity duplicates another staged artifact
     */
    void artifact(ImportArtifactDescriptor descriptor, ArtifactContentWriter content) throws IOException;
}
