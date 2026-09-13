/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import static io.github.glynch.jscene3d.editor.project.loading.EditorLoadingDiagnostics.error;
import static io.github.glynch.jscene3d.editor.project.loading.EditorLoadingDiagnostics.warning;

import io.github.glynch.jscene3d.editor.diagnostics.EditorDiagnosticCode;
import io.github.glynch.jscene3d.game.StandardGameDescriptors;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Discovers project and installed extension descriptors without executing project code. */
final class EditorExtensionMetadataLoader {
    private static final String PROJECT_RESOURCES = "src/main/resources";

    private final ExtensionCatalogLoader extensionLoader;
    private final ClassLoader editorClassLoader;
    private final List<Path> installedExtensionPath;

    /** Creates a loader for one engine version and its descriptor-only class path. */
    EditorExtensionMetadataLoader(
            String engineVersion, ClassLoader editorClassLoader, List<Path> installedExtensionPath) {
        extensionLoader = new ExtensionCatalogLoader(engineVersion);
        this.editorClassLoader = Objects.requireNonNull(editorClassLoader, "editorClassLoader");
        this.installedExtensionPath = List.copyOf(installedExtensionPath);
    }

    /** Loads discovered descriptors and adds any missing built-in game descriptors. */
    RegisteredTypeCatalog load(GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
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

    /** Discovers JSON descriptors through an isolated metadata class loader. */
    private ExtensionCatalogLoadResult loadProjectExtensions(
            GameProject project, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        Path resources = project.root().resolve(PROJECT_RESOURCES);
        List<URL> metadataRoots = extensionMetadataRoots(resources, diagnostics);
        if (metadataRoots.isEmpty()) {
            return extensionLoader.load(project, editorClassLoader);
        }
        URLClassLoader resourceLoader = new URLClassLoader(metadataRoots.toArray(URL[]::new), editorClassLoader);
        ExtensionCatalogLoadResult result = extensionLoader.load(project, resourceLoader);
        closeResourceLoader(resourceLoader, resources, diagnostics);
        return result;
    }

    /** Resolves project-local and installed descriptor roots. */
    private List<URL> extensionMetadataRoots(Path projectResources, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<Path> roots = new ArrayList<>();
        if (Files.isDirectory(projectResources)) {
            roots.add(projectResources);
        }
        installedExtensionPath.forEach(configured -> addExtensionArtifacts(configured, roots, diagnostics));
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
                    .filter(EditorExtensionMetadataLoader::isJar)
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

    /** Closes the descriptor-only class loader and reports a resource release failure. */
    private static void closeResourceLoader(
            URLClassLoader resourceLoader, Path resources, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        try {
            resourceLoader.close();
        } catch (IOException exception) {
            diagnostics.add(
                    warning(resources, EditorDiagnosticCode.EXTENSION_METADATA_UNAVAILABLE, exception.toString()));
        }
    }

    /** Adds a built-in descriptor unless discovered metadata already supplied that extension. */
    private static void addIfAbsent(List<ExtensionDescriptor> descriptors, ExtensionDescriptor descriptor) {
        boolean present =
                descriptors.stream().anyMatch(candidate -> candidate.id().equals(descriptor.id()));
        if (!present) {
            descriptors.add(descriptor);
        }
    }
}
