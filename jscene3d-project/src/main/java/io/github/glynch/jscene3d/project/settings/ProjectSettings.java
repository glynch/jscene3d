/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Portable settings shared by every editor opening a project workspace.
 *
 * <p>The record is the Jackson-bound settings model. Its compact constructor owns semantic validation while the
 * constructor supplies defaults for optional JSON properties.
 *
 * @param schema JSON schema reference written with the settings
 * @param schemaVersion settings format version
 * @param cache cache configuration
 */
@JsonPropertyOrder({"$schema", "schemaVersion", "cache"})
public record ProjectSettings(
        @JsonProperty("$schema") String schema,

        @JsonProperty(value = "schemaVersion", required = true)
        int schemaVersion,

        CacheSettings cache) {
    /** Conventional project-relative settings filename. */
    public static final String SETTINGS_NAME = ".jscene3d/settings.json";

    /** Schema reference for the current project-settings format. */
    public static final String CURRENT_SCHEMA_URI = "https://jscene3d.org/schemas/project-settings-1.json";

    /** Current project-settings schema version. */
    public static final int SCHEMA_VERSION = 1;

    /** Conventional editor cache location used when the project does not override it. */
    public static final Path DEFAULT_CACHE_LOCATION = Path.of(".jscene3d/cache");

    /** Windows drive-root syntax, which is absolute even when parsed on another operating system. */
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:/.*");

    /** Validates one complete effective settings value. */
    public ProjectSettings {
        schema = Objects.requireNonNullElse(schema, CURRENT_SCHEMA_URI);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion must be " + SCHEMA_VERSION);
        }
        cache = Objects.requireNonNullElseGet(cache, CacheSettings::defaults);
    }

    /**
     * Creates settings with one cache override and the current format envelope.
     *
     * @param cacheLocation project-relative cache location
     */
    public ProjectSettings(Path cacheLocation) {
        this(
                CURRENT_SCHEMA_URI,
                SCHEMA_VERSION,
                new CacheSettings(portable(Objects.requireNonNull(cacheLocation, "cacheLocation"))));
    }

    /**
     * Returns settings containing only standard defaults.
     *
     * @return standard project settings
     */
    public static ProjectSettings defaults() {
        return new ProjectSettings(CURRENT_SCHEMA_URI, SCHEMA_VERSION, CacheSettings.defaults());
    }

    /**
     * Returns the normalized project-relative cache location.
     *
     * @return normalized cache location
     */
    public Path cacheLocation() {
        return Path.of(cache.location());
    }

    /**
     * Resolves the configured cache beneath one normalized project root.
     *
     * @param projectRoot project workspace root
     * @return normalized absolute cache path
     */
    public Path resolveCache(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        Path resolved = root.resolve(cacheLocation()).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("cache location resolves outside the project workspace");
        }
        return resolved;
    }

    /** Returns a normalized, forward-slash project-relative location. */
    private static String portable(Path location) {
        String supplied = location.toString().replace(File.separatorChar, '/');
        if (location.isAbsolute() || WINDOWS_ABSOLUTE_PATH.matcher(supplied).matches()) {
            throw new IllegalArgumentException("cache location must be relative to the project workspace");
        }
        String normalized = location.normalize().toString().replace(File.separatorChar, '/');
        if (normalized.isBlank() || normalized.equals("..") || normalized.startsWith("../")) {
            throw new IllegalArgumentException("cache location must remain inside the project workspace");
        }
        return normalized;
    }

    /**
     * Portable cache settings.
     *
     * @param location normalized project-relative cache location using forward slashes
     */
    @JsonPropertyOrder("location")
    public record CacheSettings(String location) {
        /** Validates and normalizes the cache location. */
        public CacheSettings {
            location = portable(location == null ? DEFAULT_CACHE_LOCATION : Path.of(location));
        }

        /** Returns the standard cache configuration. */
        private static CacheSettings defaults() {
            return new CacheSettings(portable(DEFAULT_CACHE_LOCATION));
        }
    }
}
