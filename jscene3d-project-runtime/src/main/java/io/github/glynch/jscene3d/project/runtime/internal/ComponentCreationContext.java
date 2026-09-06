/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable bounded factory context for one component construction. */
final class ComponentCreationContext implements ComponentFactoryContext {
    private final Entity owner;
    private final World world;
    private final ComponentDefinition definition;
    private final ComponentTypeDescriptor descriptor;
    private final Map<PropertyId, ProjectValue> properties;
    private final RuntimeResourceLookup resources;

    /** Stores validated component construction values. */
    ComponentCreationContext(
            Entity owner,
            World world,
            ComponentDefinition definition,
            ComponentTypeDescriptor descriptor,
            Map<PropertyId, ProjectValue> properties,
            RuntimeResourceLookup resources) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.world = Objects.requireNonNull(world, "world");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    @Override
    public Entity owner() {
        return owner;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public ComponentDefinition definition() {
        return definition;
    }

    @Override
    public ComponentTypeDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public Map<PropertyId, ProjectValue> properties() {
        return properties;
    }

    @Override
    public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
        return resources.resolveResource(reference, valueType);
    }
}
