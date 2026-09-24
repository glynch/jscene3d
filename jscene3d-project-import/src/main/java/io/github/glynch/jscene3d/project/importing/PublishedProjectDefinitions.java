/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Loads authored definitions together with complete generations from a read-only project import cache. */
public final class PublishedProjectDefinitions {
    /** Prevents construction of this stateless integration entry point. */
    private PublishedProjectDefinitions() {
        throw new AssertionError("PublishedProjectDefinitions cannot be instantiated");
    }

    /**
     * Loads one project's declared imports and combines their published entity definitions with authored definitions.
     *
     * <p>This operation does not discover or execute importers, inspect source assets, or write the cache. An editor or
     * build step must publish complete generations before loading. Missing publications remain absent from the returned
     * resolver and are diagnosed normally if authored content references them.
     *
     * @param project validated project declaring imports
     * @param authored authored definition catalog
     * @param cacheRoot host-selected published import cache
     * @return resolver combining authored and published definitions
     * @throws IllegalStateException if a declared import is invalid or published definition content cannot be read
     */
    public static DefinitionResolver load(GameProject project, AssetCatalog authored, Path cacheRoot) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        AssetCatalog validAuthored = Objects.requireNonNull(authored, "authored");
        Path validCacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        List<ImportDefinition> imports = PublishedProjectImports.load(validProject);
        ImportedArtifactLookup publications = PublishedImportArtifacts.load(validCacheRoot);
        try {
            return ImportedDefinitionResolver.create(validAuthored, imports, publications);
        } catch (IOException failure) {
            throw new IllegalStateException("published project definitions cannot be read", failure);
        }
    }
}
