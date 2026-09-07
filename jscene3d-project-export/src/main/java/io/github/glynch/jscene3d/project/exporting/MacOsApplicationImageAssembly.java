/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.desktop.DesktopProjectLauncher;
import io.github.glynch.jscene3d.project.exporting.internal.ApplicationImageMetadata;
import io.github.glynch.jscene3d.project.exporting.internal.StagedPathInstall;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/** Stages, invokes, validates, and transactionally installs one macOS application image. */
final class MacOsApplicationImageAssembly {
    private static final String APP_DIRECTORY = "$APPDIR";

    private final MacOsApplicationImagePlan plan;
    private final JpackageTool tool;

    /** Stores the validated plan and packaging-tool adapter. */
    MacOsApplicationImageAssembly(MacOsApplicationImagePlan plan, JpackageTool tool) {
        this.plan = plan;
        this.tool = tool;
    }

    /**
     * Produces and installs the complete native image.
     *
     * @throws IOException when staging, packaging, validation, or installation fails
     */
    void write() throws IOException {
        Path outputParent = plan.outputRoot().getParent();
        if (outputParent == null) {
            throw new IllegalArgumentException("native image output must have a parent: " + plan.outputRoot());
        }
        Files.createDirectories(outputParent);
        Path staging = Files.createTempDirectory(outputParent, ".native-image-staging-");
        try {
            Path input = Files.createDirectory(staging.resolve("input"));
            Path packageOutput = Files.createDirectory(staging.resolve("jpackage-output"));
            populateInput(input);
            runJpackage(input, packageOutput);
            Path generatedImage = packageOutput.resolve(plan.applicationName() + ".app");
            validateGeneratedImage(generatedImage);
            StagedPathInstall.replace(generatedImage, plan.outputRoot());
        } finally {
            StagedPathInstall.deleteIfPresent(staging);
        }
    }

    /** Copies only runtime files from the reusable application-directory output. */
    private void populateInput(Path input) throws IOException {
        for (Path artifact : plan.runtimeArtifacts()) {
            Files.copy(artifact, input.resolve(artifact.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
        }
        copyTree(plan.applicationDirectory().resolve("project"), input.resolve("project"));
        copyTree(plan.applicationDirectory().resolve("content"), input.resolve("content"));
        new ApplicationImageMetadata(plan.applicationName(), plan.applicationVersion()).write(input);
    }

    /** Invokes jpackage with a structured argument vector and reports its complete failure output. */
    private void runJpackage(Path input, Path packageOutput) throws IOException {
        JpackageToolResult result = tool.execute(arguments(input, packageOutput));
        if (result.exitCode() != 0) {
            throw new IOException("jpackage failed with exit code " + result.exitCode() + ":\n" + result.output());
        }
    }

    /** Builds the complete macOS jpackage argument vector. */
    private List<String> arguments(Path input, Path packageOutput) {
        List<String> arguments = new ArrayList<>();
        option(arguments, "--type", "app-image");
        option(arguments, "--name", plan.applicationName());
        option(arguments, "--app-version", plan.applicationVersion());
        option(arguments, "--description", plan.description());
        option(arguments, "--input", input.toString());
        option(arguments, "--main-jar", plan.launcherArtifact().getFileName().toString());
        option(arguments, "--main-class", DesktopProjectLauncher.class.getName());
        option(arguments, "--dest", packageOutput.toString());
        option(arguments, "--mac-package-identifier", plan.packageIdentifier());
        option(arguments, "--mac-app-category", "games");
        for (String argument : plan.jvmArguments()) {
            option(arguments, "--java-options", argument);
        }
        option(
                arguments,
                "--java-options",
                property(DesktopProjectLauncher.ENGINE_VERSION_PROPERTY, plan.engineVersion()));
        option(
                arguments,
                "--java-options",
                property(DesktopProjectLauncher.PROJECT_DIRECTORY_PROPERTY, APP_DIRECTORY + "/project"));
        option(
                arguments,
                "--java-options",
                property(DesktopProjectLauncher.CONTENT_DIRECTORY_PROPERTY, APP_DIRECTORY + "/content"));
        return List.copyOf(arguments);
    }

    /** Appends one jpackage option and its indivisible value. */
    private static void option(List<String> arguments, String name, String value) {
        arguments.add(name);
        arguments.add(value);
    }

    /** Renders one Java system-property option. */
    private static String property(String name, String value) {
        return "-D" + name + '=' + value;
    }

    /** Requires the minimal native image structure promised by the result API. */
    private void validateGeneratedImage(Path image) throws IOException {
        if (!Files.isDirectory(image)) {
            throw new IOException("jpackage did not produce the expected application image: " + image);
        }
        Path launcher = image.resolve("Contents/MacOS").resolve(plan.applicationName());
        if (!Files.isRegularFile(launcher)) {
            throw new IOException("jpackage image does not contain its native launcher: " + launcher);
        }
        Path runtime = image.resolve("Contents/runtime/Contents/Home/lib/modules");
        if (!Files.isRegularFile(runtime)) {
            throw new IOException("jpackage image does not contain its bundled Java runtime: " + runtime);
        }
    }

    /** Copies one regular-file directory tree while rejecting symbolic links. */
    private static void copyTree(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                if (attributes.isSymbolicLink()) {
                    throw new IOException("application directory contains a symbolic link: " + directory);
                }
                Files.createDirectories(destination.resolve(source.relativize(directory)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
                    throw new IOException("application directory contains a non-regular file: " + file);
                }
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
