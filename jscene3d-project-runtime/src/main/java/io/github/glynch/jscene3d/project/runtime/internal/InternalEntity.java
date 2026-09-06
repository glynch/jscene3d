/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.World;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Mutable composition-time entity exposed publicly through a read-only interface. */
final class InternalEntity implements Entity {
    private final InternalWorld world;
    private final RuntimeEntityId id;
    private final AssetId authoredAsset;
    private final EntityId authoredId;
    private final Optional<String> name;
    private final boolean locallyEnabled;
    private final boolean enabled;
    private final @Nullable InternalEntity parent;
    private final List<Entity> children = new ArrayList<>();
    private final Map<ComponentId, Object> components = new LinkedHashMap<>();
    private boolean complete;

    /** Stores one allocated entity before its children and component values are attached. */
    InternalEntity(
            InternalWorld world,
            RuntimeEntityId id,
            AssetId authoredAsset,
            EntityId authoredId,
            Optional<String> name,
            boolean locallyEnabled,
            @Nullable InternalEntity parent) {
        this.world = Objects.requireNonNull(world, "world");
        this.id = Objects.requireNonNull(id, "id");
        this.authoredAsset = Objects.requireNonNull(authoredAsset, "authoredAsset");
        this.authoredId = Objects.requireNonNull(authoredId, "authoredId");
        this.name = Objects.requireNonNull(name, "name");
        this.locallyEnabled = locallyEnabled;
        this.enabled = locallyEnabled && (parent == null || parent.isEnabled());
        this.parent = parent;
    }

    @Override
    public RuntimeEntityId id() {
        return id;
    }

    @Override
    public AssetId authoredAsset() {
        return authoredAsset;
    }

    @Override
    public EntityId authoredId() {
        return authoredId;
    }

    @Override
    public Optional<String> name() {
        return name;
    }

    @Override
    public boolean isLocallyEnabled() {
        return locallyEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Optional<Entity> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<Entity> children() {
        return List.copyOf(children);
    }

    @Override
    public List<ComponentId> componentIds() {
        return List.copyOf(components.keySet());
    }

    @Override
    public <T> Optional<T> component(ComponentId component, Class<T> valueType) {
        Object value = components.get(Objects.requireNonNull(component, "component"));
        return value == null
                ? Optional.empty()
                : Optional.of(Objects.requireNonNull(valueType, "valueType").cast(value));
    }

    @Override
    public World world() {
        return world;
    }

    /** Adds one allocated owned child before graph completion. */
    void addChild(InternalEntity child) {
        requireIncomplete();
        children.add(Objects.requireNonNull(child, "child"));
    }

    /** Adds one constructed component value before graph completion. */
    void addComponent(ComponentId component, Object value) {
        requireIncomplete();
        if (components.putIfAbsent(
                        Objects.requireNonNull(component, "component"), Objects.requireNonNull(value, "value"))
                != null) {
            throw new IllegalStateException("runtime component identity is duplicated: " + component);
        }
    }

    /** Freezes structural collections after successful component construction. */
    void complete() {
        requireIncomplete();
        complete = true;
    }

    /** Prevents mutation after graph completion. */
    private void requireIncomplete() {
        if (complete) {
            throw new IllegalStateException("entity composition has already completed");
        }
    }
}
