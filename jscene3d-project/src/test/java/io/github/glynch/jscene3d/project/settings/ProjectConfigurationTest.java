/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.configuration.SettingRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies registry-based effective configuration and atomic project persistence. */
class ProjectConfigurationTest {
    @TempDir
    private Path projectRoot;

    @Test
    void resolvesDefaultsOverridesAndProjectPaths() throws IOException {
        ProjectConfiguration configuration = configuration(ProjectSettings.defaults());

        assertThat(configuration.get(CoreProjectSettings.CACHE_LOCATION))
                .isEqualTo(CoreProjectSettings.DEFAULT_CACHE_LOCATION);
        assertThat(configuration.resolve(CoreProjectSettings.CACHE_LOCATION))
                .isEqualTo(projectRoot.resolve(".jscene3d/cache").toAbsolutePath());
        assertThat(configuration.isOverridden(CoreProjectSettings.CACHE_LOCATION))
                .isFalse();

        configuration.update(CoreProjectSettings.CACHE_LOCATION, Path.of(".cache/imports"));

        assertThat(configuration.get(CoreProjectSettings.CACHE_LOCATION)).isEqualTo(Path.of(".cache/imports"));
        assertThat(configuration.isOverridden(CoreProjectSettings.CACHE_LOCATION))
                .isTrue();
        assertThat(Files.readString(projectRoot.resolve(ProjectSettings.SETTINGS_NAME)))
                .contains("\"jscene3d.cache.location\" : \".cache/imports\"");
    }

    @Test
    void preservesUnknownValuesAndFallsBackFromInvalidKnownValues() {
        ProjectSettings settings = new ProjectSettings(
                ProjectSettings.CURRENT_SCHEMA_URI,
                ProjectSettings.SCHEMA_VERSION,
                Map.of(CoreProjectSettings.CACHE_LOCATION.value(), "../outside", "missing.extension.option", true));

        ProjectConfiguration configuration = configuration(settings);

        assertThat(configuration.get(CoreProjectSettings.CACHE_LOCATION))
                .isEqualTo(CoreProjectSettings.DEFAULT_CACHE_LOCATION);
        assertThat(configuration.document().settings()).containsEntry("missing.extension.option", true);
        assertThat(configuration.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactlyInAnyOrder("project.settings.value", "project.settings.unknown");
    }

    @Test
    void resetRemovesTheOverrideAndRestoresTheDefault() throws IOException {
        ProjectConfiguration configuration = configuration(new ProjectSettings(Path.of("custom-cache")));

        configuration.reset(CoreProjectSettings.CACHE_LOCATION);

        assertThat(configuration.get(CoreProjectSettings.CACHE_LOCATION))
                .isEqualTo(CoreProjectSettings.DEFAULT_CACHE_LOCATION);
        assertThat(configuration.document().settings()).isEmpty();
    }

    private ProjectConfiguration configuration(ProjectSettings settings) {
        return new ProjectConfiguration(projectRoot, SettingRegistry.of(CoreProjectSettings.definitions()), settings);
    }
}
