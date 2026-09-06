/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import java.io.IOException;

/**
 * Converts one validated typed resource definition into an owned immutable-use runtime value.
 *
 * @param <T> owned runtime value type produced by this loader
 */
public interface RuntimeResourceLoader<T extends AutoCloseable> {
    /**
     * Returns the exact resource definition type accepted by this loader.
     *
     * @return registered resource type
     */
    RegisteredType type();

    /**
     * Returns the runtime value class produced by this loader.
     *
     * @return runtime value class
     */
    Class<T> valueType();

    /**
     * Creates one independently owned runtime value.
     *
     * <p>The caller owns the returned value and closes it after the last lease is released. The loader owns and must
     * close every stream it opens through {@code content}.
     *
     * @param definition structurally and catalog-validated resource definition matching {@link #type()}
     * @param content resolver for payload references in the definition
     * @return new open runtime value
     * @throws IOException when referenced content cannot be read
     * @throws IllegalArgumentException when properties or payload content violate the resource-type contract
     */
    T load(ResourceDefinition definition, ResourceContent content) throws IOException;
}
