/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyConnections;
import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyRoots;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import java.util.List;
import java.util.Objects;

/** Immutable authored definition of one Scene and its root entity entries. */
public final class SceneDefinition {
    private final AssetId id;
    private final String name;
    private final List<SignalConnection> connections;
    private final List<EntityEntry> roots;

    /**
     * Creates a Scene definition.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param roots local entities and reusable-definition placements
     */
    public SceneDefinition(AssetId id, String name, List<? extends EntityEntry> roots) {
        this(id, name, List.of(), roots);
    }

    /**
     * Creates a Scene definition with internal signal/action connections.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param connections internal signal/action connections
     * @param roots local entities and reusable-definition placements
     */
    public SceneDefinition(
            AssetId id, String name, List<SignalConnection> connections, List<? extends EntityEntry> roots) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.roots = copyRoots(roots, "roots");
        this.connections = copyConnections(this.roots, connections, "connections");
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
     * Returns internal signal/action connections in declaration order.
     *
     * @return immutable connections
     */
    public List<SignalConnection> connections() {
        return connections;
    }

    /**
     * Returns root entity entries in declaration order.
     *
     * @return immutable root entries
     */
    public List<EntityEntry> roots() {
        return roots;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SceneDefinition definition
                && id.equals(definition.id)
                && name.equals(definition.name)
                && connections.equals(definition.connections)
                && roots.equals(definition.roots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, connections, roots);
    }

    @Override
    public String toString() {
        return "SceneDefinition[id=" + id + ", name=" + name + ", connections=" + connections + ", roots=" + roots
                + ']';
    }
}
