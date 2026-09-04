/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/** Creates the owned runtime value for one registered resource type. */
@FunctionalInterface
public interface ResourceFactory {
    /**
     * Creates one resource value without starting scene lifecycle callbacks.
     *
     * <p>The runtime owns the returned value. A value implementing {@link AutoCloseable} is closed
     * exactly once after every scene object or dependent resource has released it.
     *
     * @param context bounded resource-creation context
     * @return newly owned non-null resource value
     */
    Object create(ResourceFactoryContext context);
}
