/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

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
import io.github.glynch.jscene3d.project.physics3d.Physics3dDescriptors;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Assembles editor state from project data without loading or executing application code. */
final class EditorProjectLoader {
    private static final String PROJECT_RESOURCES = "src/main/resources";
    private static final String IMPORT_CACHE = "target/import-cache";
    private static final RuntimeResourceProvider UNAVAILABLE_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("published runtime resources are unavailable");
        }
    };

    private final ProjectLoader projectLoader;
    private final ExtensionCatalogLoader extensionLoader;
    private final ClassLoader editorClassLoader;

    /** Creates a loader for the running editor version and its safe metadata class path. */
    EditorProjectLoader(String engineVersion, ClassLoader editorClassLoader) {
        projectLoader = new ProjectLoader(engineVersion);
        extensionLoader = new ExtensionCatalogLoader(engineVersion);
        this.editorClassLoader = Objects.requireNonNull(editorClassLoader, "editorClassLoader");
    }

    /** Loads one project and constructs an immutable session when its startup world is usable. */
    EditorProjectLoadResult load(Path projectDirectory) {
        LinkedHashSet<ProjectDiagnostic> diagnostics = new LinkedHashSet<>();
        ProjectLoadResult projectResult = projectLoader.load(projectDirectory);
        diagnostics.addAll(projectResult.diagnostics());
        if (projectResult.project().isEmpty()) {
            return failure(diagnostics);
        }
        GameProject project = projectResult.project().orElseThrow();

        AssetCatalogLoadResult assetResult = AssetCatalog.scan(project.root());
        diagnostics.addAll(assetResult.diagnostics());
        if (assetResult.catalog().isEmpty()) {
            return failure(diagnostics);
        }
        AssetCatalog authored = assetResult.catalog().orElseThrow();

        RegisteredTypeCatalog types = loadTypeCatalog(project, diagnostics);
        List<ImportDefinition> imports = loadImports(project, diagnostics);
        ProjectContent content = loadContent(project, authored, types, imports, diagnostics);
        DefinitionResolver definitions = content.definitions();
        List<EditorAssetItem> assets = loadAssets(project, authored, definitions, types, imports, diagnostics);
        Optional<WorldDefinition> startupWorld = loadStartupWorld(project, authored, definitions, types, diagnostics);
        if (startupWorld.isEmpty()) {
            return failure(diagnostics);
        }

        WorldDefinition world = startupWorld.orElseThrow();
        EditorHierarchyNode hierarchy = projectHierarchy(world, definitions, types, diagnostics);
        EditorProjectSession session =
                new EditorProjectSession(project, authored, types, content, world, hierarchy, assets);
        return new EditorProjectLoadResult(Optional.of(session), List.copyOf(diagnostics));
    }

    /** Loads descriptor resources through a project-resource class loader and adds built-in metadata. */
    private RegisteredTypeCatalog loadTypeCatalog(GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        ExtensionCatalogLoadResult extensionResult = loadProjectExtensions(project, diagnostics);
        diagnostics.addAll(extensionResult.diagnostics());
        List<ExtensionDescriptor> descriptors =
                new ArrayList<>(extensionResult.catalog().extensions());
        addIfAbsent(descriptors, Spatial3dDescriptors.extensionDescriptor());
        addIfAbsent(descriptors, Physics3dDescriptors.extensionDescriptor());
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
        if (!Files.isDirectory(resources)) {
            return extensionLoader.load(project, editorClassLoader);
        }
        URLClassLoader resourceLoader;
        try {
            URL resourceUrl = resources.toUri().toURL();
            resourceLoader = new URLClassLoader(new URL[] {resourceUrl}, editorClassLoader);
        } catch (IOException exception) {
            diagnostics.add(
                    error(resources, EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE, exception.toString()));
            return extensionLoader.load(project, editorClassLoader);
        }
        ExtensionCatalogLoadResult result = extensionLoader.load(project, resourceLoader);
        closeResourceLoader(resourceLoader, resources, diagnostics);
        return result;
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
            AssetCatalog authored,
            RegisteredTypeCatalog types,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        if (imports.size() != project.imports().size()) {
            return new ProjectContent(authored, UNAVAILABLE_RESOURCES);
        }
        try {
            return PublishedProjectContent.load(
                    project, types, authored, project.root().resolve(IMPORT_CACHE), Spatial3dResourceLoaders.all());
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException exception) {
            diagnostics.add(error(
                    project.root().resolve(IMPORT_CACHE),
                    EditorDiagnosticCode.IMPORT_CONTENT_UNAVAILABLE,
                    exception.toString()));
            return new ProjectContent(authored, UNAVAILABLE_RESOURCES);
        }
    }

    /** Validates authored definitions and builds deterministic asset-browser entries. */
    private static List<EditorAssetItem> loadAssets(
            GameProject project,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<EditorAssetItem> assets = new ArrayList<>();
        for (AssetMetadata metadata : authored.assets()) {
            assets.add(loadDefinitionAsset(metadata, definitions, types, diagnostics));
        }
        for (GameProject.AssetSource source : project.assets()) {
            assets.add(new EditorAssetItem(source.id(), source.id(), EditorAssetItem.Kind.SOURCE_ASSET, source.path()));
        }
        for (ImportDefinition definition : imports) {
            assets.add(new EditorAssetItem(
                    definition.id(), definition.id(), EditorAssetItem.Kind.IMPORT_DEFINITION, definition.source()));
        }
        return List.copyOf(assets);
    }

    /** Loads one authored definition for its label and records its complete validation diagnostics. */
    private static EditorAssetItem loadDefinitionAsset(
            AssetMetadata metadata,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        String fallback = metadata.path().getFileName().toString();
        if (metadata.kind() == AssetKind.ENTITY_DEFINITION) {
            DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(metadata.id()), types);
            diagnostics.addAll(result.diagnostics());
            String label = result.definition().map(EntityDefinition::name).orElse(fallback);
            return new EditorAssetItem(
                    label, metadata.id().toString(), EditorAssetItem.Kind.ENTITY_DEFINITION, metadata.path());
        }
        DefinitionLoadResult<WorldDefinition> result = definitions.loadWorld(AssetRef.to(metadata.id()), types);
        diagnostics.addAll(result.diagnostics());
        String label = result.definition().map(WorldDefinition::name).orElse(fallback);
        return new EditorAssetItem(
                label, metadata.id().toString(), EditorAssetItem.Kind.WORLD_DEFINITION, metadata.path());
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
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<EditorHierarchyNode> children = world.roots().stream()
                .map(entry -> projectEntry(entry, definitions, types, diagnostics, new HashSet<>()))
                .toList();
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                world.name(),
                Optional.empty(),
                Optional.of(world.id()),
                true,
                children);
    }

    /** Projects one local entity or reusable-definition placement recursively. */
    private static EditorHierarchyNode projectEntry(
            EntityEntry entry,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics,
            Set<AssetId> ancestors) {
        return switch (entry) {
            case LocalEntity local -> projectLocal(local, definitions, types, diagnostics, ancestors);
            case EntityPlacement placement -> projectPlacement(placement, definitions, types, diagnostics, ancestors);
        };
    }

    /** Projects one locally authored entity and its children. */
    private static EditorHierarchyNode projectLocal(
            LocalEntity local,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics,
            Set<AssetId> ancestors) {
        List<EditorHierarchyNode> children = local.children().stream()
                .map(child -> projectEntry(child, definitions, types, diagnostics, ancestors))
                .toList();
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.LOCAL_ENTITY,
                local.name().orElse("Unnamed entity"),
                Optional.of(local.id()),
                Optional.empty(),
                local.isEnabled(),
                children);
    }

    /** Projects one placement as its instantiated definition root. */
    private static EditorHierarchyNode projectPlacement(
            EntityPlacement placement,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics,
            Set<AssetId> ancestors) {
        DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(placement.definition(), types);
        diagnostics.addAll(result.diagnostics());
        Optional<EntityDefinition> loaded = result.definition();
        if (loaded.isEmpty() || !ancestors.add(placement.definition().id())) {
            return new EditorHierarchyNode(
                    EditorHierarchyNode.Kind.PLACEMENT,
                    placement.name().orElse("Unavailable definition"),
                    Optional.of(placement.id()),
                    Optional.of(placement.definition().id()),
                    placement.isEnabled(),
                    List.of());
        }
        EntityDefinition definition = loaded.orElseThrow();
        List<EditorHierarchyNode> children = definition.root().children().stream()
                .map(child -> projectEntry(child, definitions, types, diagnostics, ancestors))
                .toList();
        ancestors.remove(placement.definition().id());
        String label = placement.name().orElseGet(() -> definition.root().name().orElse(definition.name()));
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.PLACEMENT,
                label,
                Optional.of(placement.id()),
                Optional.of(definition.id()),
                placement.isEnabled(),
                children);
    }

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
