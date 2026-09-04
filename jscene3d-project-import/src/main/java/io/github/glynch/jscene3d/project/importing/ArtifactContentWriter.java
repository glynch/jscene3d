/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import java.io.IOException;
import java.io.OutputStream;

/** Writes one artifact directly to engine-owned staging storage. */
@FunctionalInterface
public interface ArtifactContentWriter {
    /**
     * Writes complete artifact content without closing the supplied stream.
     *
     * @param output engine-owned staging stream
     * @throws IOException if artifact generation or writing fails
     */
    void write(OutputStream output) throws IOException;
}
