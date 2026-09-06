/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.ComponentType;

/** Construction-time registry binding component descriptors to trusted factories. */
public interface ComponentFactoryRegistry {
    /**
     * Registers the construction adapter for one component type.
     *
     * @param type exact descriptor-declared component type
     * @param factory trusted component factory
     */
    void register(ComponentType type, ComponentFactory<?> factory);
}
