/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** Short-lived resolver for one component's effective target-valued properties. */
final class ComponentReferenceBindingContext implements ComponentReferenceResolver {
    private final EffectiveComponentProperties properties;
    private final String location;
    private boolean active = true;

    /** Stores one component-local effective property view and diagnostic location. */
    ComponentReferenceBindingContext(EffectiveComponentProperties properties, String location) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.location = Objects.requireNonNull(location, "location");
    }

    @Override
    public Entity entity(PropertyId property) {
        ScopedProjectValue scoped = required(property);
        if (!(scoped.value() instanceof ProjectValue.EntityTargetValue target)) {
            throw invalidKind(property, "entity_target");
        }
        return scoped.targetScope().findEntity(target.entity()).orElseThrow(() -> missing(property, target.entity()));
    }

    @Override
    public <T> T component(PropertyId property, Class<T> valueType) {
        Class<T> expectedType = Objects.requireNonNull(valueType, "valueType");
        ScopedProjectValue scoped = required(property);
        if (!(scoped.value() instanceof ProjectValue.ComponentTargetValue targetValue)) {
            throw invalidKind(property, "component_target");
        }
        ComponentTarget target = targetValue.target();
        Object value = scoped.targetScope().findComponent(target).orElseThrow(() -> missing(property, target));
        if (!expectedType.isInstance(value)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_REFERENCE_TYPE_INVALID,
                    "target " + target + " has runtime type " + value.getClass().getName() + " instead of "
                            + expectedType.getName(),
                    propertyLocation(property));
        }
        return expectedType.cast(value);
    }

    /** Prevents a component from retaining and later reusing this composition-only resolver. */
    void expire() {
        active = false;
    }

    /** Returns one required property after enforcing the resolver lifetime. */
    private ScopedProjectValue required(PropertyId property) {
        if (!active) {
            throw new IllegalStateException("component reference resolver has expired");
        }
        return properties.required(Objects.requireNonNull(property, "property"));
    }

    /** Creates a structured missing-target failure at one property. */
    private RuntimeCompositionException missing(PropertyId property, Object target) {
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_REFERENCE_MISSING,
                "target does not exist in its definition instance: " + target,
                propertyLocation(property));
    }

    /** Creates a structured failure when an implementation requests the wrong authored value kind. */
    private RuntimeCompositionException invalidKind(PropertyId property, String expectedKind) {
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_REFERENCE_BINDING_FAILED,
                "property " + property + " must contain " + expectedKind,
                propertyLocation(property));
    }

    /** Returns the JSON Pointer location of one component property. */
    private String propertyLocation(PropertyId property) {
        return location + "/properties/" + property;
    }
}
