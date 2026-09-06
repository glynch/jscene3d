/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Map;

/**
 * Bounded construction context supplied to one registered component factory.
 *
 * <p>The eventual world composer owns implementations of this interface. The context exposes validated effective
 * configuration and shared runtime-resource resolution without exposing mutable composer internals. Resource
 * resolution is available only during the factory invocation; the world retains acquired leases for the owning entity
 * and supplies the resource value without transferring cleanup responsibility to the component. Target-valued
 * properties remain authored values during construction; components resolve them later through {@link
 * ComponentReferenceBinder} after every factory has completed.
 */
public interface ComponentFactoryContext {
    /**
     * Returns the completely allocated entity which will own the component.
     *
     * @return owning entity
     */
    Entity owner();

    /**
     * Returns the completely allocated but inactive owning world.
     *
     * @return owning world
     */
    World world();

    /**
     * Returns a retained spawn capability restricted to direct children of this component's owner.
     *
     * @return owner-scoped spawn target
     */
    SpawnTarget spawnTarget();

    /**
     * Returns the authored component definition being constructed.
     *
     * @return authored component definition
     */
    ComponentDefinition definition();

    /**
     * Returns the exact safe descriptor used to validate the definition.
     *
     * @return component type descriptor
     */
    ComponentTypeDescriptor descriptor();

    /**
     * Returns authored values merged over descriptor defaults.
     *
     * @return immutable effective properties in descriptor declaration order
     */
    Map<PropertyId, ProjectValue> properties();

    /**
     * Resolves one shared immutable resource and attributes its lease to the owning entity.
     *
     * <p>Repeated resolution of the same reference within a world returns the identical value. The world releases the
     * lease after the final entity using it is destroyed, or during world cleanup. Components must not close the shared
     * value. This operation is valid only while the component factory is executing.
     *
     * @param <T> required runtime value type
     * @param reference portable resource reference
     * @param valueType required runtime Java type
     * @return shared immutable runtime value
     * @throws IllegalArgumentException if an argument is invalid
     * @throws IllegalStateException if the context has expired or acquisition fails
     */
    <T> T resolveResource(ResourceReference reference, Class<T> valueType);
}
