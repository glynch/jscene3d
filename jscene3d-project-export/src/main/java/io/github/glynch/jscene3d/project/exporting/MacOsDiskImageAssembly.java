/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.exporting.internal.StagedPathInstall;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stages, invokes, validates, and transactionally installs one macOS disk image. */
final class MacOsDiskImageAssembly {
    private final MacOsDiskImagePlan plan;
    private final JpackageTool tool;

    /** Stores the validated plan and packaging-tool adapter. */
    MacOsDiskImageAssembly(MacOsDiskImagePlan plan, JpackageTool tool) {
        this.plan = plan;
        this.tool = tool;
    }

    /**
     * Produces and installs the complete disk image.
     *
     * @throws IOException when staging, packaging, validation, or installation fails
     */
    void write() throws IOException {
        Path outputParent = plan.outputPath().getParent();
        if (outputParent == null) {
            throw new IllegalArgumentException("disk-image output must have a parent: " + plan.outputPath());
        }
        Files.createDirectories(outputParent);
        Path staging = Files.createTempDirectory(outputParent, ".disk-image-staging-");
        try {
            runJpackage(staging);
            Path generated = staging.resolve(plan.outputPath().getFileName());
            validateGeneratedDiskImage(generated);
            StagedPathInstall.replace(generated, plan.outputPath());
        } finally {
            StagedPathInstall.deleteIfPresent(staging);
        }
    }

    /** Invokes jpackage with a structured argument vector and reports its complete failure output. */
    private void runJpackage(Path staging) throws IOException {
        JpackageToolResult result = tool.execute(arguments(staging));
        if (result.exitCode() != 0) {
            throw new IOException("jpackage failed with exit code " + result.exitCode() + ":\n" + result.output());
        }
    }

    /** Builds the complete macOS jpackage argument vector. */
    private List<String> arguments(Path staging) {
        List<String> arguments = new ArrayList<>();
        option(arguments, "--type", "dmg");
        option(arguments, "--name", plan.applicationName());
        option(arguments, "--app-version", plan.applicationVersion());
        option(arguments, "--app-image", plan.applicationImage().toString());
        option(arguments, "--dest", staging.toString());
        return List.copyOf(arguments);
    }

    /** Appends one jpackage option and its indivisible value. */
    private static void option(List<String> arguments, String name, String value) {
        arguments.add(name);
        arguments.add(value);
    }

    /** Requires one non-empty regular DMG produced in the private staging directory. */
    private static void validateGeneratedDiskImage(Path diskImage) throws IOException {
        if (!Files.isRegularFile(diskImage) || Files.isSymbolicLink(diskImage) || Files.size(diskImage) == 0) {
            throw new IOException("jpackage did not produce the expected disk image: " + diskImage);
        }
    }
}
