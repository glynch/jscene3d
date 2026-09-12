/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies the shared setting declaration and registry contracts. */
class SettingRegistryTest {
    private static final SettingKey<Path> CACHE = new SettingKey<>("jscene3d.cache.location", Path.class);

    @Test
    void registersAndTypeChecksCoreAndExtensionSettings() {
        SettingDefinition<Path> cache = cacheDefinition();
        SettingKey<Boolean> extensionKey = new SettingKey<>("io.github.glynch.example.enabled", Boolean.class);
        SettingDefinition<Boolean> extension = new SettingDefinition<>(
                "io.github.glynch.example",
                "Example",
                extensionKey,
                SettingValueType.BOOLEAN,
                false,
                "Enabled",
                Optional.of("Enables the example."),
                "General",
                SettingScope.PROJECT,
                10,
                SettingConstraints.NONE);

        SettingRegistry registry = SettingRegistry.of(List.of(cache, extension));

        assertThat(registry.definitions()).containsExactly(cache, extension);
        assertThat(registry.require(CACHE)).isSameAs(cache);
        assertThat(registry.require(extensionKey).validate(true)).isTrue();
    }

    @Test
    void rejectsDuplicateKeysAndInvalidOwnership() {
        SettingDefinition<Path> cache = cacheDefinition();
        List<SettingDefinition<?>> duplicateSettings = List.of(cache, cache);

        assertThatThrownBy(() -> SettingRegistry.of(duplicateSettings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        assertThatThrownBy(SettingRegistryTest::invalidOwnershipDefinition)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void validatesProjectRelativePaths() {
        SettingDefinition<Path> cache = cacheDefinition();

        assertThat(cache.validate("build/../.cache")).isEqualTo(Path.of(".cache"));
        assertThatThrownBy(() -> cache.validate("../shared"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project workspace");
    }

    private static SettingDefinition<Path> cacheDefinition() {
        return new SettingDefinition<>(
                "jscene3d",
                "JScene3D",
                CACHE,
                SettingValueType.PATH,
                Path.of(".jscene3d/cache"),
                "Cache Location",
                Optional.of("Generated project data."),
                "Files",
                SettingScope.PROJECT,
                10,
                SettingConstraints.path(SettingPathKind.DIRECTORY, true));
    }

    private static SettingDefinition<Path> invalidOwnershipDefinition() {
        return new SettingDefinition<>(
                "io.github.glynch.other",
                "Other",
                CACHE,
                SettingValueType.PATH,
                Path.of("cache"),
                "Cache",
                Optional.empty(),
                "Files",
                SettingScope.PROJECT,
                0,
                SettingConstraints.path(SettingPathKind.DIRECTORY, true));
    }
}
