/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Reads one specific JSON source into validated project settings. */
public final class ProjectSettingsReader {
    private final ProjectJsonReader jsonReader;

    /** Creates a reader using the shared strict project JSON configuration. */
    public ProjectSettingsReader() {
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Reads one settings document without closing the caller-owned input.
     *
     * @param input caller-owned JSON input
     * @return validated project settings
     * @throws IOException when the document cannot be read or mapped
     */
    public ProjectSettings read(InputStream input) throws IOException {
        return jsonReader.read(Objects.requireNonNull(input, "input"), ProjectSettings.class);
    }
}
