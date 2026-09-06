/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Effective component values preserving the authored scope of every target-bearing property. */
final class EffectiveComponentProperties {
    private final Map<PropertyId, ScopedProjectValue> scopedValues;
    private final Map<PropertyId, ProjectValue> values;

    /** Copies scoped values and publishes their descriptor-ordered portable projection. */
    private EffectiveComponentProperties(Map<PropertyId, ScopedProjectValue> scopedValues) {
        this.scopedValues = Collections.unmodifiableMap(new LinkedHashMap<>(scopedValues));
        Map<PropertyId, ProjectValue> projected = new LinkedHashMap<>();
        scopedValues.forEach((property, scoped) -> projected.put(property, scoped.value()));
        values = Collections.unmodifiableMap(projected);
    }

    /** Merges defaults, authored values, and contract overrides in descriptor declaration order. */
    static EffectiveComponentProperties merge(
            ComponentTypeDescriptor descriptor,
            ComponentDefinition definition,
            Map<PropertyId, ScopedProjectValue> overrides,
            EntityInstanceScope localScope) {
        Map<PropertyId, ScopedProjectValue> result = new LinkedHashMap<>();
        for (Map.Entry<PropertyId, PropertyDescriptor> property :
                descriptor.properties().entrySet()) {
            PropertyId id = property.getKey();
            if (overrides.containsKey(id)) {
                result.put(id, Objects.requireNonNull(overrides.get(id), "override"));
            } else if (definition.properties().containsKey(id)) {
                ProjectValue value =
                        Objects.requireNonNull(definition.properties().get(id), "authored property");
                result.put(id, new ScopedProjectValue(value, localScope));
            } else {
                property.getValue()
                        .defaultValue()
                        .ifPresent(value -> result.put(id, new ScopedProjectValue(value, localScope)));
            }
        }
        return new EffectiveComponentProperties(result);
    }

    /** Returns portable effective values exposed to the component factory. */
    Map<PropertyId, ProjectValue> values() {
        return values;
    }

    /** Returns one required scoped property for reference binding. */
    ScopedProjectValue required(PropertyId property) {
        ScopedProjectValue value = scopedValues.get(Objects.requireNonNull(property, "property"));
        if (value == null) {
            throw new IllegalArgumentException("component target property is absent: " + property);
        }
        return value;
    }
}
