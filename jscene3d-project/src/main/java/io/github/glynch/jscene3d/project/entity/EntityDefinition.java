/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static io.github.glynch.jscene3d.project.entity.internal.EntityTreeChecks.copyRoots;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.asset.AssetId;
import java.util.List;
import java.util.Objects;

/** Immutable reusable single-root entity hierarchy. */
public final class EntityDefinition {
    private final AssetId id;
    private final String name;
    private final LocalEntity root;

    /**
     * Creates a reusable entity definition.
     *
     * @param id stable asset identity
     * @param name editor display name
     * @param root locally authored definition root
     */
    public EntityDefinition(AssetId id, String name, LocalEntity root) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.root = Objects.requireNonNull(root, "root");
        copyRoots(List.of(root), "root");
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
                && root.equals(definition.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, root);
    }

    @Override
    public String toString() {
        return "EntityDefinition[id=" + id + ", name=" + name + ", root=" + root + ']';
    }
}
