/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.configuration.SettingConstraints;
import io.github.glynch.jscene3d.configuration.SettingDefinition;
import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.configuration.SettingPathKind;
import io.github.glynch.jscene3d.configuration.SettingScope;
import io.github.glynch.jscene3d.configuration.SettingValueType;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Declarative built-in project settings registered through the same model as extension settings. */
public final class CoreProjectSettings {
    /** Stable built-in cache-location key. */
    public static final SettingKey<Path> CACHE_LOCATION = new SettingKey<>("jscene3d.cache.location", Path.class);

    /** Conventional project-relative cache location. */
    public static final Path DEFAULT_CACHE_LOCATION = Path.of(".jscene3d/cache");

    private static final List<SettingDefinition<?>> DEFINITIONS = List.of(new SettingDefinition<>(
            "jscene3d",
            "JScene3D",
            CACHE_LOCATION,
            SettingValueType.PATH,
            DEFAULT_CACHE_LOCATION,
            "Cache Location",
            Optional.of("Directory used for generated and imported project data."),
            "Files",
            SettingScope.PROJECT,
            10,
            SettingConstraints.path(SettingPathKind.DIRECTORY, true)));

    private CoreProjectSettings() {}

    /** Returns built-in declarations in stable presentation order. */
    public static List<SettingDefinition<?>> definitions() {
        return DEFINITIONS;
    }
}
