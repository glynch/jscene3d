/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.desktop.DesktopProjectLauncher;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.exporting.internal.ApplicationDirectoryMetadata;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Complete validated macOS application-image plan prepared without changing output. */
final class MacOsApplicationImagePlan {
    private static final Pattern MACOS_APPLICATION_VERSION = Pattern.compile("[1-9]\\d*(?:\\.\\d+){0,2}");
    private static final String LAUNCHER_ENTRY =
            DesktopProjectLauncher.class.getName().replace('.', '/') + ".class";

    private final ApplicationImageRequest request;
    private final Path applicationDirectory;
    private final ApplicationDirectoryMetadata metadata;
    private final GameProject project;
    private final List<Path> runtimeArtifacts;
    private final Path launcherArtifact;

    /** Stores a fully validated plan. */
    private MacOsApplicationImagePlan(
            ApplicationImageRequest request,
            Path applicationDirectory,
            ApplicationDirectoryMetadata metadata,
            GameProject project,
            List<Path> runtimeArtifacts,
            Path launcherArtifact) {
        this.request = request;
        this.applicationDirectory = applicationDirectory;
        this.metadata = metadata;
        this.project = project;
        this.runtimeArtifacts = runtimeArtifacts;
        this.launcherArtifact = launcherArtifact;
    }

    /**
     * Validates one application-directory input and derives all macOS packaging values.
     *
     * @param request complete image request
     * @param operatingSystemName host operating-system name
     * @return complete native-image plan
     * @throws IOException when application-directory files cannot be inspected
     */
    static MacOsApplicationImagePlan prepare(ApplicationImageRequest request, String operatingSystemName)
            throws IOException {
        ApplicationImageRequest validRequest = Objects.requireNonNull(request, "request");
        requireMacOs(operatingSystemName);
        requireMacOsVersion(validRequest.applicationVersion());
        Path root = requireApplicationDirectory(validRequest.applicationDirectory());
        ApplicationDirectoryMetadata metadata = ApplicationDirectoryMetadata.read(root);
        GameProject project = loadProject(root.resolve("project"), metadata.engineVersion());
        requireApplicationName(project.identity().name());
        List<Path> artifacts = runtimeArtifacts(root.resolve("lib"));
        Path launcherArtifact = findLauncherArtifact(artifacts);
        validateOutput(validRequest.outputDirectory(), root, project.identity().name());
        return new MacOsApplicationImagePlan(validRequest, root, metadata, project, artifacts, launcherArtifact);
    }

    /** Returns the assembled application directory. */
    Path applicationDirectory() {
        return applicationDirectory;
    }

    /** Returns the packaged project display name. */
    String applicationName() {
        return project.identity().name();
    }

    /** Returns the stable package identifier. */
    String packageIdentifier() {
        return project.identity().id();
    }

    /** Returns the package description. */
    String description() {
        return project.identity().description().orElse(applicationName());
    }

    /** Returns the requested platform-native version. */
    String applicationVersion() {
        return request.applicationVersion();
    }

    /** Returns the exact engine version recorded by the application-directory exporter. */
    String engineVersion() {
        return metadata.engineVersion();
    }

    /** Returns the original application JVM arguments. */
    List<String> jvmArguments() {
        return metadata.jvmArguments();
    }

    /** Returns all packaged runtime artifacts in deterministic order. */
    List<Path> runtimeArtifacts() {
        return runtimeArtifacts;
    }

    /** Returns the artifact containing the generic desktop launcher. */
    Path launcherArtifact() {
        return launcherArtifact;
    }

    /** Returns the final native application-image root. */
    Path outputRoot() {
        return request.outputDirectory().resolve(applicationName() + ".app");
    }

    /** Returns the final native launcher. */
    Path outputLauncher() {
        return outputRoot().resolve("Contents/MacOS").resolve(applicationName());
    }

    /** Rejects hosts for which this first native-image implementation has no contract. */
    private static void requireMacOs(String operatingSystemName) {
        String validName = Objects.requireNonNull(operatingSystemName, "operatingSystemName");
        if (!validName.startsWith("Mac")) {
            throw new UnsupportedOperationException("native application-image export currently supports macOS only");
        }
    }

    /** Requires the restricted numeric version accepted by macOS jpackage images. */
    private static void requireMacOsVersion(String version) {
        if (!MACOS_APPLICATION_VERSION.matcher(version).matches()) {
            throw new IllegalArgumentException(
                    "applicationVersion must contain one to three numeric components and start above zero: " + version);
        }
    }

    /** Resolves and checks the required application-directory layout. */
    private static Path requireApplicationDirectory(Path requestedRoot) throws IOException {
        if (!Files.isDirectory(requestedRoot)) {
            throw new IllegalArgumentException("applicationDirectory is not a directory: " + requestedRoot);
        }
        Path root = requestedRoot.toRealPath();
        requireDirectory(root.resolve("project"), "packaged project");
        requireDirectory(root.resolve("content"), "packaged content");
        requireDirectory(root.resolve("lib"), "packaged runtime library");
        return root;
    }

    /** Requires one existing directory. */
    private static void requireDirectory(Path directory, String description) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(description + " directory is absent: " + directory);
        }
    }

    /** Loads the packaged project using the recorded engine version. */
    private static GameProject loadProject(Path projectDirectory, String engineVersion) {
        ProjectLoadResult result = new ProjectLoader(engineVersion).load(projectDirectory);
        return result.project().orElseThrow(() -> invalidProject(result.diagnostics()));
    }

    /** Converts project-loader diagnostics into one export-input failure. */
    private static IllegalArgumentException invalidProject(List<ProjectDiagnostic> diagnostics) {
        return new IllegalArgumentException("packaged project is invalid: " + diagnostics);
    }

    /** Requires a name that is also one safe application-bundle filename. */
    private static void requireApplicationName(String name) {
        if (name.equals(".") || name.equals("..") || name.indexOf('/') >= 0 || name.indexOf(':') >= 0) {
            throw new IllegalArgumentException("project name is not a valid macOS application name: " + name);
        }
    }

    /** Lists regular JAR artifacts and rejects links or unrelated library entries. */
    private static List<Path> runtimeArtifacts(Path libraryDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(libraryDirectory)) {
            List<Path> artifacts = paths.sorted(
                            Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            if (artifacts.isEmpty()) {
                throw new IllegalArgumentException(
                        "packaged runtime library contains no artifacts: " + libraryDirectory);
            }
            for (Path artifact : artifacts) {
                String filename = artifact.getFileName().toString();
                if (!Files.isRegularFile(artifact) || Files.isSymbolicLink(artifact) || !filename.endsWith(".jar")) {
                    throw new IllegalArgumentException(
                            "packaged runtime library entry is not a regular JAR: " + artifact);
                }
            }
            return artifacts;
        }
    }

    /** Finds the single runtime artifact containing the generic desktop launcher. */
    private static Path findLauncherArtifact(List<Path> artifacts) throws IOException {
        Path match = null;
        for (Path artifact : artifacts) {
            if (containsLauncher(artifact)) {
                if (match != null) {
                    throw new IllegalArgumentException(
                            "multiple runtime artifacts contain the generic desktop launcher: " + match + ", "
                                    + artifact);
                }
                match = artifact;
            }
        }
        if (match == null) {
            throw new IllegalArgumentException("runtime artifacts do not contain the generic desktop launcher");
        }
        return match;
    }

    /** Returns whether one JAR contains the generic desktop launcher class. */
    private static boolean containsLauncher(Path artifact) throws IOException {
        try (JarFile jar = new JarFile(artifact.toFile())) {
            return jar.getJarEntry(LAUNCHER_ENTRY) != null;
        }
    }

    /** Rejects output which could overwrite or recursively contain the application-directory input. */
    private static void validateOutput(Path outputDirectory, Path applicationDirectory, String applicationName) {
        Path outputRoot = outputDirectory
                .resolve(applicationName + ".app")
                .toAbsolutePath()
                .normalize();
        if (outputRoot.startsWith(applicationDirectory) || applicationDirectory.startsWith(outputRoot)) {
            throw new IllegalArgumentException("native image output overlaps the application directory: " + outputRoot);
        }
    }
}
