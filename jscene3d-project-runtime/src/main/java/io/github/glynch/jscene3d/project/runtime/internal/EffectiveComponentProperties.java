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

/** Merges descriptor defaults, authored properties, and placement contract overrides deterministically. */
final class EffectiveComponentProperties {
    /** Prevents construction of this stateless implementation helper. */
    private EffectiveComponentProperties() {
        throw new AssertionError("EffectiveComponentProperties cannot be instantiated");
    }

    /** Returns immutable effective values in descriptor declaration order. */
    static Map<PropertyId, ProjectValue> merge(
            ComponentTypeDescriptor descriptor,
            ComponentDefinition definition,
            Map<PropertyId, ProjectValue> overrides) {
        Map<PropertyId, ProjectValue> result = new LinkedHashMap<>();
        for (Map.Entry<PropertyId, PropertyDescriptor> property :
                descriptor.properties().entrySet()) {
            PropertyId id = property.getKey();
            if (overrides.containsKey(id)) {
                result.put(id, Objects.requireNonNull(overrides.get(id), "override"));
            } else if (definition.properties().containsKey(id)) {
                result.put(id, Objects.requireNonNull(definition.properties().get(id), "authored property"));
            } else {
                property.getValue().defaultValue().ifPresent(value -> result.put(id, value));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
