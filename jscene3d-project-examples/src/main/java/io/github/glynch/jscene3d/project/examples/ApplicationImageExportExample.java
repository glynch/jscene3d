/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.exporting.ApplicationImage;
import io.github.glynch.jscene3d.project.exporting.ApplicationImageExporter;
import io.github.glynch.jscene3d.project.exporting.ApplicationImageRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/** Exports caller-resolved game and engine JARs as one relocatable application image. */
public final class ApplicationImageExportExample {
    private static final int REQUIRED_ARGUMENT_COUNT = 6;
    private static final Logger LOGGER = Logger.getLogger(ApplicationImageExportExample.class.getName());

    /** Prevents construction of this application entry point. */
    private ApplicationImageExportExample() {
        throw new AssertionError("ApplicationImageExportExample cannot be instantiated");
    }

    /**
     * Exports one project from engine version, launcher name, project, published content, output, and runtime JARs.
     *
     * @param arguments five fixed export values followed by one or more runtime JAR paths
     * @throws IOException when export I/O fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length < REQUIRED_ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                    "expected engine-version, launcher-name, project-directory, published-content-directory, "
                            + "output-directory, and one or more runtime-artifact arguments");
        }
        List<Path> artifacts = Arrays.asList(arguments).subList(5, arguments.length).stream()
                .map(Path::of)
                .toList();
        ApplicationImageRequest request = ApplicationImageRequest.builder()
                .engineVersion(arguments[0])
                .launcherName(arguments[1])
                .projectRoot(Path.of(arguments[2]))
                .publishedContentRoot(Path.of(arguments[3]))
                .outputDirectory(Path.of(arguments[4]))
                .runtimeArtifacts(artifacts)
                .build();
        ApplicationImage image = new ApplicationImageExporter().export(request);
        LOGGER.info(() -> "Application image = " + image.root() + ", runtime artifacts = "
                + image.runtimeArtifacts().size());
    }
}
