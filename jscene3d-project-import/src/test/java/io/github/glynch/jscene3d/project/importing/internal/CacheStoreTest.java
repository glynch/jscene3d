/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises physical import-cache validation and published-generation snapshot behavior. */
final class CacheStoreTest {
    private static final String IMPORT_ID = "sample-import";
    private static final String FINGERPRINT = "0000000000000000000000000000000000000000000000000000000000000000";

    @TempDir
    private Path temporaryDirectory;

    /** Reuses one validated immutable generation while newly opened views still validate its content. */
    @Test
    void reusesValidatedGenerationWithinPublishedView() throws IOException {
        Path cacheRoot = temporaryDirectory.resolve("cache");
        CacheStore writable = new CacheStore(cacheRoot);
        publish(writable);
        CacheStore published = CacheStore.openPublished(cacheRoot);
        CacheStore.ActiveGeneration first = published.active(IMPORT_ID).orElseThrow();
        Files.delete(first.root().resolve("artifacts/00001.bin"));

        CacheStore.ActiveGeneration reused = published.active(IMPORT_ID).orElseThrow();

        assertThat(reused).isSameAs(first);
        CacheStore newlyOpened = CacheStore.openPublished(cacheRoot);
        assertThatExceptionOfType(IOException.class).isThrownBy(() -> newlyOpened.active(IMPORT_ID));
    }

    /** Publishes one two-artifact generation through the production cache transaction. */
    private static void publish(CacheStore cache) throws IOException {
        try (TemporaryWorkspace workspace = cache.createStagingWorkspace(IMPORT_ID)) {
            Path artifacts = Files.createDirectories(workspace.root().resolve("artifacts"));
            CachedArtifact first = writeArtifact(artifacts, "first", "00000.bin");
            CachedArtifact second = writeArtifact(artifacts, "second", "00001.bin");
            CachedImportIndex index = new CachedImportIndex(
                    2,
                    IMPORT_ID,
                    "io.github.glynch.cache-test/importer",
                    1,
                    FINGERPRINT,
                    FINGERPRINT,
                    Map.of(),
                    FINGERPRINT,
                    List.of(first, second));
            cache.writeIndex(workspace.root(), index);
            cache.publish(IMPORT_ID, index, workspace);
        }
    }

    /** Writes one payload artifact and returns matching persistent metadata. */
    private static CachedArtifact writeArtifact(Path directory, String identity, String filename) throws IOException {
        Path content = directory.resolve(filename);
        Files.writeString(content, identity, StandardCharsets.UTF_8);
        return new CachedArtifact(
                identity,
                "PAYLOAD",
                null,
                null,
                null,
                "text/plain",
                List.of(),
                ImportHashes.file(content),
                Files.size(content),
                "artifacts/" + filename);
    }
}
