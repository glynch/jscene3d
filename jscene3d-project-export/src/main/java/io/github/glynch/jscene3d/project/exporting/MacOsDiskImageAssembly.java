/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.exporting.internal.StagedPathInstall;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stages, invokes, validates, and transactionally installs one macOS disk image. */
final class MacOsDiskImageAssembly {
    private final MacOsDiskImagePlan plan;
    private final DiskImageTool tool;

    /** Stores the validated plan and packaging-tool adapter. */
    MacOsDiskImageAssembly(MacOsDiskImagePlan plan, DiskImageTool tool) {
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
            Path output = Files.createDirectory(staging.resolve("output"));
            Path resources = Files.createDirectory(staging.resolve("resources"));
            tool.create(plan, output, resources);
            Path generated = output.resolve(plan.outputPath().getFileName());
            validateGeneratedDiskImage(generated);
            StagedPathInstall.replace(generated, plan.outputPath());
        } finally {
            StagedPathInstall.deleteIfPresent(staging);
        }
    }

    /** Requires one non-empty regular DMG produced in the private staging directory. */
    private static void validateGeneratedDiskImage(Path diskImage) throws IOException {
        if (!Files.isRegularFile(diskImage) || Files.isSymbolicLink(diskImage) || Files.size(diskImage) == 0) {
            throw new IOException("jpackage did not produce the expected disk image: " + diskImage);
        }
    }
}
