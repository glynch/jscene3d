/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetCatalogLoadResult;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsLoadResult;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsLoader;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryOperation;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Assembles editor state from project data without loading or executing application code. */
public final class EditorProjectLoader {
    private final ProjectLoader projectLoader;
    private final EditorExtensionMetadataLoader extensionMetadataLoader;

    /** Creates a loader for the running editor version and its safe metadata class path. */
    public EditorProjectLoader(String engineVersion, ClassLoader editorClassLoader) {
        this(engineVersion, editorClassLoader, List.of());
    }

    /**
     * Creates a loader with installed extension artifacts used only for descriptor discovery.
     *
     * @param engineVersion running engine version
     * @param editorClassLoader class loader used for built-in descriptor discovery
     * @param installedExtensionPath installed extension artifacts and directories
     */
    public EditorProjectLoader(String engineVersion, ClassLoader editorClassLoader, List<Path> installedExtensionPath) {
        projectLoader = new ProjectLoader(engineVersion);
        extensionMetadataLoader =
                new EditorExtensionMetadataLoader(engineVersion, editorClassLoader, installedExtensionPath);
    }

    /** Loads one project and constructs an immutable session when its startup world is usable. */
    public EditorProjectLoadResult load(Path projectDirectory) {
        try (TelemetryOperation operation = Telemetry.disabled().begin("editor.project.load", Map.of())) {
            return load(projectDirectory, operation);
        }
    }

    /** Loads one project while recording its material phases beneath the supplied operation. */
    public EditorProjectLoadResult load(Path projectDirectory, TelemetryOperation operation) {
        return load(projectDirectory, operation, EditorProjectLoadProgress.NONE);
    }

    /** Loads one project while recording telemetry and publishing user-facing phase changes. */
    public EditorProjectLoadResult load(
            Path projectDirectory, TelemetryOperation operation, EditorProjectLoadProgress progress) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(progress, "progress");
        LinkedHashSet<ProjectDiagnostic> diagnostics = new LinkedHashSet<>();
        progress.phaseStarted(EditorLoadingPhase.READING_MANIFEST);
        ProjectLoadResult projectResult =
                operation.measure("project.manifest.load", Map.of(), () -> projectLoader.load(projectDirectory));
        diagnostics.addAll(projectResult.diagnostics());
        if (projectResult.project().isEmpty()) {
            return failure(diagnostics);
        }
        GameProject project = projectResult.project().orElseThrow();
        progress.projectIdentified(project.identity().name());
        ProjectSettingsLoadResult settingsResult = new ProjectSettingsLoader().load(project.root());
        diagnostics.addAll(settingsResult.diagnostics());
        if (settingsResult.settings().isEmpty()) {
            return failure(diagnostics);
        }
        ProjectSettings settings = settingsResult.settings().orElseThrow();

        progress.phaseStarted(EditorLoadingPhase.SCANNING_ASSETS);
        AssetCatalogLoadResult assetResult =
                operation.measure("project.asset-catalog.scan", Map.of(), () -> AssetCatalog.scan(project.root()));
        diagnostics.addAll(assetResult.diagnostics());
        if (assetResult.catalog().isEmpty()) {
            return failure(diagnostics);
        }
        AssetCatalog authored = assetResult.catalog().orElseThrow();

        progress.phaseStarted(EditorLoadingPhase.LOADING_EXTENSIONS);
        RegisteredTypeCatalog types = operation.measure(
                "project.extensions.load", Map.of(), () -> extensionMetadataLoader.load(project, diagnostics));
        ProjectConfiguration configuration = new ProjectConfiguration(
                project.root(), EditorPublishedContentLoader.settingRegistry(project, types, diagnostics), settings);
        diagnostics.addAll(configuration.diagnostics());
        progress.phaseStarted(EditorLoadingPhase.READING_IMPORTS);
        List<ImportDefinition> imports = operation.measure(
                "project.import-definitions.load",
                Map.of(),
                () -> EditorPublishedContentLoader.loadImports(project, diagnostics));
        progress.phaseStarted(EditorLoadingPhase.LOADING_PUBLISHED_CONTENT);
        ProjectContent content = operation.measure(
                "project.published-content.load",
                Map.of(),
                () -> EditorPublishedContentLoader.load(project, configuration, authored, types, imports, diagnostics));
        DefinitionResolver definitions = content.definitions();
        progress.phaseStarted(EditorLoadingPhase.VALIDATING_ASSETS);
        List<ProjectAsset> assets = operation.measure(
                "project.assets.validate",
                Map.of(),
                () -> EditorProjectAssetLoader.load(project, authored, definitions, types, imports, diagnostics));
        progress.phaseStarted(EditorLoadingPhase.LOADING_STARTUP_WORLD);
        Optional<WorldDefinition> startupWorld = operation.measure(
                "project.startup-world.load",
                Map.of(),
                () -> EditorProjectAssetLoader.loadStartupWorld(project, authored, definitions, types, diagnostics));
        if (startupWorld.isEmpty()) {
            return failure(diagnostics);
        }

        WorldDefinition world = startupWorld.orElseThrow();
        Path worldSource = authored.find(world.id())
                .map(AssetMetadata::path)
                .orElse(project.runtime().entryScene());
        progress.phaseStarted(EditorLoadingPhase.BUILDING_HIERARCHY);
        EditorHierarchyProjector hierarchyProjector = new EditorHierarchyProjector(
                worldSource, project.root(), authored, definitions, types, new LinkedHashSet<>());
        EditorProjectSession session = operation.measure(
                "project.hierarchy.project",
                Map.of(),
                () -> new EditorProjectSession(
                        new EditorProjectSession.Source(
                                project, configuration, authored, types, content, world, worldSource),
                        assets,
                        hierarchyProjector));
        return new EditorProjectLoadResult(Optional.of(session), List.copyOf(diagnostics));
    }

    /** Resolves current editor cache, portable publication, then the transitional Maven cache. */
    public static Path resolvePublishedContentRoot(Path projectRoot) {
        return EditorPublishedContentLoader.resolveRoot(projectRoot);
    }

    /** Returns a terminal editor result while preserving all collected diagnostics. */
    private static EditorProjectLoadResult failure(LinkedHashSet<ProjectDiagnostic> diagnostics) {
        return new EditorProjectLoadResult(Optional.empty(), List.copyOf(diagnostics));
    }
}
