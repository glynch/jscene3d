/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import io.github.glynch.jscene3d.project.exporting.ApplicationDirectoryExporter;
import io.github.glynch.jscene3d.project.exporting.ApplicationDirectoryRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Process adapter used by build tools which have already resolved a runtime dependency directory. */
public final class ApplicationDirectoryExportCommand {
    private static final int REQUIRED_ARGUMENT_COUNT = 7;

    /** Prevents construction of this command-line adapter. */
    private ApplicationDirectoryExportCommand() {
        throw new AssertionError("ApplicationDirectoryExportCommand cannot be instantiated");
    }

    /**
     * Exports one application directory from engine version, launcher name, project, content, dependency directory,
     * application JAR, output directory, and optional trailing JVM arguments.
     *
     * @param arguments ordered export inputs followed by zero or more JVM arguments
     * @throws IOException when dependency discovery or export fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length < REQUIRED_ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                    "expected engine-version, launcher-name, project-directory, published-content-directory, "
                            + "runtime-artifact-directory, application-artifact, and output-directory arguments");
        }
        Path runtimeDirectory = Path.of(arguments[4]).toAbsolutePath().normalize();
        List<Path> runtimeArtifacts = discoverRuntimeArtifacts(runtimeDirectory);
        runtimeArtifacts.add(Path.of(arguments[5]));
        List<String> jvmArguments =
                List.copyOf(Arrays.asList(arguments).subList(REQUIRED_ARGUMENT_COUNT, arguments.length));
        ApplicationDirectoryRequest request = ApplicationDirectoryRequest.builder()
                .engineVersion(arguments[0])
                .launcherName(arguments[1])
                .projectRoot(Path.of(arguments[2]))
                .publishedContentRoot(Path.of(arguments[3]))
                .runtimeArtifacts(runtimeArtifacts)
                .outputDirectory(Path.of(arguments[6]))
                .jvmArguments(jvmArguments)
                .build();
        new ApplicationDirectoryExporter().export(request);
    }

    /** Discovers only regular JARs in one non-recursive dependency directory. */
    private static List<Path> discoverRuntimeArtifacts(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("runtime-artifact-directory is not a directory: " + directory);
        }
        try (var paths = Files.list(directory)) {
            return new ArrayList<>(paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList());
        }
    }
}
