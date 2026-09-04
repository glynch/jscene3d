/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.importing.ImportPublicationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/** Owns physical cache layout, active-generation lookup, and atomic publication. */
public final class CacheStore {
    private static final String ACTIVE_GENERATION = "active-generation";
    private static final String COMMIT_LOCK = ".commit.lock";
    private static final int COMMIT_LOCK_STRIPES = 64;
    private static final ReentrantLock[] PROCESS_COMMIT_LOCKS = createProcessCommitLocks();

    private final Path cacheRoot;
    private final Path importsRoot;
    private final Path stagingRoot;
    private final CacheIndexCodec codec = new CacheIndexCodec();

    /**
     * Creates and resolves one engine-owned cache root.
     *
     * @param suppliedRoot cache root, which may not yet exist
     */
    public CacheStore(Path suppliedRoot) {
        try {
            Path normalized = Objects.requireNonNull(suppliedRoot, "suppliedRoot")
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(normalized);
            cacheRoot = normalized.toRealPath();
            importsRoot = Files.createDirectories(cacheRoot.resolve("imports")).toRealPath();
            stagingRoot = Files.createDirectories(cacheRoot.resolve("staging")).toRealPath();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to initialize import cache: " + suppliedRoot, exception);
        }
    }

    /**
     * Creates one secure staging workspace on the cache filesystem.
     *
     * @param importId project-local import identity
     * @return owned secure staging workspace
     */
    public TemporaryWorkspace createStagingWorkspace(String importId) {
        return TemporaryWorkspace.create(stagingRoot, importId + '-');
    }

    /**
     * Returns the active generation and index when both are complete and valid.
     *
     * @param importId project-local import identity
     * @return active generation when published
     * @throws IOException when the active generation is incomplete or invalid
     */
    public Optional<ActiveGeneration> active(String importId) throws IOException {
        Path importRoot = importsRoot.resolve(Preconditions.requirePortableIdentity(importId, "importId"));
        Path pointer = importRoot.resolve(ACTIVE_GENERATION);
        if (!Files.isRegularFile(pointer)) {
            return Optional.empty();
        }
        String fingerprint = Files.readString(pointer, StandardCharsets.UTF_8).strip();
        Preconditions.requireSha256(fingerprint, "active generation fingerprint");
        Path generation = importRoot.resolve(fingerprint).normalize();
        if (!generation.startsWith(importRoot) || !Files.isDirectory(generation)) {
            throw new IOException("Active import generation does not exist: " + fingerprint);
        }
        CachedImportIndex index = codec.read(generation);
        validateArtifactContent(generation, index);
        return Optional.of(new ActiveGeneration(generation, index));
    }

    /**
     * Writes the complete index into staging before publication.
     *
     * @param staging staging generation root
     * @param index complete cache index
     * @throws IOException when the index cannot be written
     */
    public void writeIndex(Path staging, CachedImportIndex index) throws IOException {
        codec.write(staging, index);
    }

    /**
     * Atomically publishes staging and updates the active generation under a cross-process lock.
     *
     * @param importId project-local import identity
     * @param index complete staged index
     * @param workspace owned staging workspace
     */
    public void publish(String importId, CachedImportIndex index, TemporaryWorkspace workspace) {
        Path importRoot = importsRoot.resolve(Preconditions.requirePortableIdentity(importId, "importId"));
        ReentrantLock processLock = processLock(importRoot);
        processLock.lock();
        try {
            Files.createDirectories(importRoot);
            try (FileChannel channel = FileChannel.open(
                            importRoot.resolve(COMMIT_LOCK), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock ignored = channel.lock()) {
                publishLocked(importRoot, index, workspace);
            }
        } catch (ImportPublicationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ImportPublicationException("Unable to publish import " + importId, exception);
        } finally {
            processLock.unlock();
        }
    }

    /**
     * Opens one cached artifact path after confining it to its immutable generation.
     *
     * @param generation active immutable generation
     * @param artifact persistent artifact metadata
     * @return validated artifact content path
     * @throws IOException when content is absent or escapes its generation
     */
    public Path artifactPath(ActiveGeneration generation, CachedArtifact artifact) throws IOException {
        Path candidate = generation.root().resolve(artifact.file()).normalize();
        if (!candidate.startsWith(generation.root()) || !Files.isRegularFile(candidate)) {
            throw new IOException("Imported artifact content is missing: " + artifact.identity());
        }
        Path real = candidate.toRealPath();
        if (!real.startsWith(generation.root().toRealPath())) {
            throw new IOException("Imported artifact content escapes its generation: " + artifact.identity());
        }
        return real;
    }

    /**
     * Returns the cache-index codec used to create supported public metadata.
     *
     * @return cache-index codec
     */
    public CacheIndexCodec codec() {
        return codec;
    }

    /** Requires every indexed artifact to retain its published size and fingerprint. */
    private void validateArtifactContent(Path generation, CachedImportIndex index) throws IOException {
        ActiveGeneration active = new ActiveGeneration(generation, index);
        for (CachedArtifact artifact : index.artifacts()) {
            Path content = artifactPath(active, artifact);
            if (Files.size(content) != artifact.size()
                    || !ImportHashes.file(content).equals(artifact.contentFingerprint())) {
                throw new IOException("Imported artifact content has changed: " + artifact.identity());
            }
        }
    }

    /** Creates the fixed process-wide lock stripes used ahead of filesystem locks. */
    private static ReentrantLock[] createProcessCommitLocks() {
        ReentrantLock[] locks = new ReentrantLock[COMMIT_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    /** Selects the process-wide lock stripe for one normalized import root. */
    private static ReentrantLock processLock(Path importRoot) {
        int stripe = Math.floorMod(importRoot.hashCode(), PROCESS_COMMIT_LOCKS.length);
        return PROCESS_COMMIT_LOCKS[stripe];
    }

    /** Publishes one generation while holding its import-specific process lock. */
    private static void publishLocked(Path importRoot, CachedImportIndex index, TemporaryWorkspace workspace)
            throws IOException {
        Path generation = importRoot.resolve(index.fingerprint());
        if (!Files.exists(generation)) {
            atomicMove(workspace.root(), generation, false);
        }
        publishActivePointer(importRoot, index.fingerprint());
    }

    /** Replaces the active-generation pointer using an atomically moved temporary file. */
    private static void publishActivePointer(Path importRoot, String fingerprint) throws IOException {
        try (TemporaryWorkspace pointerWorkspace = TemporaryWorkspace.create(importRoot, "pointer-")) {
            Path candidate = pointerWorkspace.createFile("active-", ".txt");
            Files.writeString(candidate, fingerprint + '\n', StandardCharsets.UTF_8);
            atomicMove(candidate, importRoot.resolve(ACTIVE_GENERATION), true);
        }
    }

    /** Performs a required atomic move without a non-atomic fallback. */
    private static void atomicMove(Path source, Path target, boolean replace) throws IOException {
        try {
            if (replace) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            throw new ImportPublicationException(
                    "Import cache filesystem does not support required atomic publication", exception);
        }
    }

    /** Immutable physical root and validated index for one active generation.
     *
     * @param root normalized absolute generation root
     * @param index validated cache index
     */
    public record ActiveGeneration(Path root, CachedImportIndex index) {}
}
