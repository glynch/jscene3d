/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Deterministic local Maven-project fixture whose wrapper records requested goals. */
final class MavenBuildTestProject {
    private static final Set<PosixFilePermission> EXECUTABLE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE);

    private final Path root;
    private final Path wrapper;
    private final Path invocations;

    private MavenBuildTestProject(Path root, Path wrapper, Path invocations) {
        this.root = root;
        this.wrapper = wrapper;
        this.invocations = invocations;
    }

    static MavenBuildTestProject create(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>editor-build-fixture</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Path invocations = root.resolve("wrapper-invocations.txt");
        Path wrapper;
        if (OperatingSystem.current() == OperatingSystem.WINDOWS) {
            wrapper = root.resolve("mvnw.cmd");
            Files.writeString(wrapper, windowsWrapper());
        } else {
            wrapper = root.resolve("mvnw");
            Files.writeString(wrapper, unixWrapper());
            Files.setPosixFilePermissions(wrapper, EXECUTABLE_PERMISSIONS);
        }
        return new MavenBuildTestProject(root, wrapper, invocations);
    }

    Path root() {
        return root;
    }

    Path wrapper() {
        return wrapper;
    }

    void failBuild() throws IOException {
        Files.createFile(root.resolve("fail-build"));
    }

    void blockBuild() throws IOException {
        Files.createFile(root.resolve("block-build"));
    }

    ProcessHandle awaitBlockingChild() throws Exception {
        Path childProcess = root.resolve("blocking-child.pid");
        long timeoutNanos = Duration.ofSeconds(5).toNanos();
        long deadline = System.nanoTime() + timeoutNanos;
        try (WatchService changes = root.getFileSystem().newWatchService()) {
            root.register(changes, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            while (System.nanoTime() < deadline) {
                Optional<ProcessHandle> child = blockingChild(childProcess);
                if (child.isPresent()) {
                    return child.orElseThrow();
                }
                long remaining = Math.max(0L, deadline - System.nanoTime());
                WatchKey change = changes.poll(remaining, TimeUnit.NANOSECONDS);
                if (change == null) {
                    break;
                }
                change.pollEvents();
                if (!change.reset()) {
                    break;
                }
            }
        }
        throw new IllegalStateException("fixture wrapper did not start its blocking child");
    }

    private static Optional<ProcessHandle> blockingChild(Path childProcess) throws IOException {
        if (!Files.isRegularFile(childProcess)) {
            return Optional.empty();
        }
        String value = Files.readString(childProcess).strip();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return ProcessHandle.of(Long.parseLong(value));
        } catch (NumberFormatException failure) {
            return Optional.empty();
        }
    }

    List<String> invocations() throws IOException {
        return Files.readAllLines(invocations);
    }

    private static String unixWrapper() {
        return """
                #!/bin/sh
                printf '%s\\n' "$*" >> wrapper-invocations.txt
                if [ -f fail-build ]; then
                  printf 'fixture compilation failed\\n'
                  printf '[ERROR] Example.java:[4,9] cannot find symbol\\n' >&2
                  exit 1
                fi
                if [ -f block-build ]; then
                  (trap '' TERM; while :; do sleep 1; done) &
                  child=$!
                  printf '%s\\n' "$child" > blocking-child.pid
                  wait "$child"
                fi
                printf 'fixture build completed\\n'
                printf 'fixture build details\\n' >&2
                """;
    }

    private static String windowsWrapper() {
        return """
                @echo off
                echo %*>> wrapper-invocations.txt
                if exist fail-build (
                  echo fixture compilation failed
                  echo [ERROR] Example.java:[4,9] cannot find symbol 1>&2
                  exit /b 1
                )
                echo fixture build completed
                echo fixture build details 1>&2
                """;
    }
}
