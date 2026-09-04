/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportArtifactKind;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Strict codec and conversion policy for engine-owned cache indexes. */
public final class CacheIndexCodec {
    /** Fixed filename within each immutable generation. */
    public static final String INDEX_NAME = "artifact-index.json";

    private static final int SCHEMA_VERSION = 1;
    private final ObjectMapper mapper;

    /** Creates a strict deterministic cache-index codec. */
    public CacheIndexCodec() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        mapper = new ObjectMapper(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * Writes one complete index to its generation root.
     *
     * @param generationRoot destination generation root
     * @param index complete cache index
     * @throws IOException when the index cannot be written
     */
    public void write(Path generationRoot, CachedImportIndex index) throws IOException {
        mapper.writeValue(generationRoot.resolve(INDEX_NAME).toFile(), index);
    }

    /**
     * Reads and validates one complete index from its generation root.
     *
     * @param generationRoot source generation root
     * @return validated cache index
     * @throws IOException when the index cannot be read or is invalid
     */
    public CachedImportIndex read(Path generationRoot) throws IOException {
        CachedImportIndex index =
                mapper.readValue(generationRoot.resolve(INDEX_NAME).toFile(), CachedImportIndex.class);
        validate(index);
        return index;
    }

    /**
     * Builds one persistent index from a successful preparation.
     *
     * @param context completed preparation context
     * @param importer exact importer type
     * @param fingerprints related generation fingerprints
     * @return persistent cache index
     */
    public CachedImportIndex fromPreparation(
            PreparationContext context, RegisteredType importer, ImportFingerprints fingerprints) {
        Map<String, String> relativeDependencies = new LinkedHashMap<>();
        context.dependencies()
                .forEach((path, hash) -> relativeDependencies.put(
                        portableRelative(context.project().root(), path), hash));
        List<CachedArtifact> cachedArtifacts =
                context.artifacts().stream().map(this::toCachedArtifact).toList();
        return new CachedImportIndex(
                SCHEMA_VERSION,
                context.definition().id(),
                importer.id(),
                importer.version(),
                fingerprints.definition(),
                fingerprints.source(),
                relativeDependencies,
                fingerprints.complete(),
                cachedArtifacts);
    }

    /**
     * Converts persistent metadata back to its supported public representation.
     *
     * @param artifact persistent artifact metadata
     * @return public artifact metadata
     */
    public ImportedArtifactMetadata metadata(CachedArtifact artifact) {
        ImportArtifactKind kind = ImportArtifactKind.valueOf(artifact.kind());
        ImportArtifactDescriptor descriptor =
                switch (kind) {
                    case SCENE -> ImportArtifactDescriptor.scene(artifact.identity(), artifact.references());
                    case RESOURCE -> resourceDescriptor(artifact);
                    case PAYLOAD -> ImportArtifactDescriptor.payload(artifact.identity(), artifact.mediaType());
                };
        return new ImportedArtifactMetadata(descriptor, artifact.contentFingerprint(), artifact.size());
    }

    /** Restores one resource descriptor after checking its nullable JSON fields. */
    private static ImportArtifactDescriptor resourceDescriptor(CachedArtifact artifact) {
        String type = Objects.requireNonNull(artifact.resourceType(), "resourceType");
        int version = Objects.requireNonNull(artifact.resourceTypeVersion(), "resourceTypeVersion");
        return ImportArtifactDescriptor.resource(
                artifact.identity(), new RegisteredType(type, version), artifact.references());
    }

    /** Converts one staged artifact into cache-index data. */
    private CachedArtifact toCachedArtifact(StagedArtifact artifact) {
        ImportArtifactDescriptor descriptor = artifact.metadata().descriptor();
        String resourceType = descriptor.resourceType().map(RegisteredType::id).orElse(null);
        Integer resourceTypeVersion =
                descriptor.resourceType().map(RegisteredType::version).orElse(null);
        return new CachedArtifact(
                descriptor.identity(),
                descriptor.kind().name(),
                resourceType,
                resourceTypeVersion,
                descriptor.mediaType().orElseThrow(),
                descriptor.references(),
                artifact.metadata().contentFingerprint(),
                artifact.metadata().size(),
                artifact.relativePath());
    }

    /** Validates trusted-cache structure before its paths are used. */
    private void validate(CachedImportIndex index) throws IOException {
        try {
            if (index.schemaVersion() != SCHEMA_VERSION) {
                throw new IllegalArgumentException("unsupported cache index version: " + index.schemaVersion());
            }
            Preconditions.requirePortableIdentity(index.importId(), "importId");
            Preconditions.requireRegisteredTypeId(index.importerId(), "importerId");
            if (index.importerVersion() < 1) {
                throw new IllegalArgumentException("importerVersion must be positive");
            }
            Preconditions.requireSha256(index.definitionFingerprint(), "definitionFingerprint");
            Preconditions.requireSha256(index.sourceFingerprint(), "sourceFingerprint");
            Preconditions.requireSha256(index.fingerprint(), "fingerprint");
            validateDependencies(index.dependencies());
            validateArtifacts(index.artifacts());
        } catch (RuntimeException exception) {
            throw new IOException("Invalid import cache index", exception);
        }
    }

    /** Validates unique artifact identities and safe relative content paths. */
    private void validateArtifacts(List<CachedArtifact> artifacts) {
        Set<String> identities = new HashSet<>();
        for (CachedArtifact artifact : artifacts) {
            String identity = Preconditions.requirePortableIdentity(artifact.identity(), "artifact identity");
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("cache artifact identity is duplicated: " + identity);
            }
            Preconditions.requirePortableIdentity(artifact.file(), "artifact file");
            metadata(artifact);
        }
        for (CachedArtifact artifact : artifacts) {
            for (String reference : artifact.references()) {
                if (!identities.contains(reference)) {
                    throw new IllegalArgumentException("cache artifact references missing output: " + reference);
                }
            }
        }
    }

    /** Validates safe project-relative dependency paths and their fingerprints. */
    private static void validateDependencies(Map<String, String> dependencies) {
        Objects.requireNonNull(dependencies, "dependencies").forEach((path, fingerprint) -> {
            Preconditions.requirePortableIdentity(path, "dependency path");
            Preconditions.requireSha256(fingerprint, "dependency fingerprint");
        });
    }

    /** Returns one forward-slash project-relative path. */
    private static String portableRelative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }
}
