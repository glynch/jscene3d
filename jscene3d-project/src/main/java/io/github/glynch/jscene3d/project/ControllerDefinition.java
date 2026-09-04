/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Project-defined controller attached to one scene node. */
public final class ControllerDefinition {
    private final RegisteredType type;
    private final Map<String, ProjectValue> properties;

    /**
     * Creates an immutable controller definition.
     *
     * @param type registered controller type
     * @param properties editable controller properties
     */
    public ControllerDefinition(RegisteredType type, Map<String, ProjectValue> properties) {
        this.type = Objects.requireNonNull(type, "type");
        this.properties = copyValues(properties);
    }

    /**
     * Returns the registered controller type.
     *
     * @return controller type
     */
    public RegisteredType type() {
        return type;
    }

    /**
     * Returns editable controller properties in source order.
     *
     * @return immutable ordered properties
     */
    public Map<String, ProjectValue> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ControllerDefinition definition
                && type.equals(definition.type)
                && properties.equals(definition.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties);
    }

    @Override
    public String toString() {
        return "ControllerDefinition[type=" + type + ", properties=" + properties + ']';
    }

    /** Copies project values while preserving source order. */
    private static Map<String, ProjectValue> copyValues(Map<String, ProjectValue> values) {
        Objects.requireNonNull(values, "properties");
        Map<String, ProjectValue> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> copied.put(
                Objects.requireNonNull(key, "property name"), Objects.requireNonNull(value, "property value")));
        return Collections.unmodifiableMap(copied);
    }
}
