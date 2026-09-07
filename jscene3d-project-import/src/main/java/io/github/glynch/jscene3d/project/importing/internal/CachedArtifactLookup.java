/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Read-only artifact access over active immutable cache generations. */
public final class CachedArtifactLookup implements ImportedArtifactLookup {
    private final CacheStore cache;
    private final CacheIndexCodec codec;

    /**
     * Creates an artifact lookup over one cache storage policy.
     *
     * @param cache cache storage policy
     */
    public CachedArtifactLookup(CacheStore cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
        codec = cache.codec();
    }

    @Override
    public List<ImportedArtifactMetadata> artifacts(ImportDefinition definition) {
        ImportDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        try {
            return cache.active(validDefinition.id()).stream()
                    .flatMap(generation -> generation.index().artifacts().stream())
                    .map(codec::metadata)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to list imported artifacts for " + validDefinition.id(), exception);
        }
    }

    @Override
    public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
        ImportDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        String validIdentity = Preconditions.requirePortableIdentity(identity, "identity");
        try {
            Optional<CacheStore.ActiveGeneration> active = cache.active(validDefinition.id());
            if (active.isEmpty()) {
                return Optional.empty();
            }
            CacheStore.ActiveGeneration generation = active.orElseThrow();
            Optional<CachedArtifact> artifact = generation.index().artifacts().stream()
                    .filter(candidate -> candidate.identity().equals(validIdentity))
                    .findFirst();
            if (artifact.isEmpty()) {
                return Optional.empty();
            }
            CachedArtifact cachedArtifact = artifact.orElseThrow();
            return Optional.of(new InternalImportedArtifact(
                    codec.metadata(cachedArtifact), cache.artifactPath(generation, cachedArtifact)));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to open imported artifact " + validIdentity, exception);
        }
    }
}
