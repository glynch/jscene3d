/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Portable, generic settings values shared by every editor opening a project workspace.
 *
 * <p>Setting meaning and validation live in declarative setting definitions. This Jackson-bound document preserves
 * unknown extension values instead of requiring a Java field for every contribution.
 *
 * @param schema JSON schema reference written with the settings
 * @param schemaVersion settings format version
 * @param settings stored project-scope values indexed by stable setting key
 */
@JsonPropertyOrder({"$schema", "schemaVersion", "settings"})
public record ProjectSettings(
        @JsonProperty("$schema") String schema,

        @JsonProperty(value = "schemaVersion", required = true)
        int schemaVersion,

        Map<String, Object> settings) {
    /** Conventional project-relative settings filename. */
    public static final String SETTINGS_NAME = ".jscene3d/settings.json";

    /** Schema reference for the current project-settings format. */
    public static final String CURRENT_SCHEMA_URI = "https://jscene3d.org/schemas/project-settings-1.json";

    /** Current project-settings schema version. */
    public static final int SCHEMA_VERSION = 1;

    /** Validates one complete persisted settings document. */
    public ProjectSettings {
        schema = Objects.requireNonNullElse(schema, CURRENT_SCHEMA_URI);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion must be " + SCHEMA_VERSION);
        }
        settings = immutableValues(Objects.requireNonNullElse(settings, Map.of()));
    }

    /** Creates settings with one cache override and the current format envelope. */
    public ProjectSettings(Path cacheLocation) {
        this(
                CURRENT_SCHEMA_URI,
                SCHEMA_VERSION,
                Map.of(
                        CoreProjectSettings.CACHE_LOCATION.value(),
                        Objects.requireNonNull(cacheLocation, "cacheLocation").toString()));
    }

    /** Returns a document containing no explicit overrides. */
    public static ProjectSettings defaults() {
        return new ProjectSettings(CURRENT_SCHEMA_URI, SCHEMA_VERSION, Map.of());
    }

    /** Returns one untyped stored value while preserving unknown extension keys. */
    public Optional<Object> value(String key) {
        return Optional.ofNullable(settings.get(Objects.requireNonNull(key, "key")));
    }

    /** Returns a copy with one complete stored override. */
    public ProjectSettings with(String key, Object value) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(settings);
        updated.put(Objects.requireNonNull(key, "key"), immutableValue(value));
        return new ProjectSettings(schema, schemaVersion, updated);
    }

    /** Returns a copy without one stored override. */
    public ProjectSettings without(String key) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(settings);
        updated.remove(Objects.requireNonNull(key, "key"));
        return new ProjectSettings(schema, schemaVersion, updated);
    }

    /** Compatibility convenience for the built-in cache value. */
    public Path cacheLocation() {
        Object stored = settings.get(CoreProjectSettings.CACHE_LOCATION.value());
        return stored == null
                ? CoreProjectSettings.DEFAULT_CACHE_LOCATION
                : Path.of((String) stored).normalize();
    }

    /** Resolves the built-in cache value beneath one project root. */
    public Path resolveCache(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        Path location = cacheLocation();
        Path resolved = root.resolve(location).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("cache location resolves outside the project workspace");
        }
        return resolved;
    }

    private static Map<String, Object> immutableValues(Map<String, Object> values) {
        LinkedHashMap<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> copied.put(Objects.requireNonNull(key, "settings key"), immutableValue(value)));
        return Collections.unmodifiableMap(copied);
    }

    private static @Nullable Object immutableValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String
                || value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigInteger
                || value instanceof BigDecimal
                || value instanceof Double) {
            return value;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof List<?> list) {
            ArrayList<Object> copied = new ArrayList<>();
            list.forEach(item -> copied.add(immutableValue(item)));
            return Collections.unmodifiableList(copied);
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copied = new LinkedHashMap<>();
            map.forEach((key, item) -> copied.put((String) key, immutableValue(item)));
            return Collections.unmodifiableMap(copied);
        }
        throw new IllegalArgumentException(
                "unsupported JSON setting value: " + value.getClass().getName());
    }
}
