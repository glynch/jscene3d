/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Immutable, validated definition of one reusable project resource. */
public final class ResourceDefinition {
    private final Path source;
    private final RegisteredType type;
    private final Map<String, ProjectValue> properties;

    /**
     * Creates one validated resource definition.
     *
     * @param source normalized absolute source path
     * @param type exact registered resource type
     * @param properties authored properties in declaration order
     */
    public ResourceDefinition(Path source, RegisteredType type, Map<String, ProjectValue> properties) {
        this.source = requireNormalizedAbsolute(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.properties = immutableProjectValues(properties, "properties");
    }

    /**
     * Returns the canonical source path that identifies this project resource.
     *
     * @return normalized absolute source path
     */
    public Path source() {
        return source;
    }

    /**
     * Returns the registered resource type and definition version.
     *
     * @return registered resource type
     */
    public RegisteredType type() {
        return type;
    }

    /**
     * Returns authored properties in source order.
     *
     * @return immutable authored properties
     */
    public Map<String, ProjectValue> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ResourceDefinition definition
                && source.equals(definition.source)
                && type.equals(definition.type)
                && properties.equals(definition.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, type, properties);
    }

    @Override
    public String toString() {
        return "ResourceDefinition[source=" + source + ", type=" + type + ", properties=" + properties + ']';
    }
}
