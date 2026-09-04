/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.value.ResourceReference;

/** Resolves shared runtime values from portable project-resource references. */
public interface RuntimeResourceLookup {
    /**
     * Resolves one resource and requires its runtime value to have the expected Java type.
     *
     * <p>Repeated resolution of the same canonical project resource returns the identical object.
     * Resolution is synchronous and may invoke nested resource factories. The runtime owns created
     * values and closes values implementing {@link AutoCloseable} after scene objects close.
     *
     * @param <T> expected runtime value type
     * @param reference portable resource reference
     * @param valueType required runtime Java type
     * @return shared runtime resource value
     * @throws IllegalArgumentException if an argument is invalid
     * @throws IllegalStateException if resolution fails or the value has the wrong type
     */
    <T> T resolveResource(ResourceReference reference, Class<T> valueType);
}
