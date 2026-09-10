/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetCatalogLoadResult;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.exporting.ApplicationDirectoryRequest;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceLoadResult;
import io.github.glynch.jscene3d.project.resource.ResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Complete validated source plan consumed by filesystem assembly. */
public final class ExportPlan {
    private final ApplicationDirectoryRequest request;
    private final Path projectRoot;
    private final Path publishedContentRoot;
    private final List<ExportFile> projectFiles;
    private final List<Path> runtimeArtifacts;

    /** Stores a complete plan after all input validation succeeds. */
    private ExportPlan(
            ApplicationDirectoryRequest request,
            Path projectRoot,
            Path publishedContentRoot,
            List<ExportFile> projectFiles,
            List<Path> runtimeArtifacts) {
        this.request = request;
        this.projectRoot = projectRoot;
        this.publishedContentRoot = publishedContentRoot;
        this.projectFiles = List.copyOf(projectFiles);
        this.runtimeArtifacts = List.copyOf(runtimeArtifacts);
    }

    /**
     * Loads project metadata and prepares one deterministic copy plan without changing output.
     *
     * @param request complete export request
     * @return validated deterministic export plan
     * @throws IOException when project inputs cannot be read
     */
    public static ExportPlan prepare(ApplicationDirectoryRequest request) throws IOException {
        ApplicationDirectoryRequest validRequest = Objects.requireNonNull(request, "request");
        GameProject project = loadProject(validRequest);
        Path contentRoot = requireDirectory(validRequest.publishedContentRoot(), "publishedContentRoot");
        List<Path> artifacts = validateArtifacts(validRequest.runtimeArtifacts());
        validateOutput(validRequest.outputDirectory(), project.root(), contentRoot, artifacts);
        List<ImportDefinition> imports = loadImports(project);
        List<ExportFile> projectFiles = collectProjectFiles(project, imports);
        return new ExportPlan(validRequest, project.root(), contentRoot, projectFiles, artifacts);
    }

    /**
     * Returns the exact engine version supplied to generated launchers.
     *
     * @return engine compatibility version
     */
    public String engineVersion() {
        return request.engineVersion();
    }

    /**
     * Returns the portable launcher base name.
     *
     * @return launcher base name
     */
    public String launcherName() {
        return request.launcherName();
    }

    /**
     * Returns the source project root.
     *
     * @return normalized source project root
     */
    public Path projectRoot() {
        return projectRoot;
    }

    /**
     * Returns the validated published-content root.
     *
     * @return normalized published-content root
     */
    public Path publishedContentRoot() {
        return publishedContentRoot;
    }

    /** Returns project files with destinations relative to the packaged project directory. */
    List<ExportFile> projectFiles() {
        return projectFiles;
    }

    /** Returns validated runtime artifacts in deterministic filename order. */
    List<Path> runtimeArtifacts() {
        return runtimeArtifacts;
    }

    /**
     * Returns packaged artifact filenames in deterministic order.
     *
     * @return immutable artifact filename list
     */
    public List<String> artifactNames() {
        return runtimeArtifacts.stream()
                .map(path -> path.getFileName().toString())
                .toList();
    }

    /**
     * Returns the final application directory.
     *
     * @return normalized output directory
     */
    public Path outputDirectory() {
        return request.outputDirectory();
    }

    /** Returns JVM arguments placed before the launcher class path. */
    List<String> jvmArguments() {
        return request.jvmArguments();
    }

    /** Loads a valid project using the exact requested engine compatibility version. */
    private static GameProject loadProject(ApplicationDirectoryRequest request) {
        ProjectLoadResult result = new ProjectLoader(request.engineVersion()).load(request.projectRoot());
        return result.project().orElseThrow(() -> invalidDefinition("project", result.diagnostics()));
    }

    /** Loads every declared import recipe so its raw source can be excluded from runtime output. */
    private static List<ImportDefinition> loadImports(GameProject project) {
        ImportLoader loader = new ImportLoader();
        List<ImportDefinition> definitions = new ArrayList<>(project.imports().size());
        for (Path path : project.imports()) {
            ImportLoadResult result = loader.load(project, path);
            definitions.add(result.definition().orElseThrow(() -> invalidDefinition("import", result.diagnostics())));
        }
        return List.copyOf(definitions);
    }

    /** Collects runtime-relevant authored documents without copying imported raw source assets. */
    private static List<ExportFile> collectProjectFiles(GameProject project, List<ImportDefinition> imports) {
        AssetCatalogLoadResult catalogResult = AssetCatalog.scan(project.root());
        AssetCatalog catalog = catalogResult
                .catalog()
                .orElseThrow(() -> invalidDefinition("asset catalog", catalogResult.diagnostics()));
        Map<Path, ExportFile> files = new LinkedHashMap<>();
        addProjectFile(project, files, project.root().resolve(ProjectLoader.MANIFEST_NAME));
        catalog.assets().stream().map(AssetMetadata::path).forEach(path -> addProjectFile(project, files, path));
        addProjectFile(project, files, project.runtime().entryScene());
        project.runtime().startupScene().ifPresent(path -> addProjectFile(project, files, path));
        project.runtime().projectSystems().ifPresent(path -> addProjectFile(project, files, path));
        project.runtime().inputMap().ifPresent(path -> addProjectFile(project, files, path));
        project.imports().forEach(path -> addProjectFile(project, files, path));
        addLegalFiles(project, files);
        addRuntimeAssetSources(project, imports, files);
        addLaunchFiles(project, files);
        addProjectResourcePayloads(project, imports, files);
        return files.values().stream()
                .sorted(Comparator.comparing(file -> portablePath(file.destination())))
                .toList();
    }

    /** Adds every project-owned image referenced directly by launch presentation. */
    private static void addLaunchFiles(GameProject project, Map<Path, ExportFile> files) {
        project.launch().splash().ifPresent(splash -> {
            addProjectFile(project, files, splash.background());
            addProjectFile(project, files, splash.title());
            splash.studioLogo().ifPresent(path -> addProjectFile(project, files, path));
            splash.poweredByBadges().forEach(path -> addProjectFile(project, files, path));
        });
    }

    /** Adds project-file dependencies declared inside runtime-native resource documents. */
    private static void addProjectResourcePayloads(
            GameProject project, List<ImportDefinition> imports, Map<Path, ExportFile> files) {
        Set<Path> importedSources = imports.stream()
                .map(ImportDefinition::asset)
                .map(GameProject.AssetSource::path)
                .collect(Collectors.toUnmodifiableSet());
        ResourceLoader loader = new ResourceLoader();
        project.assets().stream()
                .map(GameProject.AssetSource::path)
                .filter(path -> !importedSources.contains(path))
                .filter(ExportPlan::isResourceDefinition)
                .forEach(path -> addResourcePayloads(project, loader, path, files));
    }

    /** Loads one authored resource document and adds its project-reference dependencies. */
    private static void addResourcePayloads(
            GameProject project, ResourceLoader loader, Path path, Map<Path, ExportFile> files) {
        ResourceLoadResult result = loader.load(project, path);
        ResourceDefinition definition =
                result.resource().orElseThrow(() -> invalidDefinition("resource", result.diagnostics()));
        definition.properties().values().forEach(value -> addProjectReferences(project, value, files));
    }

    /** Recursively collects project references from one portable resource property. */
    private static void addProjectReferences(GameProject project, ProjectValue value, Map<Path, ExportFile> files) {
        switch (value) {
            case ProjectValue.ReferenceValue reference -> {
                if (reference.reference().kind() == ResourceReference.Kind.PROJECT) {
                    addProjectFile(
                            project, files, reference.reference().projectPath().orElseThrow());
                }
            }
            case ProjectValue.ArrayValue array ->
                array.values().forEach(element -> addProjectReferences(project, element, files));
            case ProjectValue.ObjectValue object ->
                object.values().values().forEach(element -> addProjectReferences(project, element, files));
            default -> {
                // Scalar and entity-target values cannot contain project-file dependencies.
            }
        }
    }

    /** Identifies the stable authored resource-document suffix. */
    private static boolean isResourceDefinition(Path path) {
        return path.getFileName().toString().endsWith(".resource.json");
    }

    /** Adds optional branding and legal documents declared by the manifest. */
    private static void addLegalFiles(GameProject project, Map<Path, ExportFile> files) {
        project.identity().icon().ifPresent(path -> addProjectFile(project, files, path));
        project.legal()
                .projectLicense()
                .flatMap(GameProject.ProjectLicense::file)
                .ifPresent(path -> addProjectFile(project, files, path));
        project.legal().thirdPartyNotices().ifPresent(path -> addProjectFile(project, files, path));
        project.legal().credits().ifPresent(path -> addProjectFile(project, files, path));
    }

    /** Adds manifest assets except raw sources whose completed publication is packaged separately. */
    private static void addRuntimeAssetSources(
            GameProject project, List<ImportDefinition> imports, Map<Path, ExportFile> files) {
        Set<Path> importedSources = imports.stream()
                .map(ImportDefinition::asset)
                .map(GameProject.AssetSource::path)
                .collect(Collectors.toUnmodifiableSet());
        project.assets().stream()
                .map(GameProject.AssetSource::path)
                .filter(path -> !importedSources.contains(path))
                .forEach(path -> addProjectFile(project, files, path));
    }

    /** Validates and indexes one project-relative source file by its destination. */
    private static void addProjectFile(GameProject project, Map<Path, ExportFile> files, Path declaredPath) {
        Path source = declaredPath.toAbsolutePath().normalize();
        if (!source.startsWith(project.root()) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException(
                    "runtime project file is missing or outside the project root: " + source);
        }
        try {
            if (!source.toRealPath().startsWith(project.root())) {
                throw new IllegalArgumentException("runtime project file resolves outside the project root: " + source);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("runtime project file cannot be resolved: " + source, exception);
        }
        Path destination = project.root().relativize(source);
        ExportFile candidate = new ExportFile(source, destination);
        ExportFile previous = files.putIfAbsent(destination, candidate);
        if (previous != null && !previous.source().equals(source)) {
            throw new IllegalArgumentException("runtime project files collide at: " + destination);
        }
    }

    /** Resolves runtime artifacts and rejects ambiguous destination filenames. */
    private static List<Path> validateArtifacts(Collection<Path> suppliedArtifacts) throws IOException {
        Map<String, Path> artifacts = new LinkedHashMap<>();
        for (Path supplied : suppliedArtifacts) {
            Path artifact = requireFile(supplied, "runtime artifact");
            String filename = artifact.getFileName().toString();
            if (!filename.endsWith(".jar")) {
                throw new IllegalArgumentException("runtime artifact is not a JAR: " + artifact);
            }
            Path previous = artifacts.putIfAbsent(filename, artifact);
            if (previous != null && !previous.equals(artifact)) {
                throw new IllegalArgumentException(
                        "runtime artifacts have the same filename: " + previous + " and " + artifact);
            }
        }
        return artifacts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    /** Rejects an output which could replace an input tree or input artifact. */
    private static void validateOutput(Path output, Path projectRoot, Path contentRoot, List<Path> artifacts) {
        if (Files.isSymbolicLink(output)) {
            throw new IllegalArgumentException("outputDirectory must not be a symbolic link: " + output);
        }
        if (Files.exists(output) && !Files.isDirectory(output)) {
            throw new IllegalArgumentException("outputDirectory exists but is not a directory: " + output);
        }
        if (projectRoot.startsWith(output) || contentRoot.startsWith(output)) {
            throw new IllegalArgumentException("outputDirectory must not contain an export input: " + output);
        }
        if (artifacts.stream().anyMatch(artifact -> artifact.startsWith(output))) {
            throw new IllegalArgumentException("outputDirectory must not contain a runtime artifact: " + output);
        }
    }

    /** Resolves one required input directory to its real location. */
    private static Path requireDirectory(Path path, String name) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(name + " is not a directory: " + path);
        }
        return path.toRealPath();
    }

    /** Resolves one required input file to its real location. */
    private static Path requireFile(Path path, String name) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(name + " is not a regular file: " + path);
        }
        return path.toRealPath();
    }

    /** Creates a stable concise validation exception from structured project diagnostics. */
    private static IllegalArgumentException invalidDefinition(String subject, List<ProjectDiagnostic> diagnostics) {
        String details = diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)
                .map(diagnostic -> diagnostic.code().code() + " at " + diagnostic.location())
                .collect(Collectors.joining(", "));
        return new IllegalArgumentException(subject + " is invalid: " + details);
    }

    /** Renders one relative path with stable separators for sorting. */
    private static String portablePath(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }
}
