/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;

/**
 * Bounded construction context supplied to one registered component factory.
 *
 * <p>The eventual world composer owns implementations of this interface. The context exposes validated effective
 * configuration and shared runtime-resource lookup without exposing mutable composer internals.
 */
public interface ComponentFactoryContext extends RuntimeResourceLookup {
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
}
