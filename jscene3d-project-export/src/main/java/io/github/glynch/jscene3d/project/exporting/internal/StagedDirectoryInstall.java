/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Transactional replacement and cleanup for privately staged export directories. */
public final class StagedDirectoryInstall {
    /** Prevents construction. */
    private StagedDirectoryInstall() {
        throw new AssertionError("StagedDirectoryInstall cannot be instantiated");
    }

    /**
     * Replaces one output directory with a complete staging directory.
     *
     * @param staging complete private staging directory
     * @param output final output directory
     * @throws IOException when installation or restoration fails
     */
    public static void replace(Path staging, Path output) throws IOException {
        if (Files.notExists(output)) {
            move(staging, output);
            return;
        }
        Path parent = requireParent(output);
        Path backup =
                Files.createTempDirectory(parent, '.' + output.getFileName().toString() + "-backup-");
        Files.delete(backup);
        move(output, backup);
        try {
            move(staging, output);
        } catch (IOException exception) {
            restoreBackup(output, backup, exception);
            throw exception;
        }
        deleteTree(backup);
    }

    /**
     * Deletes one staging or backup tree if it still exists.
     *
     * @param root known private export directory
     * @throws IOException when the tree cannot be deleted
     */
    public static void deleteIfPresent(Path root) throws IOException {
        if (Files.exists(root)) {
            deleteTree(root);
        }
    }

    /** Restores the previous output and retains both failures if restoration itself fails. */
    private static void restoreBackup(Path output, Path backup, IOException installFailure) {
        try {
            if (Files.exists(output)) {
                deleteTree(output);
            }
            move(backup, output);
        } catch (IOException restorationFailure) {
            installFailure.addSuppressed(restorationFailure);
        }
    }

    /** Moves one directory atomically where supported and portably otherwise. */
    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination);
        }
    }

    /** Deletes a known export staging, backup, or replaced-output tree child first. */
    private static void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Returns the required output parent. */
    private static Path requireParent(Path output) {
        Path parent = output.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("output must have a parent: " + output);
        }
        return parent;
    }
}
