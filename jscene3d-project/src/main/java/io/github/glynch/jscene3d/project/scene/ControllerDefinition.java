/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
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
        this.properties = immutableProjectValues(properties, "properties");
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
}
