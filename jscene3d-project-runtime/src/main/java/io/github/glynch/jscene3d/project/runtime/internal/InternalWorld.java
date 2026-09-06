/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Composition-time world implementation which owns all constructed component values. */
final class InternalWorld implements World {
    private final WorldDefinition definition;
    private final RuntimeResourceLookup resources;
    private final List<Entity> roots = new ArrayList<>();
    private final Map<RuntimeEntityId, Entity> entities = new LinkedHashMap<>();
    private List<Object> components = List.of();
    private boolean complete;
    private boolean closed;

    /** Creates an empty world shell visible to factories while its complete graph is constructed. */
    InternalWorld(WorldDefinition definition, RuntimeResourceLookup resources) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    @Override
    public WorldDefinition definition() {
        return definition;
    }

    @Override
    public List<Entity> roots() {
        return List.copyOf(roots);
    }

    @Override
    public Optional<Entity> find(RuntimeEntityId id) {
        return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
        if (closed) {
            throw new IllegalStateException("world is closed");
        }
        return resources.resolveResource(reference, valueType);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException failure = null;
        for (int index = components.size() - 1; index >= 0; index--) {
            failure = closeComponent(components.get(index), failure);
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Adds one allocated entity to the world lookup. */
    void addEntity(InternalEntity entity) {
        requireBuilding();
        if (entities.putIfAbsent(entity.id(), entity) != null) {
            throw new IllegalStateException("runtime entity identity is duplicated: " + entity.id());
        }
    }

    /** Adds one allocated world root in authored order. */
    void addRoot(InternalEntity entity) {
        requireBuilding();
        roots.add(Objects.requireNonNull(entity, "entity"));
    }

    /** Freezes the successfully built graph and takes ownership of component values. */
    void complete(List<Object> values) {
        requireBuilding();
        entities.values().stream().map(InternalEntity.class::cast).forEach(InternalEntity::complete);
        components = List.copyOf(values);
        complete = true;
    }

    /** Marks an unsuccessfully composed world unusable without closing values owned by the caller's rollback. */
    void fail() {
        closed = true;
    }

    /** Requires that structural composition has not completed or failed. */
    private void requireBuilding() {
        if (complete || closed) {
            throw new IllegalStateException("world composition is not open");
        }
    }

    /** Closes one component and accumulates failures without skipping later cleanup. */
    private static @Nullable RuntimeException closeComponent(Object component, @Nullable RuntimeException existing) {
        if (!(component instanceof AutoCloseable closeable)) {
            return existing;
        }
        try {
            closeable.close();
            return existing;
        } catch (Exception exception) {
            @Nullable RuntimeException failure = existing;
            if (failure == null) {
                failure = new IllegalStateException("component cleanup failed", exception);
            } else {
                failure.addSuppressed(exception);
            }
            return failure;
        }
    }
}
