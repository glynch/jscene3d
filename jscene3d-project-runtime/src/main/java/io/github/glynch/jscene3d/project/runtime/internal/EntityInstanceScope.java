/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Distinct live expansion of one world or reusable entity-definition asset. */
final class EntityInstanceScope {
    private final AssetId asset;
    private final InstanceOverrides overrides;
    private final Map<EntityId, InternalEntity> entities = new LinkedHashMap<>();
    private final Map<ComponentTarget, Object> components = new LinkedHashMap<>();
    private final Map<EndpointTarget, RuntimeEndpointAddress> exportedSignals = new LinkedHashMap<>();
    private final Map<EndpointTarget, RuntimeEndpointAddress> exportedActions = new LinkedHashMap<>();

    /** Creates one empty instance-local identity table. */
    EntityInstanceScope(AssetId asset, InstanceOverrides overrides) {
        this.asset = Objects.requireNonNull(asset, "asset");
        this.overrides = Objects.requireNonNull(overrides, "overrides");
    }

    /** Returns the asset expanded by this scope. */
    AssetId asset() {
        return asset;
    }

    /** Returns contract overrides resolved specifically for this scope. */
    InstanceOverrides overrides() {
        return overrides;
    }

    /** Binds one stable local identity to its live entity within only this scope. */
    void bind(EntityId id, InternalEntity entity) {
        if (entities.putIfAbsent(Objects.requireNonNull(id, "id"), Objects.requireNonNull(entity, "entity")) != null) {
            throw new IllegalStateException("entity identity is duplicated within one instance scope: " + id);
        }
    }

    /** Binds one locally authored component identity without exposing nested placement internals. */
    void bindComponent(EntityId entity, ComponentId component, Object value) {
        ComponentTarget target = new ComponentTarget(entity, component);
        if (components.putIfAbsent(target, Objects.requireNonNull(value, "value")) != null) {
            throw new IllegalStateException("component identity is duplicated within one instance scope: " + target);
        }
    }

    /** Finds one entity or placement root in this exact live definition instance. */
    Optional<InternalEntity> findEntity(EntityId id) {
        return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
    }

    /** Finds one locally authored component in this exact live definition instance. */
    Optional<Object> findComponent(ComponentTarget target) {
        return Optional.ofNullable(components.get(Objects.requireNonNull(target, "target")));
    }

    /** Binds one placed definition's public signal to the private live endpoint it exports. */
    void bindExportedSignal(EndpointTarget target, RuntimeEndpointAddress address) {
        bindExportedEndpoint(exportedSignals, target, address, "signal");
    }

    /** Binds one placed definition's public action to the private live endpoint it exports. */
    void bindExportedAction(EndpointTarget target, RuntimeEndpointAddress address) {
        bindExportedEndpoint(exportedActions, target, address, "action");
    }

    /** Resolves one local or placed-definition signal within exactly this definition instance. */
    Optional<RuntimeEndpointAddress> findSignal(EndpointTarget target) {
        return findEndpoint(exportedSignals, target);
    }

    /** Resolves one local or placed-definition action within exactly this definition instance. */
    Optional<RuntimeEndpointAddress> findAction(EndpointTarget target) {
        return findEndpoint(exportedActions, target);
    }

    /** Adds one unique placement-contract endpoint mapping. */
    private static void bindExportedEndpoint(
            Map<EndpointTarget, RuntimeEndpointAddress> endpoints,
            EndpointTarget target,
            RuntimeEndpointAddress address,
            String kind) {
        EndpointTarget validTarget = Objects.requireNonNull(target, "target");
        if (validTarget.component().isPresent()) {
            throw new IllegalArgumentException("exported " + kind + " target must identify a placement contract");
        }
        if (endpoints.putIfAbsent(validTarget, Objects.requireNonNull(address, "address")) != null) {
            throw new IllegalStateException("exported " + kind + " identity is duplicated: " + validTarget);
        }
    }

    /** Resolves a direct component endpoint or a previously bound placement-contract endpoint. */
    private Optional<RuntimeEndpointAddress> findEndpoint(
            Map<EndpointTarget, RuntimeEndpointAddress> exports, EndpointTarget target) {
        EndpointTarget validTarget = Objects.requireNonNull(target, "target");
        if (validTarget.component().isEmpty()) {
            return Optional.ofNullable(exports.get(validTarget));
        }
        return findEntity(validTarget.entity())
                .map(entity -> new RuntimeEndpointAddress(
                        entity.id(), validTarget.component().orElseThrow(), validTarget.endpoint()));
    }
}
