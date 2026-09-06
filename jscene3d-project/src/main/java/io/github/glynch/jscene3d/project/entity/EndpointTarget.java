/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Stable target of a local component endpoint or a placed definition's exported endpoint. */
public final class EndpointTarget {
    private final EntityId entity;
    private final @Nullable ComponentId component;
    private final EndpointId endpoint;

    /** Stores one target selected through a named public factory. */
    private EndpointTarget(EntityId entity, @Nullable ComponentId component, EndpointId endpoint) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.component = component;
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    }

    /**
     * Targets an endpoint on a locally authored component.
     *
     * @param entity local entity identity
     * @param component component identity
     * @param endpoint endpoint identity
     * @return component endpoint target
     */
    public static EndpointTarget component(EntityId entity, ComponentId component, EndpointId endpoint) {
        return new EndpointTarget(entity, Objects.requireNonNull(component, "component"), endpoint);
    }

    /**
     * Targets an exported endpoint on a reusable-definition placement.
     *
     * @param placement placement identity
     * @param endpoint exported endpoint identity
     * @return placement-contract endpoint target
     */
    public static EndpointTarget placement(EntityId placement, EndpointId endpoint) {
        return new EndpointTarget(placement, null, endpoint);
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
     * Returns the signal or action identity.
     *
     * @return endpoint identity
     */
    public EndpointId endpoint() {
        return endpoint;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof EndpointTarget target
                && entity.equals(target.entity)
                && Objects.equals(component, target.component)
                && endpoint.equals(target.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, component, endpoint);
    }

    @Override
    public String toString() {
        return "EndpointTarget[entity=" + entity + ", component=" + component + ", endpoint=" + endpoint + ']';
    }
}
