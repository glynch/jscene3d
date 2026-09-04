/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** File-backed read handle for one immutable published artifact. */
public final class InternalImportedArtifact implements ImportedArtifact {
    private final ImportedArtifactMetadata metadata;
    private final Path content;
    private boolean closed;

    /**
     * Creates one retained artifact handle.
     *
     * @param metadata immutable artifact metadata
     * @param content validated immutable cache content path
     */
    public InternalImportedArtifact(ImportedArtifactMetadata metadata, Path content) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.content = Objects.requireNonNull(content, "content");
    }

    @Override
    public ImportedArtifactMetadata metadata() {
        requireOpen();
        return metadata;
    }

    @Override
    public InputStream openStream() throws IOException {
        requireOpen();
        return Files.newInputStream(content);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    /** Requires this read handle to remain open. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Imported artifact is closed");
        }
    }
}
