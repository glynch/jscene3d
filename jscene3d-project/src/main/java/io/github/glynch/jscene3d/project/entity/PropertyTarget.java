/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Stable target of a local component property or a placed definition's exported argument. */
public final class PropertyTarget {
    private final EntityId entity;
    private final @Nullable ComponentId component;
    private final PropertyId property;

    /** Stores one target selected through a named public factory. */
    private PropertyTarget(EntityId entity, @Nullable ComponentId component, PropertyId property) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.component = component;
        this.property = Objects.requireNonNull(property, "property");
    }

    /**
     * Targets a property on a locally authored component.
     *
     * @param entity local entity identity
     * @param component component identity
     * @param property property identity
     * @return component-property target
     */
    public static PropertyTarget component(EntityId entity, ComponentId component, PropertyId property) {
        return new PropertyTarget(entity, Objects.requireNonNull(component, "component"), property);
    }

    /**
     * Targets an exported argument on a reusable-definition placement.
     *
     * @param placement placement identity
     * @param argument exported argument identity
     * @return placement-contract target
     */
    public static PropertyTarget placement(EntityId placement, PropertyId argument) {
        return new PropertyTarget(placement, null, argument);
    }

    /**
     * Returns the target entity or placement identity.
     *
     * @return target identity
     */
    public EntityId entity() {
        return entity;
    }

    /**
     * Returns the local component identity, absent for a placement contract.
     *
     * @return optional local component identity
     */
    public Optional<ComponentId> component() {
        return Optional.ofNullable(component);
    }

    /**
     * Returns the component property or exported argument identity.
     *
     * @return property or argument identity
     */
    public PropertyId property() {
        return property;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PropertyTarget target
                && entity.equals(target.entity)
                && Objects.equals(component, target.component)
                && property.equals(target.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, component, property);
    }

    @Override
    public String toString() {
        return "PropertyTarget[entity=" + entity + ", component=" + component + ", property=" + property + ']';
    }
}
