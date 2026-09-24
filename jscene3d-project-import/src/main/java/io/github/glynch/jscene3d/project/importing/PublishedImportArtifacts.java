/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.importing.internal.CacheStore;
import io.github.glynch.jscene3d.project.importing.internal.CachedArtifactLookup;
import java.nio.file.Path;
import java.util.Objects;

/** Opens read-only access to complete generations in a published import cache. */
public final class PublishedImportArtifacts {
    private PublishedImportArtifacts() {
        throw new AssertionError("PublishedImportArtifacts cannot be instantiated");
    }

    /**
     * Opens one immutable-generation lookup without creating or modifying cache paths.
     *
     * @param cacheRoot existing or absent published-content root
     * @return read-only lookup over active published generations
     */
    public static ImportedArtifactLookup load(Path cacheRoot) {
        Path validCacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        return new CachedArtifactLookup(CacheStore.openPublished(validCacheRoot));
    }
}
