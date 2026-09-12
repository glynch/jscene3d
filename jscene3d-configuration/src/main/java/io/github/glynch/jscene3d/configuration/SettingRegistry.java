/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable index of validated core and extension setting declarations. */
public final class SettingRegistry {
    private final Map<String, SettingDefinition<?>> definitions;

    private SettingRegistry(Collection<SettingDefinition<?>> contributions) {
        LinkedHashMap<String, SettingDefinition<?>> indexed = new LinkedHashMap<>();
        for (SettingDefinition<?> definition : contributions) {
            SettingDefinition<?> declared = Objects.requireNonNull(definition, "contributions entry");
            if (indexed.putIfAbsent(declared.key().value(), declared) != null) {
                throw new IllegalArgumentException(
                        "setting key is already registered: " + declared.key().value());
            }
        }
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    /** Creates a deterministic registry in contribution order. */
    public static SettingRegistry of(Collection<SettingDefinition<?>> contributions) {
        return new SettingRegistry(Objects.requireNonNull(contributions, "contributions"));
    }

    /** Returns declarations in contribution order. */
    public List<SettingDefinition<?>> definitions() {
        return List.copyOf(definitions.values());
    }

    /** Finds a declaration by its stable textual key. */
    public Optional<SettingDefinition<?>> find(String key) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(key, "key")));
    }

    /** Returns and type-checks the declaration for one typed key. */
    public <T> SettingDefinition<T> require(SettingKey<T> key) {
        SettingKey<T> requested = Objects.requireNonNull(key, "key");
        SettingDefinition<?> definition = find(requested.value())
                .orElseThrow(() -> new IllegalArgumentException("setting is not registered: " + requested.value()));
        if (!definition.key().valueClass().equals(requested.valueClass())) {
            throw new IllegalArgumentException("setting key uses the wrong Java type: " + requested.value());
        }
        return cast(definition);
    }

    @SuppressWarnings("unchecked")
    private static <T> SettingDefinition<T> cast(SettingDefinition<?> definition) {
        return (SettingDefinition<T>) definition;
    }
}
