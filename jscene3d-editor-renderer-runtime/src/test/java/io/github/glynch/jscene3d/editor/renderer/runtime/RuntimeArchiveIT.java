/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the independently consumable editor-renderer runtime archive. */
final class RuntimeArchiveIT {
    private static final Set<String> REQUIRED_MODULES = Set.of(
            "io.github.glynch.jscene3d.core",
            "io.github.glynch.jscene3d.lwjgl",
            "io.github.glynch.jscene3d.iosurface.macos",
            "io.github.glynch.jscene3d.editor.renderer",
            "org.joml",
            "org.lwjgl",
            "org.lwjgl.glfw",
            "org.lwjgl.opengl",
            "org.lwjgl.stb");
    private static final Set<String> REQUIRED_NATIVE_LIBRARIES = Set.of(
            "native/libiosurface_bridge.dylib",
            "native/liblwjgl.dylib",
            "native/libglfw.dylib",
            "native/liblwjgl_opengl.dylib",
            "native/liblwjgl_stb.dylib");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void containsRequiredModulesAndNativeLibrariesWithoutDevelopmentArtifacts() throws IOException {
        Path archive = Path.of(System.getProperty("rendererRuntimeArchive"));
        assertThat(archive).isRegularFile();

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Set<String> entries = zip.stream().map(ZipEntry::getName).collect(Collectors.toSet());
            List<String> nativeLibraries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .filter(entry -> entry.startsWith("native/"))
                    .toList();
            assertThat(nativeLibraries).containsExactlyInAnyOrderElementsOf(REQUIRED_NATIVE_LIBRARIES);
            assertThat(entries).allSatisfy(entry -> {
                assertThat(entry).doesNotStartWith("/").doesNotContain("..").doesNotContain("/Users/");
                assertThat(entry)
                        .doesNotContain("target/classes")
                        .doesNotContain("classpath.txt")
                        .doesNotContain("surefire")
                        .doesNotContain("junit")
                        .doesNotContain("assertj");
            });
            assertThat(entries)
                    .filteredOn(entry -> entry.endsWith(".jar"))
                    .allSatisfy(entry -> assertThat(entry).matches("lib/[^/]+\\.jar"));
            assertNoAbsoluteDevelopmentPaths(zip);

            Set<String> modules = modulesIn(zip);
            assertThat(modules).containsAll(REQUIRED_MODULES);
        }
    }

    private Set<String> modulesIn(ZipFile zip) throws IOException {
        Path libraries = temporaryDirectory.resolve("lib");
        Files.createDirectories(libraries);
        List<Path> jarPaths = new ArrayList<>();
        for (ZipEntry entry :
                zip.stream().filter(item -> isRuntimeJar(item.getName())).toList()) {
            Path jar = libraries.resolve(Path.of(entry.getName()).getFileName().toString());
            try (var input = zip.getInputStream(entry)) {
                Files.copy(input, jar);
            }
            assertNoAbsoluteDevelopmentPaths(jar);
            jarPaths.add(jar);
        }

        assertThat(jarPaths).isNotEmpty();
        Set<String> modules = new HashSet<>();
        ModuleFinder.of(jarPaths.toArray(Path[]::new))
                .findAll()
                .forEach(reference -> modules.add(reference.descriptor().name()));
        return modules;
    }

    private static boolean isRuntimeJar(String entry) {
        return entry.startsWith("lib/") && entry.endsWith(".jar");
    }

    private static void assertNoAbsoluteDevelopmentPaths(ZipFile zip) throws IOException {
        for (ZipEntry entry : zip.stream().filter(item -> !item.isDirectory()).toList()) {
            try (var input = zip.getInputStream(entry)) {
                assertThat(containsAbsoluteDevelopmentPath(input.readAllBytes()))
                        .as("absolute development path in %s", entry.getName())
                        .isFalse();
            }
        }
    }

    private static void assertNoAbsoluteDevelopmentPaths(Path jar) throws IOException {
        try (JarFile archive = new JarFile(jar.toFile())) {
            for (var entry :
                    archive.stream().filter(item -> !item.isDirectory()).toList()) {
                try (var input = archive.getInputStream(entry)) {
                    assertThat(containsAbsoluteDevelopmentPath(input.readAllBytes()))
                            .as("absolute development path in %s!/%s", jar.getFileName(), entry.getName())
                            .isFalse();
                }
            }
        }
    }

    private static boolean containsAbsoluteDevelopmentPath(byte[] content) {
        byte[] marker = "/Users/".getBytes(StandardCharsets.UTF_8);
        for (int offset = 0; offset <= content.length - marker.length; offset++) {
            int matched = 0;
            while (matched < marker.length && content[offset + matched] == marker[matched]) {
                matched++;
            }
            if (matched == marker.length) {
                return true;
            }
        }
        return false;
    }
}
