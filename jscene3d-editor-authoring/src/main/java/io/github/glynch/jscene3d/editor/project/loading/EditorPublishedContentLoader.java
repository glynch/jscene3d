/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import static io.github.glynch.jscene3d.editor.project.loading.EditorLoadingDiagnostics.error;

import io.github.glynch.jscene3d.configuration.definition.SettingDefinition;
import io.github.glynch.jscene3d.configuration.registry.SettingRegistry;
import io.github.glynch.jscene3d.editor.diagnostics.EditorDiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.PublishedProjectDefinitions;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.CoreProjectSettings;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsLoader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loads import definitions and resolves their already-published project content. */
final class EditorPublishedContentLoader {
    private static final String PROJECT_RESOURCES = "src/main/resources";
    private static final String PUBLISHED_CONTENT = ".jscene3d/published";
    private static final String LEGACY_MAVEN_CACHE = "target/import-cache";

    private EditorPublishedContentLoader() {}

    /** Loads every import definition structurally without invoking an importer. */
    static List<ImportDefinition> loadImports(GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        ImportLoader loader = new ImportLoader();
        List<ImportDefinition> imports = new ArrayList<>();
        for (Path importPath : project.imports()) {
            ImportLoadResult result = loader.load(project, importPath);
            diagnostics.addAll(result.diagnostics());
            result.definition().ifPresent(imports::add);
        }
        return List.copyOf(imports);
    }

    /** Combines authored definitions with already-published definitions without creating runtime resources. */
    static DefinitionResolver load(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authored,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        if (imports.size() != project.imports().size()) {
            return authored;
        }
        Path publishedContentRoot = resolveRoot(project.root(), configuration);
        try {
            return PublishedProjectDefinitions.load(project, authored, publishedContentRoot);
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException exception) {
            diagnostics.add(
                    error(publishedContentRoot, EditorDiagnosticCode.IMPORT_CONTENT_UNAVAILABLE, exception.toString()));
            return authored;
        }
    }

    /** Resolves current editor cache, portable publication, then transitional Maven output. */
    static Path resolveRoot(Path projectRoot) {
        Path validProjectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        ProjectSettings settings =
                new ProjectSettingsLoader().load(validProjectRoot).settings().orElse(ProjectSettings.defaults());
        ProjectConfiguration configuration = new ProjectConfiguration(
                validProjectRoot, SettingRegistry.of(CoreProjectSettings.definitions()), settings);
        return resolveRoot(validProjectRoot, configuration);
    }

    /** Composes built-in and discovered extension settings into one deterministic registry. */
    static SettingRegistry settingRegistry(
            GameProject project, RegisteredTypeCatalog types, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        Map<String, SettingDefinition<?>> definitions = new LinkedHashMap<>();
        CoreProjectSettings.definitions()
                .forEach(definition -> definitions.put(definition.key().value(), definition));
        types.extensions().stream()
                .flatMap(extension -> extension.settings().stream())
                .forEach(definition -> {
                    SettingDefinition<?> existing =
                            definitions.putIfAbsent(definition.key().value(), definition);
                    if (existing != null) {
                        diagnostics.add(error(
                                project.root().resolve(PROJECT_RESOURCES),
                                EditorDiagnosticCode.SETTING_REGISTRY_INVALID,
                                "Setting " + definition.key().value() + " is declared by both " + existing.owner()
                                        + " and " + definition.owner()));
                    }
                });
        return SettingRegistry.of(definitions.values());
    }

    /** Resolves configured editor cache, portable publication, then transitional Maven output. */
    private static Path resolveRoot(Path projectRoot, ProjectConfiguration configuration) {
        Path validProjectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        Path projectCache = configuration.resolve(CoreProjectSettings.CACHE_LOCATION);
        if (Files.isDirectory(projectCache.resolve("imports"))) {
            return projectCache;
        }
        Path publishedContent = validProjectRoot.resolve(PUBLISHED_CONTENT);
        if (Files.isDirectory(publishedContent.resolve("imports"))) {
            return publishedContent;
        }
        return validProjectRoot.resolve(LEGACY_MAVEN_CACHE);
    }
}
