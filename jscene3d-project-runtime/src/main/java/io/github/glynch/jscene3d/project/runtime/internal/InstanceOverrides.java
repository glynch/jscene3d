/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Contract arguments resolved within one distinct definition-instance scope. */
final class InstanceOverrides {
    private static final InstanceOverrides EMPTY = new InstanceOverrides(Map.of(), Map.of());

    private final Map<ComponentTarget, Map<PropertyId, ProjectValue>> components;
    private final Map<EntityId, Map<PropertyId, ProjectValue>> placements;

    /** Stores immutable override indexes. */
    private InstanceOverrides(
            Map<ComponentTarget, Map<PropertyId, ProjectValue>> components,
            Map<EntityId, Map<PropertyId, ProjectValue>> placements) {
        this.components = immutableNestedMap(components);
        this.placements = immutableNestedMap(placements);
    }

    /** Returns the shared empty override scope. */
    static InstanceOverrides empty() {
        return EMPTY;
    }

    /** Resolves supplied public arguments to private component properties or nested placement arguments. */
    static InstanceOverrides resolve(EntityContract contract, Map<PropertyId, ProjectValue> arguments) {
        Objects.requireNonNull(contract, "contract");
        Map<ComponentTarget, Map<PropertyId, ProjectValue>> components = new LinkedHashMap<>();
        Map<EntityId, Map<PropertyId, ProjectValue>> placements = new LinkedHashMap<>();
        for (Map.Entry<PropertyId, ProjectValue> argument : arguments.entrySet()) {
            PropertyTarget target = target(contract, argument.getKey());
            if (target.component().isPresent()) {
                ComponentTarget component =
                        new ComponentTarget(target.entity(), target.component().orElseThrow());
                components
                        .computeIfAbsent(component, ignored -> new LinkedHashMap<>())
                        .put(target.property(), argument.getValue());
            } else {
                placements
                        .computeIfAbsent(target.entity(), ignored -> new LinkedHashMap<>())
                        .put(target.property(), argument.getValue());
            }
        }
        return new InstanceOverrides(components, placements);
    }

    /** Returns component property overrides for one local target. */
    Map<PropertyId, ProjectValue> component(EntityId entity, ComponentId component) {
        return components.getOrDefault(new ComponentTarget(entity, component), Map.of());
    }

    /** Returns public arguments supplied through the containing definition to one nested placement. */
    Map<PropertyId, ProjectValue> placement(EntityId placement) {
        return placements.getOrDefault(placement, Map.of());
    }

    /** Finds the unique property target already validated by the asset catalog. */
    private static PropertyTarget target(EntityContract contract, PropertyId argument) {
        EntityContract.Parameter parameter = contract.parameters().get(argument);
        if (parameter != null) {
            return parameter.target();
        }
        EntityContract.ResourceBinding binding = contract.resourceBindings().get(argument);
        if (binding == null) {
            throw new IllegalStateException("validated contract argument has no target: " + argument);
        }
        return binding.target();
    }

    /** Deeply copies a two-level ordered map. */
    private static <K> Map<K, Map<PropertyId, ProjectValue>> immutableNestedMap(
            Map<K, Map<PropertyId, ProjectValue>> source) {
        Map<K, Map<PropertyId, ProjectValue>> result = new LinkedHashMap<>();
        source.forEach((key, values) -> result.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(values))));
        return Collections.unmodifiableMap(result);
    }

    /** Stable private component target within one definition instance. */
    private record ComponentTarget(EntityId entity, ComponentId component) {
        /** Validates one target key. */
        private ComponentTarget {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(component, "component");
        }
    }
}
