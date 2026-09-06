/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePositive;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Objects;

/** Immutable authored configuration of one component on an entity. */
public final class ComponentDefinition {
    private final ComponentId id;
    private final ComponentTypeId type;
    private final int typeVersion;
    private final Map<String, ProjectValue> properties;

    /**
     * Creates an authored component definition.
     *
     * @param id stable component identity within its entity
     * @param type registered component type identity
     * @param typeVersion positive configuration-schema version
     * @param properties authored property values in declaration order
     */
    public ComponentDefinition(
            ComponentId id, ComponentTypeId type, int typeVersion, Map<String, ProjectValue> properties) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.typeVersion = requirePositive(typeVersion, "typeVersion");
        this.properties = immutableProjectValues(properties, "properties");
    }

    /**
     * Returns the component identity.
     *
     * @return component identity
     */
    public ComponentId id() {
        return id;
    }

    /**
     * Returns the registered component type identity.
     *
     * @return component type identity
     */
    public ComponentTypeId type() {
        return type;
    }

    /**
     * Returns the configuration-schema version.
     *
     * @return positive type version
     */
    public int typeVersion() {
        return typeVersion;
    }

    /**
     * Returns the immutable authored properties in declaration order.
     *
     * @return authored properties
     */
    public Map<String, ProjectValue> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ComponentDefinition definition
                && typeVersion == definition.typeVersion
                && id.equals(definition.id)
                && type.equals(definition.type)
                && properties.equals(definition.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, typeVersion, properties);
    }

    @Override
    public String toString() {
        return "ComponentDefinition[id=" + id + ", type=" + type + ", typeVersion=" + typeVersion + ", properties="
                + properties + ']';
    }
}
