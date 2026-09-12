/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies portable project settings and safe cache-path validation. */
class ProjectSettingsLoaderTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void suppliesDefaultCacheWhenSettingsAreAbsent() {
        ProjectSettingsLoadResult result = new ProjectSettingsLoader().load(temporaryDirectory);

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.settings()).contains(ProjectSettings.defaults());
        assertThat(result.settings().orElseThrow().resolveCache(temporaryDirectory))
                .isEqualTo(temporaryDirectory.resolve(".jscene3d/cache").toAbsolutePath());
    }

    @Test
    void loadsPortableCacheLocation() throws IOException {
        writeSettings("""
                {
                  "$schema": "https://jscene3d.org/schemas/project-settings-1.json",
                  "schemaVersion": 1,
                  "cache": {"location": ".cache/jscene3d"}
                }
                """);

        ProjectSettingsLoadResult result = new ProjectSettingsLoader().load(temporaryDirectory);

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.settings().orElseThrow().cacheLocation()).isEqualTo(Path.of(".cache/jscene3d"));
    }

    @Test
    void rejectsCacheLocationOutsideProject() throws IOException {
        writeSettings("""
                {"schemaVersion": 1, "cache": {"location": "../shared-cache"}}
                """);

        ProjectSettingsLoadResult result = new ProjectSettingsLoader().load(temporaryDirectory);

        assertThat(result.settings()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(diagnostic ->
                        assertThat(diagnostic.code()).isEqualTo(ProjectSettingsDiagnosticCode.SETTINGS_INVALID));
    }

    @Test
    void rejectsUnknownSettings() throws IOException {
        writeSettings("""
                {"schemaVersion": 1, "cache": {}, "machinePath": "/machine/cache"}
                """);

        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .isEmpty();
    }

    @Test
    void suppliesDefaultCacheWhenCacheObjectIsAbsent() throws IOException {
        writeSettings("""
                {"schemaVersion": 1}
                """);

        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .contains(ProjectSettings.defaults());
    }

    @Test
    void rejectsMalformedAndUnsupportedDocuments() throws IOException {
        writeSettings("{");
        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .isEmpty();

        writeSettings("""
                {"schemaVersion": 2}
                """);
        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .isEmpty();

        writeSettings("""
                {"schemaVersion": 1, "cache": []}
                """);
        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .isEmpty();

        writeSettings("""
                {"schemaVersion": 1, "cache": {"location": " "}}
                """);
        assertThat(new ProjectSettingsLoader().load(temporaryDirectory).settings())
                .isEmpty();
    }

    @Test
    void reportsUnreadableSettingsPath() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve(ProjectSettings.SETTINGS_NAME));

        ProjectSettingsLoadResult result = new ProjectSettingsLoader().load(temporaryDirectory);

        assertThat(result.settings()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(diagnostic ->
                        assertThat(diagnostic.code()).isEqualTo(ProjectSettingsDiagnosticCode.SETTINGS_READ_FAILED));
    }

    @Test
    void rejectsNonPortableCachePaths() {
        Path empty = Path.of("");
        Path absolute = temporaryDirectory.toAbsolutePath();
        Path parent = Path.of("../cache");

        assertThatThrownBy(() -> new ProjectSettings(empty)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectSettings(absolute)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectSettings(parent)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesResultConsistency() {
        ProjectDiagnostic error = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                ProjectSettingsDiagnosticCode.SETTINGS_INVALID,
                temporaryDirectory.toUri(),
                "",
                Map.of());
        Optional<ProjectSettings> absent = Optional.empty();
        Optional<ProjectSettings> present = Optional.of(ProjectSettings.defaults());
        List<ProjectDiagnostic> noDiagnostics = List.of();
        List<ProjectDiagnostic> errors = List.of(error);

        assertThatThrownBy(() -> new ProjectSettingsLoadResult(absent, noDiagnostics))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectSettingsLoadResult(present, errors))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void packagesVersionOneSchema() throws IOException {
        String resource = "/META-INF/jscene3d/project/project-settings-1.schema.json";

        try (var input = ProjectSettingsLoaderTest.class.getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("\"$id\": \"https://jscene3d.org/schemas/project-settings-1.json\"")
                    .contains("\"schemaVersion\": {\"const\": 1}");
        }
    }

    private void writeSettings(String content) throws IOException {
        Path settings = temporaryDirectory.resolve(ProjectSettings.SETTINGS_NAME);
        Files.createDirectories(settings.getParent());
        Files.writeString(settings, content);
    }
}
