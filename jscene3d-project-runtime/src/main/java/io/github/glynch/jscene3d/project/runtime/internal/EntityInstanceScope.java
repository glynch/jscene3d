/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Distinct live expansion of one world or reusable entity-definition asset. */
final class EntityInstanceScope {
    private final AssetId asset;
    private final InstanceOverrides overrides;
    private final Map<EntityId, InternalEntity> entities = new LinkedHashMap<>();

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
}
