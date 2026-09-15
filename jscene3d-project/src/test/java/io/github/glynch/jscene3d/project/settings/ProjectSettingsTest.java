/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies direct settings-document values independently of JSON persistence. */
final class ProjectSettingsTest {
    @Test
    void suppliesFormatDefaultsAndSupportsFunctionalUpdates() {
        ProjectSettings defaults = ProjectSettings.defaults();
        ProjectSettings updated = defaults.with("example.enabled", true).with("example.label", "Label");
        int incompatibleVersion = 2;
        Map<String, Object> noSettings = Map.of();

        assertThat(defaults.schema()).isEqualTo(ProjectSettings.CURRENT_SCHEMA_URI);
        assertThat(defaults.settings()).isEmpty();
        assertThat(updated.value("example.enabled")).contains(true);
        assertThat(updated.value("example.label")).contains("Label");
        assertThat(updated.without("example.enabled").settings()).containsOnlyKeys("example.label");
        assertThatThrownBy(
                        () -> new ProjectSettings(ProjectSettings.CURRENT_SCHEMA_URI, incompatibleVersion, noSettings))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copiesSupportedJsonValuesWithoutRetainingMutableInputs() {
        List<Object> mutableList = new ArrayList<>(List.of("entry", BigInteger.TEN));
        Map<String, Object> mutableObject = new LinkedHashMap<>();
        mutableObject.put("nested", mutableList);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("text", "value");
        values.put("boolean", true);
        values.put("integer", 1);
        values.put("long", 2L);
        values.put("bigInteger", BigInteger.TEN);
        values.put("decimal", BigDecimal.TEN);
        values.put("double", 1.5D);
        values.put("float", 2.5F);
        values.put("object", mutableObject);

        ProjectSettings settings =
                new ProjectSettings(ProjectSettings.CURRENT_SCHEMA_URI, ProjectSettings.SCHEMA_VERSION, values);
        mutableList.add("later");
        mutableObject.put("later", false);
        values.put("later", false);

        assertThat(settings.settings()).doesNotContainKey("later").containsEntry("float", new BigDecimal("2.5"));
        Map<?, ?> object = (Map<?, ?>) settings.value("object").orElseThrow();
        Map<String, Object> stored = settings.settings();
        Map<String, Object> unsupported = Map.of("unsupported", new Object());
        assertThat(object.get("nested")).isEqualTo(List.of("entry", BigInteger.TEN));
        assertThatThrownBy(() -> stored.put("another", true)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ProjectSettings(
                        ProjectSettings.CURRENT_SCHEMA_URI, ProjectSettings.SCHEMA_VERSION, unsupported))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesDefaultAndConfiguredCacheLocationsInsideTheWorkspace(@TempDir Path projectRoot) {
        ProjectSettings defaults = ProjectSettings.defaults();
        ProjectSettings configured = new ProjectSettings(Path.of("build/../.cache"));
        ProjectSettings outside = new ProjectSettings(Path.of("../outside"));

        assertThat(defaults.cacheLocation()).isEqualTo(CoreProjectSettings.DEFAULT_CACHE_LOCATION);
        assertThat(defaults.resolveCache(projectRoot)).isEqualTo(projectRoot.resolve(".jscene3d/cache"));
        assertThat(configured.cacheLocation()).isEqualTo(Path.of(".cache"));
        assertThat(configured.resolveCache(projectRoot)).isEqualTo(projectRoot.resolve(".cache"));
        assertThatThrownBy(() -> outside.resolveCache(projectRoot)).isInstanceOf(IllegalArgumentException.class);
    }
}
