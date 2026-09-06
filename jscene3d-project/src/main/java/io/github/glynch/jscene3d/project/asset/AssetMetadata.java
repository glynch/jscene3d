/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePositive;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import java.nio.file.Path;
import java.util.Objects;

/** Catalog metadata read without constructing an asset's complete definition.
 *
 * @param id stable asset identity
 * @param kind authored asset kind
 * @param formatVersion positive asset-format version
 * @param path current normalized absolute source path
 */
public record AssetMetadata(AssetId id, AssetKind kind, int formatVersion, Path path) {
    /** Validates catalog metadata. */
    public AssetMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        formatVersion = requirePositive(formatVersion, "formatVersion");
        path = requireNormalizedAbsolute(path, "path");
    }
}
