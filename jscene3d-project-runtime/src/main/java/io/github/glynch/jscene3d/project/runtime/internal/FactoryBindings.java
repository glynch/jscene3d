/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable factory index used only while runtime extensions register. */
public final class FactoryBindings {
    private final Map<ComponentType, ComponentFactory<?>> components;

    /** Creates an empty composition-time factory index. */
    public FactoryBindings() {
        components = new LinkedHashMap<>();
    }

    /**
     * Adds one uniquely registered component factory.
     *
     * @param type exact component type
     * @param factory trusted component factory
     */
    public void addComponent(ComponentType type, ComponentFactory<?> factory) {
        if (components.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException("component factory is already registered: " + type);
        }
    }

    /**
     * Returns the required component factory.
     *
     * @param type exact component type
     * @param location diagnostic location used when absent
     * @return registered component factory
     */
    public ComponentFactory<?> requireComponent(ComponentType type, String location) {
        ComponentFactory<?> factory = components.get(type);
        if (factory == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_FACTORY_MISSING,
                    "no component factory is registered for " + type,
                    location);
        }
        return factory;
    }
}
