/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/** Writable per-project, per-version files kept separate from the immutable server distribution. */
record JdtLanguageServerProjectLayout(Path configuration, Path workspace, Path errorLog) {
    static JdtLanguageServerProjectLayout prepare(
            Path projectRoot,
            Path userCache,
            JdtLanguageServerDistribution distribution,
            JdtLanguageServerMetadata metadata)
            throws IOException {
        Path canonicalProjectRoot =
                Objects.requireNonNull(projectRoot, "projectRoot").toRealPath();
        Path cache = canonicalLocation(Objects.requireNonNull(userCache, "userCache"))
                .resolve("language-servers/jdtls")
                .resolve(metadata.version())
                .resolve("projects")
                .resolve(projectIdentity(canonicalProjectRoot));
        Path configuration = cache.resolve("configuration");
        Path workspaceLocation = cache.resolve("workspace");
        requireSeparateLocations(canonicalProjectRoot, workspaceLocation);
        copyConfiguration(distribution.platformConfiguration(), configuration);
        Path workspace = Files.createDirectories(workspaceLocation);
        Path logs = Files.createDirectories(cache.resolve("logs"));
        return new JdtLanguageServerProjectLayout(configuration, workspace, logs.resolve("stderr.log"));
    }

    private static String projectIdentity(Path projectRoot) {
        Path fileName = projectRoot.getFileName();
        String name = fileName == null ? "project" : fileName.toString();
        UUID location = UUID.nameUUIDFromBytes(projectRoot.toString().getBytes(StandardCharsets.UTF_8));
        return name + "-" + location;
    }

    private static void requireSeparateLocations(Path projectRoot, Path workspace) throws IOException {
        if (workspace.startsWith(projectRoot) || projectRoot.startsWith(workspace)) {
            throw new IOException("JDT LS workspace must not overlap the Java project: " + workspace);
        }
    }

    private static Path canonicalLocation(Path location) throws IOException {
        Path existing = location.toAbsolutePath().normalize();
        Deque<Path> missing = new ArrayDeque<>();
        while (!Files.exists(existing)) {
            Path fileName = existing.getFileName();
            if (fileName == null) {
                break;
            }
            missing.addFirst(fileName);
            existing = existing.getParent();
        }
        Path canonical = existing.toRealPath();
        for (Path segment : missing) {
            canonical = canonical.resolve(segment);
        }
        return canonical.normalize();
    }

    private static void copyConfiguration(Path source, Path destination) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext()) {
                copy(source, destination, iterator.next());
            }
        }
    }

    private static void copy(Path sourceRoot, Path destinationRoot, Path source) throws IOException {
        Path destination = destinationRoot.resolve(sourceRoot.relativize(source));
        if (Files.isDirectory(source)) {
            Files.createDirectories(destination);
        } else {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
