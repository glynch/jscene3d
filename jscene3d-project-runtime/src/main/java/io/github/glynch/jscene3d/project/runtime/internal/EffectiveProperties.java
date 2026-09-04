/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared descriptor-default application for runtime factories. */
final class EffectiveProperties {
    /** Prevents instantiation of this shared property policy. */
    private EffectiveProperties() {
        throw new AssertionError("EffectiveProperties cannot be instantiated");
    }

    /** Applies descriptor defaults before authored values while retaining declaration order. */
    static Map<String, ProjectValue> merge(RegisteredTypeDescriptor descriptor, Map<String, ProjectValue> authored) {
        Map<String, ProjectValue> effective = new LinkedHashMap<>();
        for (PropertyDescriptor property : descriptor.properties().values()) {
            property.defaultValue().ifPresent(value -> effective.put(property.id(), value));
        }
        effective.putAll(authored);
        return Collections.unmodifiableMap(effective);
    }
}
