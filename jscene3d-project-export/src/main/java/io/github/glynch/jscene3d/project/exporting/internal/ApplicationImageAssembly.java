/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Filesystem implementation of one validated application-image export plan. */
public final class ApplicationImageAssembly {
    private static final String DESKTOP_LAUNCHER = "io.github.glynch.jscene3d.project.desktop.DesktopProjectLauncher";

    /** Prevents construction. */
    private ApplicationImageAssembly() {
        throw new AssertionError("ApplicationImageAssembly cannot be instantiated");
    }

    /**
     * Stages and installs one complete application image.
     *
     * @param plan validated source and destination plan
     * @throws IOException when staging, installation, or cleanup fails
     */
    public static void write(ExportPlan plan) throws IOException {
        ExportPlan validPlan = Objects.requireNonNull(plan, "plan");
        Path output = validPlan.outputDirectory();
        Path parent = output.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("outputDirectory must have a parent: " + output);
        }
        Files.createDirectories(parent);
        Path staging =
                Files.createTempDirectory(parent, '.' + output.getFileName().toString() + "-staging-");
        try {
            populate(validPlan, staging);
            install(staging, output);
        } finally {
            deleteIfPresent(staging);
        }
    }

    /** Writes every application-image section into a private staging directory. */
    private static void populate(ExportPlan plan, Path staging) throws IOException {
        copyProject(plan, staging.resolve("project"));
        copyPublishedContent(plan.publishedContentRoot(), staging.resolve("content"));
        copyArtifacts(plan, staging.resolve("lib"));
        writeLaunchers(plan, staging.resolve("bin"));
    }

    /** Copies the planned authored project documents. */
    private static void copyProject(ExportPlan plan, Path destinationRoot) throws IOException {
        for (ExportFile file : plan.projectFiles()) {
            copyFile(file.source(), destinationRoot.resolve(file.destination()));
        }
    }

    /** Copies committed published content while excluding cache coordination state. */
    private static void copyPublishedContent(Path sourceRoot, Path destinationRoot) throws IOException {
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Path relative = sourceRoot.relativize(directory);
                if (relative.getNameCount() == 1
                        && relative.getFileName().toString().equals("staging")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(destinationRoot.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (attributes.isSymbolicLink()) {
                    throw new IOException("published content contains a symbolic link: " + file);
                }
                if (!file.getFileName().toString().equals(".commit.lock")) {
                    copyFile(file, destinationRoot.resolve(sourceRoot.relativize(file)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Copies the complete caller-resolved runtime class path. */
    private static void copyArtifacts(ExportPlan plan, Path destinationRoot) throws IOException {
        for (Path artifact : plan.runtimeArtifacts()) {
            copyFile(artifact, destinationRoot.resolve(artifact.getFileName()));
        }
    }

    /** Writes portable launchers which depend only on application-image-relative paths. */
    private static void writeLaunchers(ExportPlan plan, Path destinationRoot) throws IOException {
        Files.createDirectories(destinationRoot);
        Path posix = destinationRoot.resolve(plan.launcherName());
        Path windows = destinationRoot.resolve(plan.launcherName() + ".cmd");
        Files.writeString(posix, posixLauncher(plan), StandardCharsets.UTF_8);
        Files.writeString(windows, windowsLauncher(plan), StandardCharsets.UTF_8);
        makeExecutable(posix);
    }

    /** Renders the POSIX launcher with every caller value treated as one shell argument. */
    private static String posixLauncher(ExportPlan plan) {
        StringBuilder javaArguments = new StringBuilder();
        for (String argument : plan.jvmArguments()) {
            javaArguments.append(' ').append(posixArgument(argument));
        }
        return "#!/bin/sh\n"
                + "# Generated by JScene3D project export.\n\n"
                + "set -eu\n\n"
                + "launcher_directory=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\n"
                + "application_home=$(CDPATH= cd -- \"$launcher_directory/..\" && pwd)\n"
                + "java_command=java\n"
                + "if [ -n \"${JAVA_HOME:-}\" ]; then\n"
                + "    java_command=\"$JAVA_HOME/bin/java\"\n"
                + "fi\n\n"
                + "exec \"$java_command\""
                + javaArguments
                + " -classpath \"$application_home/lib/*\" \\\n    "
                + DESKTOP_LAUNCHER
                + ' '
                + posixArgument(plan.engineVersion())
                + " \\\n    \"$application_home/project\" \\\n    \"$application_home/content\"\n";
    }

    /** Renders the Windows command launcher with application-image-relative paths. */
    private static String windowsLauncher(ExportPlan plan) {
        StringBuilder javaArguments = new StringBuilder();
        for (String argument : plan.jvmArguments()) {
            javaArguments.append(' ').append(windowsArgument(argument));
        }
        return "@echo off\r\n"
                + "rem Generated by JScene3D project export.\r\n\r\n"
                + "setlocal\r\n"
                + "set \"APPLICATION_HOME=%~dp0..\"\r\n"
                + "set \"JAVA_COMMAND=java\"\r\n"
                + "if defined JAVA_HOME set \"JAVA_COMMAND=%JAVA_HOME%\\bin\\java.exe\"\r\n\r\n"
                + "\"%JAVA_COMMAND%\""
                + javaArguments
                + " -classpath \"%APPLICATION_HOME%\\lib\\*\" "
                + DESKTOP_LAUNCHER
                + ' '
                + windowsArgument(plan.engineVersion())
                + " \"%APPLICATION_HOME%\\project\" \"%APPLICATION_HOME%\\content\"\r\n";
    }

    /** Quotes one arbitrary non-control POSIX shell argument. */
    private static String posixArgument(String value) {
        return '\'' + value.replace("'", "'\"'\"'") + '\'';
    }

    /** Quotes one arbitrary non-control Windows process argument. */
    private static String windowsArgument(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /** Copies one regular file after creating its destination parent. */
    private static void copyFile(Path source, Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("destination must have a parent: " + destination);
        }
        Files.createDirectories(parent);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Marks a launcher executable on POSIX filesystems and does nothing elsewhere. */
    private static void makeExecutable(Path launcher) throws IOException {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(launcher, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Windows and other non-POSIX filesystems use the generated command launcher.
        }
    }

    /** Installs staging, restoring an existing image if the final move fails. */
    private static void install(Path staging, Path output) throws IOException {
        if (Files.notExists(output)) {
            move(staging, output);
            return;
        }
        Path backup = Files.createTempDirectory(
                output.getParent(), '.' + output.getFileName().toString() + "-backup-");
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

    /** Restores the previous image and retains both failures if restoration itself fails. */
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

    /** Deletes one remaining staging or backup tree when present. */
    private static void deleteIfPresent(Path root) throws IOException {
        if (Files.exists(root)) {
            deleteTree(root);
        }
    }

    /** Deletes a known application-image staging, backup, or replaced-output tree child first. */
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
}
