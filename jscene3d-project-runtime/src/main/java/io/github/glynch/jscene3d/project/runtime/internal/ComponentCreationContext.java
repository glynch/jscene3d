/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Map;
import java.util.Objects;

/** Immutable bounded factory context for one component construction. */
final class ComponentCreationContext implements ComponentFactoryContext {
    private final Entity owner;
    private final InternalWorld world;
    private final ComponentDefinition definition;
    private final ComponentTypeDescriptor descriptor;
    private final Map<PropertyId, ProjectValue> properties;
    private final String location;
    private boolean active = true;

    /** Stores validated component construction values. */
    ComponentCreationContext(
            Entity owner,
            InternalWorld world,
            ComponentDefinition definition,
            ComponentTypeDescriptor descriptor,
            EffectiveComponentProperties properties,
            String location) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.world = Objects.requireNonNull(world, "world");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.properties = properties.values();
        this.location = Objects.requireNonNull(location, "location");
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
        if (!active) {
            throw new IllegalStateException("component factory context has expired");
        }
        return world.resolveResource((InternalEntity) owner, reference, valueType, location);
    }

    /** Expires construction-only resource resolution after the factory returns. */
    void expire() {
        active = false;
    }
}
