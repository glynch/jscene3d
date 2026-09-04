/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable description of one named artifact written during import preparation. */
public final class ImportArtifactDescriptor {
    private final String identity;
    private final ImportArtifactKind kind;
    private final Optional<RegisteredType> resourceType;
    private final Optional<String> mediaType;
    private final List<String> references;

    /** Stores one descriptor after validating kind-specific metadata. */
    private ImportArtifactDescriptor(
            String identity,
            ImportArtifactKind kind,
            Optional<RegisteredType> resourceType,
            Optional<String> mediaType,
            List<String> references) {
        this.identity = Preconditions.requirePortableIdentity(identity, "identity");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.mediaType = Preconditions.requireOptionalNonBlank(mediaType, "mediaType");
        this.references = Preconditions.copyPortableIdentities(references, "references");
        requireConsistentMetadata();
    }

    /**
     * Describes a serialized scene.
     *
     * @param identity deterministic importer-local output identity
     * @param references other outputs referenced by the scene
     * @return scene artifact descriptor
     */
    public static ImportArtifactDescriptor scene(String identity, List<String> references) {
        return new ImportArtifactDescriptor(
                identity, ImportArtifactKind.SCENE, Optional.empty(), Optional.of("application/json"), references);
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
                identity, ImportArtifactKind.PAYLOAD, Optional.empty(), Optional.of(mediaType), List.of());
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
                && resourceType.equals(descriptor.resourceType)
                && mediaType.equals(descriptor.mediaType)
                && references.equals(descriptor.references);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, kind, resourceType, mediaType, references);
    }

    @Override
    public String toString() {
        return "ImportArtifactDescriptor[identity=" + identity + ", kind=" + kind + ", resourceType=" + resourceType
                + ", mediaType=" + mediaType + ", references=" + references + ']';
    }

    /** Requires metadata appropriate to the selected artifact kind. */
    private void requireConsistentMetadata() {
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
