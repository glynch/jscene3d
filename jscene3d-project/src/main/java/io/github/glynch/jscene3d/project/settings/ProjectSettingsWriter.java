/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/** Writes validated project settings as deterministic JSON. */
public final class ProjectSettingsWriter {
    private static final ObjectWriter JSON = new ObjectMapper(JsonFactory.builder()
                    .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                    .build())
            .writerWithDefaultPrettyPrinter();

    /** Creates a stateless project-settings writer. */
    public ProjectSettingsWriter() {
        super();
    }

    /**
     * Writes one complete settings document without closing the caller-owned output.
     *
     * @param output caller-owned JSON destination
     * @param settings validated project settings
     * @throws IOException when the document cannot be encoded or written
     */
    public void write(OutputStream output, ProjectSettings settings) throws IOException {
        OutputStream destination = Objects.requireNonNull(output, "output");
        JSON.writeValue(destination, Objects.requireNonNull(settings, "settings"));
        destination.write('\n');
    }
}
