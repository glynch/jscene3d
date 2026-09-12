/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the distinct project-settings reading, writing, loading, and saving responsibilities. */
class ProjectSettingsPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void readerBindsSettingsDirectlyWithJacksonDefaults() throws IOException {
        ProjectSettings settings = read("""
                {"schemaVersion":1,"settings":{"jscene3d.cache.location":"build/../.cache/jscene3d"}}
                """);

        assertThat(settings.schema()).isEqualTo(ProjectSettings.CURRENT_SCHEMA_URI);
        assertThat(settings.schemaVersion()).isEqualTo(ProjectSettings.SCHEMA_VERSION);
        assertThat(settings.value(CoreProjectSettings.CACHE_LOCATION.value())).contains("build/../.cache/jscene3d");
    }

    @Test
    void readerRetainsStrictJacksonChecks() {
        assertThatThrownBy(() -> read("""
                {"schemaVersion":1,"settings":{},"unknown":true}
                """)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> read("""
                {"schemaVersion":1,"schemaVersion":1}
                """)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> read("""
                {"schemaVersion":1} {}
                """)).isInstanceOf(IOException.class);
    }

    @Test
    void writerProducesCanonicalRoundTrippableJson() throws IOException {
        ProjectSettings expected = new ProjectSettings(Path.of(".cache/jscene3d"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new ProjectSettingsWriter().write(output, expected);

        String json = output.toString(StandardCharsets.UTF_8);
        assertThat(json)
                .startsWith("{\n")
                .contains("\"$schema\" : \"" + ProjectSettings.CURRENT_SCHEMA_URI + "\"")
                .contains("\"schemaVersion\" : 1")
                .contains("\"jscene3d.cache.location\" : \".cache/jscene3d\"")
                .endsWith("\n");
        assertThat(read(json)).isEqualTo(expected);
    }

    @Test
    void saverAtomicallyReplacesTheConventionalSettingsFile() throws IOException {
        ProjectSettings first = new ProjectSettings(Path.of("first-cache"));
        ProjectSettings second = new ProjectSettings(Path.of("second-cache"));
        ProjectSettingsSaver saver = new ProjectSettingsSaver();

        saver.save(temporaryDirectory, first);
        saver.save(temporaryDirectory, second);

        Path target = temporaryDirectory.resolve(ProjectSettings.SETTINGS_NAME);
        assertThat(Files.readString(target)).contains("\"jscene3d.cache.location\" : \"second-cache\"");
        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .contains(second);
        try (var files = Files.list(target.getParent())) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("settings.json");
        }
    }

    @Test
    void saverCleansTemporaryFileWhenAtomicReplacementFails() throws IOException {
        Path target = temporaryDirectory.resolve(ProjectSettings.SETTINGS_NAME);
        Files.createDirectories(target);
        Files.writeString(target.resolve("occupied"), "occupied");
        ProjectSettingsSaver saver = new ProjectSettingsSaver();
        ProjectSettings settings = ProjectSettings.defaults();

        assertThatThrownBy(() -> saver.save(temporaryDirectory, settings)).isInstanceOf(IOException.class);

        try (var files = Files.list(target.getParent())) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("settings.json");
        }
    }

    private static ProjectSettings read(String json) throws IOException {
        return new ProjectSettingsReader().read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
