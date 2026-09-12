/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.util.Objects;
import java.util.Optional;

/**
 * Complete declarative metadata from which the editor validates, presents, and persists one setting.
 *
 * @param owner stable identity of the contributing core module or extension
 * @param ownerDisplayName user-facing contributor name
 * @param key stable typed setting key
 * @param valueType built-in value family
 * @param defaultValue effective value when no scope supplies an override
 * @param displayName user-facing setting name
 * @param description optional user-facing explanation
 * @param category user-facing grouping within the contributor
 * @param scope persistence scope
 * @param order relative presentation order within the category
 * @param constraints validation and generated-editor hints
 * @param <T> exposed setting value type
 */
public record SettingDefinition<T>(
        String owner,
        String ownerDisplayName,
        SettingKey<T> key,
        SettingValueType valueType,
        T defaultValue,
        String displayName,
        Optional<String> description,
        String category,
        SettingScope scope,
        int order,
        SettingConstraints constraints) {
    /** Copies and validates one complete declaration. */
    public SettingDefinition {
        requireNonBlank(owner, "owner");
        requireNonBlank(ownerDisplayName, "ownerDisplayName");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueType, "valueType");
        if (!key.value().startsWith(owner + '.')) {
            throw new IllegalArgumentException("setting key does not belong to contributor " + owner);
        }
        if (!key.valueClass().equals(valueType.valueClass())) {
            throw new IllegalArgumentException("setting key Java type does not match its declared value type");
        }
        Object converted = valueType.convert(Objects.requireNonNull(defaultValue, "defaultValue"));
        Objects.requireNonNull(constraints, "constraints");
        constraints.validate(valueType, converted);
        requireNonBlank(displayName, "displayName");
        description = Objects.requireNonNull(description, "description")
                .map(String::strip)
                .filter(value -> !value.isEmpty());
        requireNonBlank(category, "category");
        Objects.requireNonNull(scope, "scope");
    }

    /** Converts and validates one stored or user-supplied value. */
    public T validate(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("setting value must not be null");
        }
        Object converted = valueType.convert(value);
        constraints.validate(valueType, converted);
        return key.valueClass().cast(converted);
    }

    private static void requireNonBlank(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
