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
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Composition-time world implementation which owns all constructed component values. */
final class InternalWorld implements World {
    private final WorldDefinition definition;
    private final RuntimeResourceLookup resources;
    private final List<Entity> roots = new ArrayList<>();
    private final Map<RuntimeEntityId, Entity> entities = new LinkedHashMap<>();
    private final WorldLifecycle lifecycle = new WorldLifecycle();
    private final EndpointRouter endpointRouter = new EndpointRouter();
    private WorldSchedule schedule = WorldSchedule.empty();
    private boolean complete;

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
    public void activate() {
        lifecycle.activate();
        endpointRouter.activate();
    }

    @Override
    public boolean isActive() {
        return lifecycle.isActive();
    }

    @Override
    public boolean isClosed() {
        return lifecycle.isClosed();
    }

    @Override
    public void advanceFixed(Duration step) {
        requireActive();
        schedule.advanceFixed(step);
    }

    @Override
    public void advanceFrame(Duration elapsed, float interpolation) {
        requireActive();
        schedule.advanceFrame(elapsed, interpolation);
    }

    @Override
    public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("world is closed");
        }
        return resources.resolveResource(reference, valueType);
    }

    @Override
    public void close() {
        if (schedule.isExecuting()) {
            throw new IllegalStateException("world update is in progress");
        }
        endpointRouter.deactivate();
        lifecycle.close();
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
    void complete(List<WorldComponentEntry> values) {
        requireBuilding();
        entities.values().stream().map(InternalEntity.class::cast).forEach(InternalEntity::complete);
        lifecycle.complete(values);
        schedule = new WorldSchedule(values);
        complete = true;
    }

    /** Returns the world-owned endpoint router during transactional composition. */
    EndpointRouter endpointRouter() {
        requireBuilding();
        return endpointRouter;
    }

    /** Marks an unsuccessfully composed world unusable without closing values owned by the caller's rollback. */
    void fail() {
        endpointRouter.deactivate();
        lifecycle.failComposition();
    }

    /** Requires that structural composition has not completed or failed. */
    private void requireBuilding() {
        if (complete || lifecycle.isClosed()) {
            throw new IllegalStateException("world composition is not open");
        }
    }

    /** Requires successful activation before component updates can execute. */
    private void requireActive() {
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("world is closed");
        }
        if (!lifecycle.isActive()) {
            throw new IllegalStateException("world is not active");
        }
    }
}
