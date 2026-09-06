/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.util.List;
import java.util.Optional;

/** Opens logical artifacts from published import generations without exposing cache paths. */
public interface ImportedArtifactLookup {
    /**
     * Lists artifacts from the active published generation in deterministic publication order.
     *
     * @param definition structurally validated import definition
     * @return immutable artifact metadata, or an empty list when no generation is published
     */
    List<ImportedArtifactMetadata> artifacts(ImportDefinition definition);

    /**
     * Opens one artifact from the active published generation for an import definition.
     *
     * <p>The returned handle belongs to the caller and must be closed. An empty result means that
     * no published artifact currently exists for the requested identity.
     *
     * @param definition structurally validated import definition
     * @param identity importer-local artifact identity
     * @return owned artifact handle when available
     */
    Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity);
}
