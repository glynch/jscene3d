/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/**
 * Trusted construction adapter for one registered component type.
 *
 * <p>Runtime components are ordinary non-null Java objects; they need not extend an engine base class. The world
 * composer invokes a factory only after descriptor validation and graph allocation. Throwing aborts transactional
 * construction of the complete definition instance.
 *
 * @param <T> runtime component implementation type
 */
@FunctionalInterface
public interface ComponentFactory<T> {
    /**
     * Creates one runtime component.
     *
     * @param context validated bounded construction context
     * @return newly created non-null runtime component
     */
    T create(ComponentFactoryContext context);
}
