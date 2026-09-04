/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Owns an isolated temporary directory and everything created beneath it.
 *
 * <p>A workspace is not thread-safe. The caller must close it when its files are no longer needed.
 * Closing is idempotent and recursively deletes the workspace without following symbolic links.
 * Every other operation fails after closure. Filesystem failures are reported as {@link
 * UncheckedIOException}.
 */
public final class TemporaryWorkspace implements AutoCloseable {
    private final Path root;
    private boolean closed;

    /** Stores one newly created normalized absolute workspace root. */
    private TemporaryWorkspace(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Creates a uniquely named workspace in the platform temporary directory.
     *
     * @param prefix filename prefix of at least three characters
     * @return workspace owning the newly created directory
     * @throws NullPointerException if {@code prefix} is {@code null}
     * @throws IllegalArgumentException if {@code prefix} is not a simple filename prefix
     * @throws UncheckedIOException if the directory cannot be created
     */
    public static TemporaryWorkspace create(String prefix) {
        String validPrefix = requireSimpleNamePart(prefix, "prefix", true);
        try {
            return new TemporaryWorkspace(Files.createTempDirectory(validPrefix, ownerOnlyDirectoryAttributes()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to create temporary workspace", exception);
        }
    }

    /**
     * Returns the workspace root.
     *
     * @return normalized absolute root directory
     * @throws IllegalStateException if the workspace is closed
     */
    public Path root() {
        requireOpen();
        return root;
    }

    /**
     * Creates a uniquely named directory directly beneath the workspace root.
     *
     * @param prefix filename prefix of at least three characters
     * @return normalized absolute directory path
     * @throws NullPointerException if {@code prefix} is {@code null}
     * @throws IllegalArgumentException if {@code prefix} is not a simple filename prefix
     * @throws IllegalStateException if the workspace is closed
     * @throws UncheckedIOException if the directory cannot be created
     */
    public Path createDirectory(String prefix) {
        requireOpen();
        String validPrefix = requireSimpleNamePart(prefix, "prefix", true);
        try {
            return Files.createTempDirectory(root, validPrefix).toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to create temporary directory in " + root, exception);
        }
    }

    /**
     * Creates a uniquely named file directly beneath the workspace root.
     *
     * @param prefix filename prefix of at least three characters
     * @param suffix filename suffix, such as {@code .png}
     * @return normalized absolute file path
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if either argument is not a simple filename part
     * @throws IllegalStateException if the workspace is closed
     * @throws UncheckedIOException if the file cannot be created
     */
    public Path createFile(String prefix, String suffix) {
        requireOpen();
        String validPrefix = requireSimpleNamePart(prefix, "prefix", true);
        String validSuffix = requireSimpleNamePart(suffix, "suffix", false);
        try {
            return Files.createTempFile(root, validPrefix, validSuffix)
                    .toAbsolutePath()
                    .normalize();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to create temporary file in " + root, exception);
        }
    }

    /**
     * Reports whether this workspace has been closed.
     *
     * @return {@code true} after the first call to {@link #close()}
     */
    public boolean isClosed() {
        return closed;
    }

    /** Recursively deletes the workspace exactly once. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            deleteRecursively(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to delete temporary workspace: " + root, exception);
        }
    }

    /** Deletes every existing entry below the supplied root before deleting the root itself. */
    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> walkedPaths = Files.walk(root)) {
            paths = walkedPaths.sorted(Comparator.reverseOrder()).toList();
        }
        List<IOException> failures = new ArrayList<>();
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                failures.add(exception);
            }
        }
        throwCombinedFailure(failures, root);
    }

    /** Throws one cleanup failure carrying every subsequent failure as suppressed information. */
    private static void throwCombinedFailure(List<IOException> failures, Path root) throws IOException {
        if (failures.isEmpty()) {
            return;
        }
        IOException combined =
                new IOException("Unable to completely delete temporary workspace: " + root, failures.getFirst());
        failures.stream().skip(1).forEach(combined::addSuppressed);
        throw combined;
    }

    /** Requires a nonempty portable filename part and optionally a three-character minimum. */
    private static String requireSimpleNamePart(String value, String name, boolean requireMinimumLength) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()
                || validValue.indexOf('/') >= 0
                || validValue.indexOf('\\') >= 0
                || ".".equals(validValue)
                || "..".equals(validValue)
                || requireMinimumLength && validValue.length() < 3) {
            throw new IllegalArgumentException(name + " must be a simple filename part"
                    + (requireMinimumLength ? " of at least three characters" : ""));
        }
        return validValue;
    }

    /** Supplies restrictive root permissions atomically on POSIX filesystems. */
    private static FileAttribute<?>[] ownerOnlyDirectoryAttributes() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
        return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(permissions)};
    }

    /** Requires this workspace to remain open. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Temporary workspace is closed");
        }
    }
}
