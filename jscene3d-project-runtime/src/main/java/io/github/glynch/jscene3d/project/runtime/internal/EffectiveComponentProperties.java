/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionValue;
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

    /** Adapts already effective safe values to their live runtime scope indexes. */
    static EffectiveComponentProperties from(
            Map<PropertyId, CompositionValue> planned, RuntimeCompositionScopes scopes) {
        Map<PropertyId, ScopedProjectValue> result = new LinkedHashMap<>();
        planned.forEach((property, value) ->
                result.put(property, new ScopedProjectValue(value.value(), scopes.require(value.authoredScope()))));
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
