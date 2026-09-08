/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Objects;

/** Immutable bounded factory context for one component construction. */
final class ComponentCreationContext implements ComponentFactoryContext {
    private final Entity owner;
    private final InternalWorld world;
    private final ComponentDefinition definition;
    private final ComponentTypeDescriptor descriptor;
    private final ComponentProperties properties;
    private final String location;
    private final ResourceAccess resourceAccess;
    private boolean active = true;

    /** Stores validated component construction values. */
    ComponentCreationContext(
            Entity owner,
            InternalWorld world,
            ComponentDefinition definition,
            ComponentTypeDescriptor descriptor,
            EffectiveComponentProperties properties,
            String location,
            ResourceAccess resourceAccess) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.world = Objects.requireNonNull(world, "world");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.properties = new ComponentProperties(properties.values());
        this.location = Objects.requireNonNull(location, "location");
        this.resourceAccess = Objects.requireNonNull(resourceAccess, "resourceAccess");
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
    public SpawnTarget spawnTarget() {
        requireActive();
        return world.spawnTarget(owner);
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
    public ComponentProperties properties() {
        return properties;
    }

    @Override
    public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
        requireActive();
        return resourceAccess == ResourceAccess.PREPARED_ONLY
                ? world.resolvePreparedResource((InternalEntity) owner, reference, valueType, location)
                : world.resolveResource((InternalEntity) owner, reference, valueType, location);
    }

    /** Expires construction-only resource resolution after the factory returns. */
    void expire() {
        active = false;
    }

    /** Requires factory-only operations to remain inside the factory invocation. */
    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("component factory context has expired");
        }
    }

    /** Closed resource-acquisition policies for initial and prepared construction. */
    enum ResourceAccess {
        /** Initial composition may acquire resources synchronously. */
        ACQUIRE,
        /** Runtime spawning may use only resources retained during preparation. */
        PREPARED_ONLY
    }
}
