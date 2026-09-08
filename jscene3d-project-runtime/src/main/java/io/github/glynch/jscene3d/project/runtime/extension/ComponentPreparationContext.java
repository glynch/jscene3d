/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.value.ResourceReference;

/** Bounded preparation context for resolving one component's reusable runtime resources. */
public interface ComponentPreparationContext {
    /**
     * Returns the authored component definition being prepared.
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
     * Returns effective authored values available before instance-specific ordinary parameters are supplied.
     *
     * @return immutable effective properties in descriptor declaration order
     */
    ComponentProperties properties();

    /**
     * Resolves and retains one shared immutable resource before simulation.
     *
     * @param <T> required runtime value type
     * @param reference portable resource reference
     * @param valueType required runtime Java type
     * @return shared immutable runtime value
     */
    <T> T resolveResource(ResourceReference reference, Class<T> valueType);
}
