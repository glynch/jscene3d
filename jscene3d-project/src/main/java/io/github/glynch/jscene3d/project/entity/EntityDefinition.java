/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyConnections;
import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyRoots;
import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.requireContractTargets;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import java.util.List;
import java.util.Objects;

/** Immutable reusable single-root entity hierarchy. */
public final class EntityDefinition {
    private final AssetId id;
    private final String name;
    private final EntityContract contract;
    private final List<SignalConnection> connections;
    private final LocalEntity root;

    /**
     * Creates a reusable entity definition.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param root locally authored definition root
     */
    public EntityDefinition(AssetId id, String name, LocalEntity root) {
        this(id, name, EntityContract.empty(), List.of(), root);
    }

    /**
     * Creates a reusable entity definition with an exported contract and internal connections.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param contract deliberately exported definition interface
     * @param connections internal signal/action connections
     * @param root locally authored definition root
     */
    public EntityDefinition(
            AssetId id, String name, EntityContract contract, List<SignalConnection> connections, LocalEntity root) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.contract = Objects.requireNonNull(contract, "contract");
        this.root = Objects.requireNonNull(root, "root");
        List<EntityEntry> roots = copyRoots(List.of(root), "root");
        requireContractTargets(root, contract, "contract");
        this.connections = copyConnections(roots, connections, "connections");
    }

    /**
     * Returns the stable asset identity.
     *
     * @return asset identity
     */
    public AssetId id() {
        return id;
    }

    /**
     * Returns the editor display name.
     *
     * @return display name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the deliberately exported definition interface.
     *
     * @return immutable public contract
     */
    public EntityContract contract() {
        return contract;
    }

    /**
     * Returns internal signal/action connections in declaration order.
     *
     * @return immutable connections
     */
    public List<SignalConnection> connections() {
        return connections;
    }

    /**
     * Returns the single locally authored root.
     *
     * @return definition root
     */
    public LocalEntity root() {
        return root;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof EntityDefinition definition
                && id.equals(definition.id)
                && name.equals(definition.name)
                && contract.equals(definition.contract)
                && connections.equals(definition.connections)
                && root.equals(definition.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, contract, connections, root);
    }

    @Override
    public String toString() {
        return "EntityDefinition[id=" + id + ", name=" + name + ", contract=" + contract + ", connections="
                + connections + ", root=" + root + ']';
    }
}
