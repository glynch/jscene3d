/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import java.io.IOException;
import java.io.InputStream;

/** Owned read handle retaining one immutable published import generation. */
public interface ImportedArtifact extends AutoCloseable {
    /**
     * Returns immutable artifact metadata.
     *
     * @return artifact metadata
     * @throws IllegalStateException if this handle is closed
     */
    ImportedArtifactMetadata metadata();

    /**
     * Opens a new stream over immutable serialized content.
     *
     * @return caller-owned content stream
     * @throws IllegalStateException if this handle is closed
     * @throws IOException if content cannot be opened
     */
    InputStream openStream() throws IOException;

    /**
     * Returns whether this artifact handle has been closed.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();

    /** Releases this generation read handle idempotently. */
    @Override
    void close();
}
