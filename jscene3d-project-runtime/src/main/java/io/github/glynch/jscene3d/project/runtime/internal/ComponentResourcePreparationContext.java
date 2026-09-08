/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Objects;

/** One-use component preparation context backed by a world resource transaction. */
final class ComponentResourcePreparationContext implements ComponentPreparationContext {
    private final ComponentDefinition definition;
    private final ComponentTypeDescriptor descriptor;
    private final ComponentProperties properties;
    private final WorldResources.Preparation resources;
    private final String location;
    private boolean active = true;

    /** Stores one validated component plan and resource transaction. */
    ComponentResourcePreparationContext(
            ComponentDefinition definition,
            ComponentTypeDescriptor descriptor,
            EffectiveComponentProperties properties,
            WorldResources.Preparation resources,
            String location) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.properties = new ComponentProperties(
                Objects.requireNonNull(properties, "properties").values());
        this.resources = Objects.requireNonNull(resources, "resources");
        this.location = Objects.requireNonNull(location, "location");
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
        if (!active) {
            throw new IllegalStateException("component preparation context has expired");
        }
        return resources.resolve(reference, valueType, location);
    }

    /** Expires resource preparation after the registered factory returns. */
    void expire() {
        active = false;
    }
}
