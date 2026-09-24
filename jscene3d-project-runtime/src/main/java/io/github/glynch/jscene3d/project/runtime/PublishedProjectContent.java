/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.importing.ImportedDefinitionResolver;
import io.github.glynch.jscene3d.project.importing.PublishedImportArtifacts;
import io.github.glynch.jscene3d.project.importing.PublishedProjectImports;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Composes runtime content from authored state and a read-only published-import cache. */
public final class PublishedProjectContent {
    private PublishedProjectContent() {
        throw new AssertionError("PublishedProjectContent cannot be instantiated");
    }

    /**
     * Combines authored and published definitions with runtime resources from the same publications.
     *
     * <p>This operation never discovers or executes import providers and never writes the cache.
     *
     * @param project validated project declaring imports
     * @param catalog resolved safe type metadata
     * @param authored authored definition catalog
     * @param cacheRoot host-selected published import cache
     * @param resourceLoaders runtime loaders for supported resource types
     * @return complete runtime project content
     */
    public static ProjectContent load(
            GameProject project,
            RegisteredTypeCatalog catalog,
            AssetCatalog authored,
            Path cacheRoot,
            Collection<RuntimeResourceLoader<?>> resourceLoaders) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        RegisteredTypeCatalog validCatalog = Objects.requireNonNull(catalog, "catalog");
        AssetCatalog validAuthored = Objects.requireNonNull(authored, "authored");
        Path validCacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        List<RuntimeResourceLoader<?>> validResourceLoaders = List.copyOf(resourceLoaders);
        List<ImportDefinition> imports = PublishedProjectImports.load(validProject);
        ImportedArtifactLookup publications = PublishedImportArtifacts.load(validCacheRoot);
        DefinitionResolver definitions;
        try {
            definitions = ImportedDefinitionResolver.create(validAuthored, imports, publications);
        } catch (IOException failure) {
            throw new IllegalStateException("published project definitions cannot be read", failure);
        }
        RuntimeResourceProvider resources = ImportedRuntimeResources.create(
                validProject, validCatalog, imports, publications, validResourceLoaders);
        return new ProjectContent(definitions, resources);
    }
}
