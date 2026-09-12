/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.project.internal.AtomicProjectFileWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Saves project settings to their conventional workspace location using atomic replacement. */
public final class ProjectSettingsSaver {
    private final ProjectSettingsWriter writer;

    /** Creates a saver backed by the canonical project-settings writer. */
    public ProjectSettingsSaver() {
        writer = new ProjectSettingsWriter();
    }

    /**
     * Saves one effective settings value beneath the supplied project root.
     *
     * @param projectRoot project workspace root
     * @param settings validated project settings
     * @throws IOException when the complete settings file cannot be persisted atomically
     */
    public void save(Path projectRoot, ProjectSettings settings) throws IOException {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(output, Objects.requireNonNull(settings, "settings"));
        AtomicProjectFileWriter.write(root.resolve(ProjectSettings.SETTINGS_NAME), output.toByteArray());
    }
}
