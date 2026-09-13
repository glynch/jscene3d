/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Resolves a staged JDT LS home without making projects supply executable paths. */
final class JdtLanguageServerDistributionLocator {
    static final String HOME_PROPERTY = "jscene3d.jdtls.home";

    Optional<Path> locate() {
        String configured = System.getProperty(HOME_PROPERTY, "").strip();
        if (!configured.isEmpty()) {
            return Optional.of(Path.of(configured).toAbsolutePath().normalize());
        }
        return developmentCandidates().stream().filter(Files::isDirectory).findFirst();
    }

    private static List<Path> developmentCandidates() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path runtimeHome =
                Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        return List.of(
                workingDirectory.resolve("jscene3d-editor-java/target/jdtls"),
                runtimeHome.resolve("../../../app/jdtls").normalize(),
                runtimeHome.resolve("../app/jdtls").normalize());
    }
}
