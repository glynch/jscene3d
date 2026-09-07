/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.exporting.internal.StagedPathInstall;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Stages, invokes, validates, and transactionally installs one macOS disk image. */
final class MacOsDiskImageAssembly {
    private static final Path APPLICATIONS_DIRECTORY = Path.of("/Applications");

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
            Path generated = staging.resolve(plan.outputPath().getFileName());
            Path content = stageContent(staging.resolve("content"));
            tool.create(content, plan.applicationName(), generated);
            validateGeneratedDiskImage(generated);
            StagedPathInstall.replace(generated, plan.outputPath());
        } finally {
            StagedPathInstall.deleteIfPresent(staging);
        }
    }

    /** Stages the application image and conventional Applications link without invoking Finder. */
    private Path stageContent(Path content) throws IOException {
        Files.createDirectory(content);
        copyApplicationImage(
                plan.applicationImage(), content.resolve(plan.applicationImage().getFileName()));
        Files.createSymbolicLink(content.resolve("Applications"), APPLICATIONS_DIRECTORY);
        return content;
    }

    /** Copies an application bundle while preserving native file attributes and symbolic links. */
    private static void copyApplicationImage(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Files.createDirectories(
                        destination.resolve(source.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Path target = destination.resolve(source.relativize(file).toString());
                if (attributes.isSymbolicLink()) {
                    Files.createSymbolicLink(target, Files.readSymbolicLink(file));
                } else {
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Requires one non-empty regular DMG produced in the private staging directory. */
    private static void validateGeneratedDiskImage(Path diskImage) throws IOException {
        if (!Files.isRegularFile(diskImage) || Files.isSymbolicLink(diskImage) || Files.size(diskImage) == 0) {
            throw new IOException("hdiutil did not produce the expected disk image: " + diskImage);
        }
    }
}
