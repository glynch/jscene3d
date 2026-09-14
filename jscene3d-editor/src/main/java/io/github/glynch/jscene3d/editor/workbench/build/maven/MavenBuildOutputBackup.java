/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** Preserves Maven output while a destructive clean rebuild is in progress. */
final class MavenBuildOutputBackup {
    private static final Path BACKUP_DIRECTORY = Path.of(".jscene3d", "cache", "build", "maven");

    private final Path target;
    private final @Nullable Path backupRoot;
    private final @Nullable Path backupTarget;

    private MavenBuildOutputBackup(Path target, @Nullable Path backupRoot, @Nullable Path backupTarget) {
        this.target = target;
        this.backupRoot = backupRoot;
        this.backupTarget = backupTarget;
    }

    static MavenBuildOutputBackup capture(Path projectRoot) throws IOException {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve("target");
        if (!Files.exists(target, NOFOLLOW_LINKS)) {
            return new MavenBuildOutputBackup(target, null, null);
        }

        Path backups = root.resolve(BACKUP_DIRECTORY);
        Files.createDirectories(backups);
        Path backupRoot = Files.createTempDirectory(backups, "rebuild-");
        Path backupTarget = backupRoot.resolve("target");
        try {
            Files.move(target, backupTarget);
            return new MavenBuildOutputBackup(target, backupRoot, backupTarget);
        } catch (IOException failure) {
            Files.deleteIfExists(backupRoot);
            throw failure;
        }
    }

    void commit() throws IOException {
        deleteTree(backupRoot);
    }

    void rollback() throws IOException {
        deleteTree(target);
        if (backupTarget != null && Files.exists(backupTarget, NOFOLLOW_LINKS)) {
            Files.move(backupTarget, target);
        }
        deleteTree(backupRoot);
    }

    private static void deleteTree(@Nullable Path root) throws IOException {
        if (root == null || !Files.exists(root, NOFOLLOW_LINKS)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> descendants = Files.walk(root)) {
            paths = descendants.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
