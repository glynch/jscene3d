/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.util.Optional;

/** Opens logical artifacts from published import generations without exposing cache paths. */
@FunctionalInterface
public interface ImportedArtifactLookup {
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
