/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.world;

import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyRoots;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import java.util.List;
import java.util.Objects;

/** Immutable authored definition of one world and its root entity entries. */
public final class WorldDefinition {
    private final AssetId id;
    private final String name;
    private final List<EntityEntry> roots;

    /**
     * Creates a world definition.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param roots local entities and reusable-definition placements
     */
    public WorldDefinition(AssetId id, String name, List<? extends EntityEntry> roots) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.roots = copyRoots(roots, "roots");
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
        return other instanceof WorldDefinition definition
                && id.equals(definition.id)
                && name.equals(definition.name)
                && roots.equals(definition.roots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, roots);
    }

    @Override
    public String toString() {
        return "WorldDefinition[id=" + id + ", name=" + name + ", roots=" + roots + ']';
    }
}
