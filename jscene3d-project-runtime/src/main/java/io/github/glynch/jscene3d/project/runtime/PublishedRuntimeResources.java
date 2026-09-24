/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.importing.PublishedImportArtifacts;
import io.github.glynch.jscene3d.project.importing.PublishedProjectImports;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Composes runtime resource access over authored files and read-only published import artifacts. */
public final class PublishedRuntimeResources {
    private PublishedRuntimeResources() {
        throw new AssertionError("PublishedRuntimeResources cannot be instantiated");
    }

    /**
     * Creates a runtime provider without discovering or executing import providers.
     *
     * @param project validated project declaring imports
     * @param catalog resolved safe type metadata
     * @param cacheRoot host-selected published import cache
     * @param resourceLoaders runtime loaders for supported resource types
     * @return runtime resource provider backed by authored and published content
     */
    public static RuntimeResourceProvider load(
            GameProject project,
            RegisteredTypeCatalog catalog,
            Path cacheRoot,
            Collection<RuntimeResourceLoader<?>> resourceLoaders) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        RegisteredTypeCatalog validCatalog = Objects.requireNonNull(catalog, "catalog");
        Path validCacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        List<RuntimeResourceLoader<?>> validResourceLoaders = List.copyOf(resourceLoaders);
        List<ImportDefinition> imports = PublishedProjectImports.load(validProject);
        ImportedArtifactLookup publications = PublishedImportArtifacts.load(validCacheRoot);
        return ImportedRuntimeResources.create(validProject, validCatalog, imports, publications, validResourceLoaders);
    }
}
