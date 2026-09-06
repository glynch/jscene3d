/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Structural invariants shared by world and reusable entity definitions. */
public final class EntityTreeChecks {
    /** Prevents construction of this invariant container. */
    private EntityTreeChecks() {
        throw new AssertionError("EntityTreeChecks cannot be instantiated");
    }

    /**
     * Copies root entries and rejects duplicate entity identities across their local hierarchy.
     *
     * @param roots root entries
     * @param name argument name used in failures
     * @return immutable roots
     */
    public static List<EntityEntry> copyRoots(List<? extends EntityEntry> roots, String name) {
        List<EntityEntry> copied = List.copyOf(Objects.requireNonNull(roots, name));
        requireUniqueIds(copied, name);
        return copied;
    }

    /**
     * Copies connections and requires every structural target to exist in the containing asset.
     *
     * @param roots containing asset roots
     * @param connections authored signal/action connections
     * @param name argument name used in failures
     * @return immutable connections
     */
    public static List<SignalConnection> copyConnections(
            List<? extends EntityEntry> roots, List<SignalConnection> connections, String name) {
        List<SignalConnection> copied = List.copyOf(Objects.requireNonNull(connections, name));
        EntryIndex index = new EntryIndex(roots);
        for (SignalConnection connection : copied) {
            index.requireEndpoint(connection.signal(), name + " signal");
            index.requireEndpoint(connection.action(), name + " action");
        }
        return copied;
    }

    /**
     * Requires every private target exported by a contract to exist in its definition hierarchy.
     *
     * @param root definition root
     * @param contract exported contract
     * @param name argument name used in failures
     */
    public static void requireContractTargets(LocalEntity root, EntityContract contract, String name) {
        EntryIndex index = new EntryIndex(List.of(root));
        contract.parameters().values().forEach(value -> index.requireProperty(value.target(), name + " parameter"));
        contract.signals().values().forEach(value -> index.requireEndpoint(value.target(), name + " signal"));
        contract.actions().values().forEach(value -> index.requireEndpoint(value.target(), name + " action"));
        contract.attachments().values().forEach(value -> index.requireSpatial(value.target(), name + " attachment"));
        contract.resourceBindings()
                .values()
                .forEach(value -> index.requireProperty(value.target(), name + " resource binding"));
    }

    /** Rejects duplicate identities while traversing only locally declared hierarchy entries. */
    private static void requireUniqueIds(List<EntityEntry> roots, String name) {
        Set<EntityId> ids = new HashSet<>();
        ArrayDeque<EntityEntry> remaining = new ArrayDeque<>(roots);
        while (!remaining.isEmpty()) {
            EntityEntry entry = remaining.removeFirst();
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException(name + " contains a duplicate entity id: " + entry.id());
            }
            if (entry instanceof LocalEntity local) {
                remaining.addAll(local.children());
            }
        }
    }

    /** Stable entry/component index used only while checking authored targets. */
    private static final class EntryIndex {
        private final Map<EntityId, EntityEntry> entries = new LinkedHashMap<>();

        /** Indexes a hierarchy already known to contain unique entity identities. */
        private EntryIndex(List<? extends EntityEntry> roots) {
            ArrayDeque<EntityEntry> remaining = new ArrayDeque<>(roots);
            while (!remaining.isEmpty()) {
                EntityEntry entry = remaining.removeFirst();
                entries.put(entry.id(), entry);
                if (entry instanceof LocalEntity local) {
                    remaining.addAll(local.children());
                }
            }
        }

        /** Requires a property target to match either a local component or a placement contract. */
        private void requireProperty(PropertyTarget target, String name) {
            EntityEntry entry = requireEntry(target.entity(), name);
            if (entry instanceof LocalEntity local) {
                ComponentId component = target.component()
                        .orElseThrow(() -> new IllegalArgumentException(name + " requires a local component id"));
                requireComponent(local, component, name);
            } else if (target.component().isPresent()) {
                throw new IllegalArgumentException(name + " cannot address a component inside a placement");
            }
        }

        /** Requires an endpoint target to match either a local component or a placement contract. */
        private void requireEndpoint(EndpointTarget target, String name) {
            EntityEntry entry = requireEntry(target.entity(), name);
            if (entry instanceof LocalEntity local) {
                ComponentId component = target.component()
                        .orElseThrow(() -> new IllegalArgumentException(name + " requires a local component id"));
                requireComponent(local, component, name);
            } else if (target.component().isPresent()) {
                throw new IllegalArgumentException(name + " cannot address a component inside a placement");
            }
        }

        /** Requires a spatial target to match a local entity/component or a placement seam. */
        private void requireSpatial(SpatialTarget target, String name) {
            EntityEntry entry = requireEntry(target.entity(), name);
            if (entry instanceof LocalEntity local) {
                target.component().ifPresent(component -> requireComponent(local, component, name));
                if (target.attachment().isPresent() && target.component().isEmpty()) {
                    throw new IllegalArgumentException(name + " local attachment requires a component id");
                }
            } else if (target.component().isPresent()) {
                throw new IllegalArgumentException(name + " cannot address a component inside a placement");
            }
        }

        /** Returns an addressed entry or rejects the dangling entity identity. */
        private EntityEntry requireEntry(EntityId id, String name) {
            EntityEntry entry = entries.get(id);
            if (entry == null) {
                throw new IllegalArgumentException(name + " references an unknown entity id: " + id);
            }
            return entry;
        }

        /** Requires one local entity to own the addressed component identity. */
        private static void requireComponent(LocalEntity entity, ComponentId id, String name) {
            for (ComponentDefinition component : entity.components()) {
                if (component.id().equals(id)) {
                    return;
                }
            }
            throw new IllegalArgumentException(name + " references an unknown component id: " + id);
        }
    }
}
