/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.Objects;

/** Immutable metadata for one completely written imported artifact. */
public final class ImportedArtifactMetadata {
    private final ImportArtifactDescriptor descriptor;
    private final String contentFingerprint;
    private final long size;

    /**
     * Creates artifact metadata.
     *
     * @param descriptor logical artifact description
     * @param contentFingerprint lowercase content SHA-256 fingerprint
     * @param size non-negative serialized size in bytes
     */
    public ImportedArtifactMetadata(ImportArtifactDescriptor descriptor, String contentFingerprint, long size) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.contentFingerprint = Preconditions.requireSha256(contentFingerprint, "contentFingerprint");
        if (size < 0L) {
            throw new IllegalArgumentException("size must not be negative: " + size);
        }
        this.size = size;
    }

    /**
     * Returns the deterministic importer-local identity.
     *
     * @return deterministic importer-local identity
     */
    public String identity() {
        return descriptor.identity();
    }

    /**
     * Returns the logical artifact description.
     *
     * @return logical artifact description
     */
    public ImportArtifactDescriptor descriptor() {
        return descriptor;
    }

    /**
     * Returns the lowercase content SHA-256 fingerprint.
     *
     * @return lowercase content SHA-256 fingerprint
     */
    public String contentFingerprint() {
        return contentFingerprint;
    }

    /**
     * Returns the serialized size in bytes.
     *
     * @return serialized size in bytes
     */
    public long size() {
        return size;
    }
}
