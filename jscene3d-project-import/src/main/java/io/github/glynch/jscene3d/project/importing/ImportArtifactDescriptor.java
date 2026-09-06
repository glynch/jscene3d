/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable description of one named artifact written during import preparation. */
public final class ImportArtifactDescriptor {
    private final String identity;
    private final ImportArtifactKind kind;
    private final Optional<AssetId> assetId;
    private final Optional<RegisteredType> resourceType;
    private final Optional<String> mediaType;
    private final List<String> references;

    /** Stores one descriptor after validating kind-specific metadata. */
    private ImportArtifactDescriptor(
            String identity,
            ImportArtifactKind kind,
            Optional<AssetId> assetId,
            Optional<RegisteredType> resourceType,
            Optional<String> mediaType,
            List<String> references) {
        this.identity = Preconditions.requirePortableIdentity(identity, "identity");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.assetId = Objects.requireNonNull(assetId, "assetId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.mediaType = Preconditions.requireOptionalNonBlank(mediaType, "mediaType");
        this.references = Preconditions.copyPortableIdentities(references, "references");
        requireConsistentMetadata();
    }

    /**
     * Describes a serialized generated entity definition.
     *
     * @param identity deterministic importer-local output identity
     * @param assetId authoritative project-wide identity declared by the generated definition
     * @param references other outputs referenced by the definition
     * @return entity-definition artifact descriptor
     */
    public static ImportArtifactDescriptor entityDefinition(String identity, AssetId assetId, List<String> references) {
        return new ImportArtifactDescriptor(
                identity,
                ImportArtifactKind.ENTITY_DEFINITION,
                Optional.of(Objects.requireNonNull(assetId, "assetId")),
                Optional.empty(),
                Optional.of("application/json"),
                references);
    }

    /**
     * Describes a serialized typed project resource.
     *
     * @param identity deterministic importer-local output identity
     * @param resourceType registered resource type stored by the document
     * @param references other outputs referenced by the resource
     * @return resource artifact descriptor
     */
    public static ImportArtifactDescriptor resource(
            String identity, RegisteredType resourceType, List<String> references) {
        return new ImportArtifactDescriptor(
                identity,
                ImportArtifactKind.RESOURCE,
                Optional.empty(),
                Optional.of(resourceType),
                Optional.of("application/json"),
                references);
    }

    /**
     * Describes opaque imported content.
     *
     * @param identity deterministic importer-local output identity
     * @param mediaType content media type
     * @return payload artifact descriptor
     */
    public static ImportArtifactDescriptor payload(String identity, String mediaType) {
        return new ImportArtifactDescriptor(
                identity,
                ImportArtifactKind.PAYLOAD,
                Optional.empty(),
                Optional.empty(),
                Optional.of(mediaType),
                List.of());
    }

    /**
     * Returns the deterministic importer-local identity.
     *
     * @return portable artifact identity
     */
    public String identity() {
        return identity;
    }

    /**
     * Returns the serialized artifact kind.
     *
     * @return artifact kind
     */
    public ImportArtifactKind kind() {
        return kind;
    }

    /**
     * Returns the authoritative project-wide definition identity.
     *
     * @return asset identity exactly when {@link #kind()} is {@link ImportArtifactKind#ENTITY_DEFINITION}
     */
    public Optional<AssetId> assetId() {
        return assetId;
    }

    /**
     * Returns the registered resource type for a resource artifact.
     *
     * @return resource type exactly when {@link #kind()} is {@link ImportArtifactKind#RESOURCE}
     */
    public Optional<RegisteredType> resourceType() {
        return resourceType;
    }

    /**
     * Returns the serialized content media type.
     *
     * @return content media type
     */
    public Optional<String> mediaType() {
        return mediaType;
    }

    /**
     * Returns other outputs referenced by this artifact.
     *
     * @return immutable referenced identities
     */
    public List<String> references() {
        return references;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ImportArtifactDescriptor descriptor
                && identity.equals(descriptor.identity)
                && kind == descriptor.kind
                && assetId.equals(descriptor.assetId)
                && resourceType.equals(descriptor.resourceType)
                && mediaType.equals(descriptor.mediaType)
                && references.equals(descriptor.references);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, kind, assetId, resourceType, mediaType, references);
    }

    @Override
    public String toString() {
        return "ImportArtifactDescriptor[identity=" + identity + ", kind=" + kind + ", assetId=" + assetId
                + ", resourceType=" + resourceType + ", mediaType=" + mediaType + ", references=" + references + ']';
    }

    /** Requires metadata appropriate to the selected artifact kind. */
    private void requireConsistentMetadata() {
        if (kind == ImportArtifactKind.ENTITY_DEFINITION && assetId.isEmpty()) {
            throw new IllegalArgumentException("entity-definition artifacts require assetId");
        }
        if (kind != ImportArtifactKind.ENTITY_DEFINITION && assetId.isPresent()) {
            throw new IllegalArgumentException("assetId is valid only for entity-definition artifacts");
        }
        if (kind == ImportArtifactKind.RESOURCE && resourceType.isEmpty()) {
            throw new IllegalArgumentException("resource artifacts require resourceType");
        }
        if (kind != ImportArtifactKind.RESOURCE && resourceType.isPresent()) {
            throw new IllegalArgumentException("resourceType is valid only for resource artifacts");
        }
        if (mediaType.isEmpty()) {
            throw new IllegalArgumentException("artifacts require mediaType");
        }
    }
}
