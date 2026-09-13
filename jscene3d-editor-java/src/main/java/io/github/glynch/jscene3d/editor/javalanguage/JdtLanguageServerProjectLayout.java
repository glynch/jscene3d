/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

/** Writable per-project, per-version files kept separate from the immutable server distribution. */
record JdtLanguageServerProjectLayout(Path configuration, Path workspace, Path errorLog) {
    static JdtLanguageServerProjectLayout prepare(
            Path projectRoot, JdtLanguageServerDistribution distribution, JdtLanguageServerMetadata metadata)
            throws IOException {
        Path cache = Objects.requireNonNull(projectRoot, "projectRoot")
                .resolve(".jscene3d/cache/language-servers/jdtls")
                .resolve(metadata.version());
        Path configuration = cache.resolve("configuration");
        copyConfiguration(distribution.platformConfiguration(), configuration);
        Path workspace = Files.createDirectories(cache.resolve("workspace"));
        Path logs = Files.createDirectories(cache.resolve("logs"));
        return new JdtLanguageServerProjectLayout(configuration, workspace, logs.resolve("stderr.log"));
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
