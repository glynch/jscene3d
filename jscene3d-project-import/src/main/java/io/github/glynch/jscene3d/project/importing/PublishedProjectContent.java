/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Loads authored content together with complete generations from a read-only project import cache. */
public final class PublishedProjectContent {
    /** Prevents construction of this stateless integration entry point. */
    private PublishedProjectContent() {
        throw new AssertionError("PublishedProjectContent cannot be instantiated");
    }

    /**
     * Loads one project's declared import definitions and combines their published artifacts with authored content.
     *
     * <p>This operation does not discover or execute importers, inspect source assets, or write the cache. An editor or
     * build step must publish complete generations before runtime composition. Missing publications remain absent from
     * the returned resolver and are diagnosed normally if authored content references them.
     *
     * @param project validated project declaring imports
     * @param catalog resolved safe type metadata
     * @param authored authored definition catalog
     * @param cacheRoot host-selected published import cache
     * @param resourceLoaders runtime loaders for supported immutable resource types
     * @return project content combining authored and published definitions and resources
     * @throws IllegalStateException if a declared import is invalid or published definition content cannot be read
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
        List<ImportDefinition> imports = loadDeclaredImports(validProject);
        ImportManager publications = ImportManager.create(validProject, validCatalog, validCacheRoot, List.of());
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

    /** Loads every manifest-declared import and preserves all structured failures. */
    private static List<ImportDefinition> loadDeclaredImports(GameProject project) {
        ImportLoader loader = new ImportLoader();
        List<ImportDefinition> definitions = new ArrayList<>();
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        for (Path importPath : project.imports()) {
            ImportLoadResult result = loader.load(project, importPath);
            result.definition().ifPresent(definitions::add);
            diagnostics.addAll(result.diagnostics());
        }
        if (!diagnostics.isEmpty()) {
            String summary = diagnostics.stream()
                    .map(diagnostic -> diagnostic.code().code() + " at " + diagnostic.source() + diagnostic.location())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("declared project imports are invalid: " + summary);
        }
        return List.copyOf(definitions);
    }
}
