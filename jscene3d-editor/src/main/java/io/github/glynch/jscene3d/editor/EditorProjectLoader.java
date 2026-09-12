/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.configuration.SettingDefinition;
import io.github.glynch.jscene3d.configuration.SettingRegistry;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.game.StandardGameDescriptors;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetCatalogLoadResult;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.PublishedProjectContent;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.settings.CoreProjectSettings;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsLoadResult;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsLoader;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryOperation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Assembles editor state from project data without loading or executing application code. */
public final class EditorProjectLoader {
    private static final String PROJECT_RESOURCES = "src/main/resources";
    private static final String PUBLISHED_CONTENT = ".jscene3d/published";
    private static final String LEGACY_MAVEN_CACHE = "target/import-cache";
    private static final RuntimeResourceProvider UNAVAILABLE_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("published runtime resources are unavailable");
        }
    };

    private final ProjectLoader projectLoader;
    private final ExtensionCatalogLoader extensionLoader;
    private final ClassLoader editorClassLoader;
    private final List<Path> installedExtensionPath;

    /** Creates a loader for the running editor version and its safe metadata class path. */
    EditorProjectLoader(String engineVersion, ClassLoader editorClassLoader) {
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
        extensionLoader = new ExtensionCatalogLoader(engineVersion);
        this.editorClassLoader = Objects.requireNonNull(editorClassLoader, "editorClassLoader");
        this.installedExtensionPath = List.copyOf(installedExtensionPath);
    }

    /** Loads one project and constructs an immutable session when its startup world is usable. */
    EditorProjectLoadResult load(Path projectDirectory) {
        try (TelemetryOperation operation = Telemetry.disabled().begin("editor.project.load", Map.of())) {
            return load(projectDirectory, operation);
        }
    }

    /** Loads one project while recording its material phases beneath the supplied operation. */
    EditorProjectLoadResult load(Path projectDirectory, TelemetryOperation operation) {
        return load(projectDirectory, operation, EditorProjectLoadProgress.NONE);
    }

    /** Loads one project while recording telemetry and publishing user-facing phase changes. */
    EditorProjectLoadResult load(
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
        RegisteredTypeCatalog types =
                operation.measure("project.extensions.load", Map.of(), () -> loadTypeCatalog(project, diagnostics));
        ProjectConfiguration configuration =
                new ProjectConfiguration(project.root(), settingRegistry(project, types, diagnostics), settings);
        diagnostics.addAll(configuration.diagnostics());
        progress.phaseStarted(EditorLoadingPhase.READING_IMPORTS);
        List<ImportDefinition> imports =
                operation.measure("project.import-definitions.load", Map.of(), () -> loadImports(project, diagnostics));
        progress.phaseStarted(EditorLoadingPhase.LOADING_PUBLISHED_CONTENT);
        ProjectContent content = operation.measure(
                "project.published-content.load",
                Map.of(),
                () -> loadContent(project, configuration, authored, types, imports, diagnostics));
        DefinitionResolver definitions = content.definitions();
        progress.phaseStarted(EditorLoadingPhase.VALIDATING_ASSETS);
        List<ProjectAsset> assets = operation.measure(
                "project.assets.validate",
                Map.of(),
                () -> loadAssets(project, authored, definitions, types, imports, diagnostics));
        progress.phaseStarted(EditorLoadingPhase.LOADING_STARTUP_WORLD);
        Optional<WorldDefinition> startupWorld = operation.measure(
                "project.startup-world.load",
                Map.of(),
                () -> loadStartupWorld(project, authored, definitions, types, diagnostics));
        if (startupWorld.isEmpty()) {
            return failure(diagnostics);
        }

        WorldDefinition world = startupWorld.orElseThrow();
        Path worldSource = authored.find(world.id())
                .map(AssetMetadata::path)
                .orElse(project.runtime().entryScene());
        progress.phaseStarted(EditorLoadingPhase.BUILDING_HIERARCHY);
        EditorHierarchyProjection hierarchyProjection = (updated, modifiedEntityIds, enabledEditor) -> projectHierarchy(
                updated,
                worldSource,
                new HierarchyProjectionContext(project.root(), authored, definitions, types, new LinkedHashSet<>()),
                modifiedEntityIds,
                enabledEditor);
        EditorProjectSession session = operation.measure(
                "project.hierarchy.project",
                Map.of(),
                () -> new EditorProjectSession(
                        new EditorProjectSession.Source(
                                project, configuration, authored, types, content, world, worldSource),
                        assets,
                        hierarchyProjection));
        return new EditorProjectLoadResult(Optional.of(session), List.copyOf(diagnostics));
    }

    /** Loads descriptor resources through a project-resource class loader and adds built-in metadata. */
    private RegisteredTypeCatalog loadTypeCatalog(GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        ExtensionCatalogLoadResult extensionResult = loadProjectExtensions(project, diagnostics);
        diagnostics.addAll(extensionResult.diagnostics());
        List<ExtensionDescriptor> descriptors =
                new ArrayList<>(extensionResult.catalog().extensions());
        StandardGameDescriptors.all().forEach(descriptor -> addIfAbsent(descriptors, descriptor));
        try {
            return RegisteredTypeCatalog.of(descriptors);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(
                    project.root().resolve(ProjectLoader.MANIFEST_NAME),
                    EditorDiagnosticCode.TYPE_CATALOG_INVALID,
                    exception.toString()));
            return extensionResult.catalog();
        }
    }

    /** Discovers JSON descriptors without placing project classes on the editor's execution path. */
    private ExtensionCatalogLoadResult loadProjectExtensions(
            GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        Path resources = project.root().resolve(PROJECT_RESOURCES);
        List<URL> metadataRoots = extensionMetadataRoots(resources, diagnostics);
        if (metadataRoots.isEmpty()) {
            return extensionLoader.load(project, editorClassLoader);
        }
        URLClassLoader resourceLoader;
        resourceLoader = new URLClassLoader(metadataRoots.toArray(URL[]::new), editorClassLoader);
        ExtensionCatalogLoadResult result = extensionLoader.load(project, resourceLoader);
        closeResourceLoader(resourceLoader, resources, diagnostics);
        return result;
    }

    /** Resolves project-local and installed descriptor roots without loading extension classes. */
    private List<URL> extensionMetadataRoots(Path projectResources, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<Path> roots = new ArrayList<>();
        if (Files.isDirectory(projectResources)) {
            roots.add(projectResources);
        }
        for (Path configured : installedExtensionPath) {
            addExtensionArtifacts(configured, roots, diagnostics);
        }
        List<URL> urls = new ArrayList<>();
        for (Path root : roots) {
            try {
                urls.add(root.toUri().toURL());
            } catch (IOException exception) {
                diagnostics.add(error(root, EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE, exception.toString()));
            }
        }
        return List.copyOf(urls);
    }

    /** Adds one artifact or the sorted JAR contents of one installed-extension directory. */
    private static void addExtensionArtifacts(
            Path configured, List<Path> roots, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        if (Files.isRegularFile(configured)) {
            roots.add(configured);
            return;
        }
        if (!Files.isDirectory(configured)) {
            diagnostics.add(warning(
                    configured,
                    EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE,
                    "installed extension path does not exist"));
            return;
        }
        if (Files.isRegularFile(configured.resolve(ExtensionCatalogLoader.DESCRIPTOR_RESOURCE))) {
            roots.add(configured);
            return;
        }
        try (var entries = Files.list(configured)) {
            entries.filter(Files::isRegularFile)
                    .filter(EditorProjectLoader::isJar)
                    .sorted()
                    .forEach(roots::add);
        } catch (IOException exception) {
            diagnostics.add(
                    warning(configured, EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE, exception.toString()));
        }
    }

    /** Returns whether an installed-extension artifact has the conventional JAR suffix. */
    private static boolean isJar(Path path) {
        return path.getFileName().toString().endsWith(".jar");
    }

    /** Closes the descriptor-only class loader and reports an unlikely resource release failure. */
    private static void closeResourceLoader(
            URLClassLoader resourceLoader, Path resources, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        try {
            resourceLoader.close();
        } catch (IOException exception) {
            diagnostics.add(
                    warning(resources, EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE, exception.toString()));
        }
    }

    /** Adds a built-in descriptor unless the discovered metadata already supplied that extension. */
    private static void addIfAbsent(List<ExtensionDescriptor> descriptors, ExtensionDescriptor descriptor) {
        boolean present =
                descriptors.stream().anyMatch(candidate -> candidate.id().equals(descriptor.id()));
        if (!present) {
            descriptors.add(descriptor);
        }
    }

    /** Loads every import definition structurally without discovering or invoking an importer. */
    private static List<ImportDefinition> loadImports(
            GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        ImportLoader loader = new ImportLoader();
        List<ImportDefinition> imports = new ArrayList<>();
        for (Path importPath : project.imports()) {
            ImportLoadResult result = loader.load(project, importPath);
            diagnostics.addAll(result.diagnostics());
            result.definition().ifPresent(imports::add);
        }
        return List.copyOf(imports);
    }

    /** Combines authored definitions with already-published definitions and spatial resources. */
    private static ProjectContent loadContent(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authored,
            RegisteredTypeCatalog types,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        if (imports.size() != project.imports().size()) {
            return new ProjectContent(authored, UNAVAILABLE_RESOURCES);
        }
        Path publishedContentRoot = resolvePublishedContentRoot(project.root(), configuration);
        try {
            return PublishedProjectContent.load(
                    project, types, authored, publishedContentRoot, Spatial3dResourceLoaders.all());
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException exception) {
            diagnostics.add(
                    error(publishedContentRoot, EditorDiagnosticCode.IMPORT_CONTENT_UNAVAILABLE, exception.toString()));
            return new ProjectContent(authored, UNAVAILABLE_RESOURCES);
        }
    }

    /** Resolves current editor cache, portable publication, then the transitional Maven cache. */
    static Path resolvePublishedContentRoot(Path projectRoot) {
        Path validProjectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        ProjectSettings settings =
                new ProjectSettingsLoader().load(validProjectRoot).settings().orElse(ProjectSettings.defaults());
        ProjectConfiguration configuration = new ProjectConfiguration(
                validProjectRoot, SettingRegistry.of(CoreProjectSettings.definitions()), settings);
        return resolvePublishedContentRoot(validProjectRoot, configuration);
    }

    /** Resolves configured editor cache, portable publication, then transitional Maven output. */
    private static Path resolvePublishedContentRoot(Path projectRoot, ProjectConfiguration configuration) {
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

    /** Composes built-in and discovered extension settings into one deterministic registry. */
    private static SettingRegistry settingRegistry(
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

    /** Validates authored definitions and builds deterministic asset-browser entries. */
    private static List<ProjectAsset> loadAssets(
            GameProject project,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<ProjectAsset> assets = new ArrayList<>();
        for (AssetMetadata metadata : authored.assets()) {
            assets.add(loadDefinitionAsset(metadata, project.root(), definitions, types, diagnostics));
        }
        for (GameProject.AssetSource source : project.assets()) {
            assets.add(new ProjectAsset(
                    source.id(),
                    source.id(),
                    ProjectAsset.Kind.SOURCE_ASSET,
                    source.path(),
                    EditorInspectorProjector.sourceAsset(source, project.root())));
        }
        for (ImportDefinition definition : imports) {
            assets.add(new ProjectAsset(
                    definition.id(),
                    definition.id(),
                    ProjectAsset.Kind.IMPORT_DEFINITION,
                    definition.source(),
                    EditorInspectorProjector.importDefinition(definition, project.root())));
        }
        return List.copyOf(assets);
    }

    /** Loads one authored definition for its label and records its complete validation diagnostics. */
    private static ProjectAsset loadDefinitionAsset(
            AssetMetadata metadata,
            Path projectRoot,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        String fallback = metadata.path().getFileName().toString();
        if (metadata.kind() == AssetKind.ENTITY_DEFINITION) {
            DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(metadata.id()), types);
            diagnostics.addAll(result.diagnostics());
            Optional<EntityDefinition> definition = result.definition();
            String label = definition.map(EntityDefinition::name).orElse(fallback);
            EditorSelection selection = definition
                    .map(value -> EditorInspectorProjector.entityDefinition(value, metadata.path(), projectRoot, types))
                    .orElseGet(() -> unavailableAssetSelection(metadata, label, projectRoot));
            return new ProjectAsset(
                    label, metadata.id().toString(), ProjectAsset.Kind.ENTITY_DEFINITION, metadata.path(), selection);
        }
        DefinitionLoadResult<WorldDefinition> result = definitions.loadWorld(AssetRef.to(metadata.id()), types);
        diagnostics.addAll(result.diagnostics());
        Optional<WorldDefinition> definition = result.definition();
        String label = definition.map(WorldDefinition::name).orElse(fallback);
        EditorSelection selection = definition
                .map(value -> EditorInspectorProjector.worldDefinition(value, metadata.path(), projectRoot))
                .orElseGet(() -> unavailableAssetSelection(metadata, label, projectRoot));
        return new ProjectAsset(
                label, metadata.id().toString(), ProjectAsset.Kind.WORLD_DEFINITION, metadata.path(), selection);
    }

    /** Preserves selection metadata for a definition whose complete data could not be loaded. */
    private static EditorSelection unavailableAssetSelection(AssetMetadata metadata, String label, Path projectRoot) {
        String source = projectRoot.relativize(metadata.path()).toString();
        EditorDetails view = new EditorDetails(
                label,
                metadata.kind() == AssetKind.ENTITY_DEFINITION ? "Entity definition" : "World definition",
                source,
                metadata.id().toString(),
                List.of(new EditorIcon(EditorIcons.READ_ONLY, "Unavailable definition · read-only")),
                List.of(new EditorDetails.Section(
                        "Definition",
                        Optional.of("Complete definition data is unavailable; see Diagnostics"),
                        false,
                        List.of())));
        String key = EditorSelectionKinds.ASSET.value() + ":" + metadata.path() + ':' + metadata.id();
        return new EditorSelection(EditorSelectionKinds.ASSET, key, view);
    }

    /** Resolves and validates the manifest's configured startup world by its stable asset identity. */
    private static Optional<WorldDefinition> loadStartupWorld(
            GameProject project,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        Optional<AssetMetadata> metadata = authored.assets().stream()
                .filter(candidate -> candidate.path().equals(project.runtime().entryScene()))
                .filter(candidate -> candidate.kind() == AssetKind.WORLD_DEFINITION)
                .findFirst();
        if (metadata.isEmpty()) {
            diagnostics.add(error(
                    project.runtime().entryScene(),
                    EditorDiagnosticCode.STARTUP_WORLD_MISSING,
                    "runtime.entryScene does not identify an authored world definition"));
            return Optional.empty();
        }
        DefinitionLoadResult<WorldDefinition> result =
                definitions.loadWorld(AssetRef.to(metadata.orElseThrow().id()), types);
        diagnostics.addAll(result.diagnostics());
        return result.definition();
    }

    /** Projects a world and placed definition roots without introducing placement wrapper nodes. */
    private static EditorHierarchyNode projectHierarchy(
            WorldDefinition world,
            Path source,
            HierarchyProjectionContext context,
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabledEditor) {
        List<EditorHierarchyNode> children = world.roots().stream()
                .map(entry ->
                        projectEntry(entry, source, context, new HashSet<>(), false, modifiedEntityIds, enabledEditor))
                .toList();
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                world.name(),
                Optional.empty(),
                Optional.of(world.id()),
                true,
                EditorInspectorProjector.world(world, source, context.projectRoot()),
                children);
    }

    /** Projects one local entity or reusable-definition placement recursively. */
    private static EditorHierarchyNode projectEntry(
            EntityEntry entry,
            Path source,
            HierarchyProjectionContext context,
            Set<AssetId> ancestors,
            boolean generated,
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabledEditor) {
        return switch (entry) {
            case LocalEntity local ->
                projectLocal(local, source, context, ancestors, generated, modifiedEntityIds, enabledEditor);
            case EntityPlacement placement ->
                projectPlacement(placement, source, context, ancestors, generated, modifiedEntityIds, enabledEditor);
        };
    }

    /** Projects one locally authored entity and its children. */
    private static EditorHierarchyNode projectLocal(
            LocalEntity local,
            Path source,
            HierarchyProjectionContext context,
            Set<AssetId> ancestors,
            boolean generated,
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabledEditor) {
        List<EditorHierarchyNode> children = local.children().stream()
                .map(child ->
                        projectEntry(child, source, context, ancestors, generated, modifiedEntityIds, enabledEditor))
                .toList();
        Optional<Consumer<Boolean>> editor =
                generated ? Optional.empty() : Optional.of(value -> enabledEditor.accept(local.id(), value));
        return new EditorHierarchyNode(
                generated ? EditorHierarchyNode.Kind.GENERATED_ENTITY : EditorHierarchyNode.Kind.LOCAL_ENTITY,
                local.name().orElse("Unnamed entity"),
                Optional.of(local.id()),
                Optional.empty(),
                new EditorHierarchyNode.AuthoringState(
                        local.isEnabled(), !generated && modifiedEntityIds.contains(local.id())),
                EditorInspectorProjector.entity(
                        local, source, context.projectRoot(), context.types(), generated, editor),
                children);
    }

    /** Projects one placement as its instantiated definition root. */
    private static EditorHierarchyNode projectPlacement(
            EntityPlacement placement,
            Path source,
            HierarchyProjectionContext context,
            Set<AssetId> ancestors,
            boolean generated,
            Set<EntityId> modifiedEntityIds,
            BiConsumer<EntityId, Boolean> enabledEditor) {
        DefinitionLoadResult<EntityDefinition> result =
                context.definitions().loadEntity(placement.definition(), context.types());
        context.diagnostics().addAll(result.diagnostics());
        Optional<EntityDefinition> loaded = result.definition();
        if (loaded.isEmpty() || !ancestors.add(placement.definition().id())) {
            return new EditorHierarchyNode(
                    EditorHierarchyNode.Kind.PLACEMENT,
                    placement.name().orElse("Unavailable definition"),
                    Optional.of(placement.id()),
                    Optional.of(placement.definition().id()),
                    new EditorHierarchyNode.AuthoringState(
                            placement.isEnabled(), !generated && modifiedEntityIds.contains(placement.id())),
                    EditorInspectorProjector.placement(
                            placement,
                            loaded,
                            source,
                            context.projectRoot(),
                            context.types(),
                            generated,
                            generated
                                    ? Optional.empty()
                                    : Optional.of(value -> enabledEditor.accept(placement.id(), value))),
                    List.of());
        }
        EntityDefinition definition = loaded.orElseThrow();
        Path definitionSource = context.authored()
                .find(definition.id())
                .map(AssetMetadata::path)
                .orElse(source);
        List<EditorHierarchyNode> children = definition.root().children().stream()
                .map(child -> projectEntry(
                        child, definitionSource, context, ancestors, true, modifiedEntityIds, enabledEditor))
                .toList();
        ancestors.remove(placement.definition().id());
        String label = placement.name().orElseGet(() -> definition.root().name().orElse(definition.name()));
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.PLACEMENT,
                label,
                Optional.of(placement.id()),
                Optional.of(definition.id()),
                new EditorHierarchyNode.AuthoringState(
                        placement.isEnabled(), !generated && modifiedEntityIds.contains(placement.id())),
                EditorInspectorProjector.placement(
                        placement,
                        loaded,
                        source,
                        context.projectRoot(),
                        context.types(),
                        generated,
                        generated
                                ? Optional.empty()
                                : Optional.of(value -> enabledEditor.accept(placement.id(), value))),
                children);
    }

    /** Shared immutable services and state for recursive hierarchy projection. */
    private record HierarchyProjectionContext(
            Path projectRoot,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {}

    /** Returns a terminal editor result while preserving all collected diagnostics. */
    private static EditorProjectLoadResult failure(LinkedHashSet<ProjectDiagnostic> diagnostics) {
        return new EditorProjectLoadResult(Optional.empty(), List.copyOf(diagnostics));
    }

    /** Creates one editor loading error. */
    private static ProjectDiagnostic error(Path source, EditorDiagnosticCode code, String detail) {
        return diagnostic(ProjectDiagnostic.Severity.ERROR, source, code, detail);
    }

    /** Creates one non-terminal editor warning. */
    private static ProjectDiagnostic warning(Path source, EditorDiagnosticCode code, String detail) {
        return diagnostic(ProjectDiagnostic.Severity.WARNING, source, code, detail);
    }

    /** Creates one structured editor-owned diagnostic. */
    private static ProjectDiagnostic diagnostic(
            ProjectDiagnostic.Severity severity, Path source, EditorDiagnosticCode code, String detail) {
        return new ProjectDiagnostic(
                severity,
                code,
                source.toAbsolutePath().normalize().toUri(),
                "",
                Map.of("technicalDetail", Objects.requireNonNullElse(detail, code.defaultMessage())));
    }
}
