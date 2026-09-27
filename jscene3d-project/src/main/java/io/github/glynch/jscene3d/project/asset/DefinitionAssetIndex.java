/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.asset.internal.DefinitionDocumentReader;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable internal index of authored and generated definition documents. */
final class DefinitionAssetIndex {
    private final Path projectRoot;
    private final Map<AssetId, Source> sources;

    /** Stores copied sources in deterministic insertion order. */
    private DefinitionAssetIndex(Path projectRoot, Map<AssetId, Source> sources) {
        this.projectRoot = projectRoot;
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
    }

    /** Starts an index builder from an authored catalog. */
    static Builder builder(AssetCatalog assets) {
        return new Builder(assets);
    }

    /** Returns the containing project root used to resolve project references. */
    Path projectRoot() {
        return projectRoot;
    }

    /** Returns the logical project source used when a requested identity is absent. */
    URI projectSource() {
        return projectRoot.toUri();
    }

    /** Finds one definition source by authoritative identity. */
    Optional<Source> find(AssetId id) {
        return Optional.ofNullable(sources.get(id));
    }

    /** Returns an immutable copy with one trusted authored root supplied from candidate bytes. */
    DefinitionAssetIndex withAuthoredOverride(AssetMetadata metadata, byte[] content) {
        Map<AssetId, Source> overridden = new LinkedHashMap<>(sources);
        Source current = overridden.get(metadata.id());
        if (current == null || !current.matches(metadata)) {
            throw new IllegalArgumentException("authored source is not present in this resolver: " + metadata.id());
        }
        overridden.put(metadata.id(), Source.authored(metadata, content));
        return new DefinitionAssetIndex(projectRoot, overridden);
    }

    /** One authored file or generated in-memory definition document. */
    static final class Source {
        private final AssetId id;
        private final AssetKind kind;
        private final int formatVersion;
        private final URI logicalSource;
        private final @Nullable AssetMetadata metadata;
        private final byte @Nullable [] content;

        /** Stores one validated source representation. */
        private Source(
                AssetId id,
                AssetKind kind,
                int formatVersion,
                URI logicalSource,
                @Nullable AssetMetadata metadata,
                byte @Nullable [] content) {
            this.id = id;
            this.kind = kind;
            this.formatVersion = formatVersion;
            this.logicalSource = logicalSource;
            this.metadata = metadata;
            this.content = content == null ? null : Arrays.copyOf(content, content.length);
        }

        /** Creates one authored source. */
        static Source authored(AssetMetadata metadata) {
            return new Source(
                    metadata.id(),
                    metadata.kind(),
                    metadata.formatVersion(),
                    metadata.path().toUri(),
                    metadata,
                    null);
        }

        /** Creates one authored source whose exact content is already retained by the caller. */
        static Source authored(AssetMetadata metadata, byte[] content) {
            return new Source(
                    metadata.id(),
                    metadata.kind(),
                    metadata.formatVersion(),
                    metadata.path().toUri(),
                    metadata,
                    Objects.requireNonNull(content, "content"));
        }

        /** Creates one generated entity-definition source. */
        static Source generatedEntity(AssetId id, URI source, byte[] content) {
            return new Source(id, AssetKind.ENTITY_DEFINITION, AssetCatalog.FORMAT_VERSION, source, null, content);
        }

        AssetId id() {
            return id;
        }

        AssetKind kind() {
            return kind;
        }

        URI source() {
            return logicalSource;
        }

        /** Returns whether metadata still identifies this exact authored catalog entry. */
        boolean matches(AssetMetadata candidate) {
            return metadata != null && metadata.equals(candidate);
        }

        /** Reads and structurally validates this source as an entity definition. */
        DefinitionDocumentReader.ReadResult<EntityDefinition> readEntity(Path projectRoot) {
            if (metadata != null && content == null) {
                return DefinitionDocumentReader.readEntity(projectRoot, metadata);
            }
            if (metadata != null) {
                byte[] sourceContent = Objects.requireNonNull(content, "authored content");
                return DefinitionDocumentReader.readEntity(
                        projectRoot,
                        metadata,
                        new ByteArrayInputStream(Arrays.copyOf(sourceContent, sourceContent.length)));
            }
            byte[] generatedContent = Objects.requireNonNull(content, "generated content");
            return DefinitionDocumentReader.readEntity(
                    projectRoot,
                    logicalSource,
                    id,
                    formatVersion,
                    new ByteArrayInputStream(Arrays.copyOf(generatedContent, generatedContent.length)));
        }

        /** Reads and structurally validates this authored source as a world definition. */
        DefinitionDocumentReader.ReadResult<WorldDefinition> readWorld(Path projectRoot) {
            if (content != null) {
                byte[] sourceContent = content;
                return DefinitionDocumentReader.readWorld(
                        projectRoot,
                        Objects.requireNonNull(metadata, "authored world metadata"),
                        new ByteArrayInputStream(Arrays.copyOf(sourceContent, sourceContent.length)));
            }
            return DefinitionDocumentReader.readWorld(
                    projectRoot, Objects.requireNonNull(metadata, "authored world metadata"));
        }
    }

    /** Mutable construction state hidden behind the public resolver builder. */
    static final class Builder {
        private final Path projectRoot;
        private final Map<AssetId, Source> sources = new LinkedHashMap<>();

        /** Copies authored sources in catalog order. */
        private Builder(AssetCatalog assets) {
            projectRoot = assets.root();
            for (AssetMetadata metadata : assets.assets()) {
                sources.put(metadata.id(), Source.authored(metadata));
            }
        }

        /** Adds one generated source with collision detection. */
        void addGeneratedEntity(AssetId id, URI source, byte[] content) {
            Source previous = sources.putIfAbsent(id, Source.generatedEntity(id, source, content));
            if (previous != null) {
                throw new IllegalArgumentException(
                        "definition asset ID is already present at " + previous.source() + ": " + id);
            }
        }

        /** Replaces one authored source with copied candidate bytes for one normal graph load. */
        Builder overrideAuthored(AssetMetadata metadata, byte[] content) {
            Source current = sources.get(metadata.id());
            if (current == null || !current.matches(metadata)) {
                throw new IllegalArgumentException("authored source is not present in this catalog: " + metadata.id());
            }
            sources.put(metadata.id(), Source.authored(metadata, content));
            return this;
        }

        /** Copies the completed index. */
        DefinitionAssetIndex build() {
            return new DefinitionAssetIndex(projectRoot, sources);
        }
    }
}
