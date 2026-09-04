/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import java.util.List;
import java.util.Map;

/** JSON persistence model for one complete immutable import generation.
 *
 * @param schemaVersion cache-index schema version
 * @param importId project-local import identity
 * @param importerId registered importer identity
 * @param importerVersion registered importer version
 * @param definitionFingerprint configuration fingerprint
 * @param sourceFingerprint source content fingerprint
 * @param dependencies project-relative dependency fingerprints
 * @param fingerprint complete generation fingerprint
 * @param artifacts imported artifact index
 */
public record CachedImportIndex(
        int schemaVersion,
        String importId,
        String importerId,
        int importerVersion,
        String definitionFingerprint,
        String sourceFingerprint,
        Map<String, String> dependencies,
        String fingerprint,
        List<CachedArtifact> artifacts) {}
