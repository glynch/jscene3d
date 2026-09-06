/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Composition-time world implementation which owns all constructed component values. */
final class InternalWorld implements World {
    private final WorldDefinition definition;
    private final WorldModules modules;
    private final RuntimeResourceLookup resources;
    private final List<Entity> roots = new ArrayList<>();
    private final Map<RuntimeEntityId, Entity> entities = new LinkedHashMap<>();
    private final WorldLifecycle lifecycle = new WorldLifecycle();
    private final EndpointRouter endpointRouter;
    private final Map<InternalEntity, Enablement> requestedEnablement = new LinkedHashMap<>();
    private final Set<InternalEntity> requestedDestruction = new LinkedHashSet<>();
    private WorldSchedule schedule = WorldSchedule.empty();
    private boolean complete;
    private boolean committingMutation;

    /** Creates an empty world shell visible to factories while its complete graph is constructed. */
    InternalWorld(WorldDefinition definition, WorldModules modules, RuntimeResourceLookup resources) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.resources = Objects.requireNonNull(resources, "resources");
        endpointRouter = new EndpointRouter(this::commitWhenIdle);
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
    public <T extends WorldModule> Optional<T> findModule(Class<T> type) {
        return modules.find(type);
    }

    @Override
    public <T extends WorldModule> T requireModule(Class<T> type) {
        return modules.require(type);
    }

    @Override
    public void activate() {
        try {
            lifecycle.activate();
            endpointRouter.activate();
        } catch (RuntimeException failure) {
            closeModules(failure);
            throw failure;
        }
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
        requireAdvancementOpen();
        schedule.advanceFixed(step);
    }

    @Override
    public void advanceFrame(Duration elapsed, float interpolation) {
        requireActive();
        requireAdvancementOpen();
        schedule.advanceFrame(elapsed, interpolation);
    }

    @Override
    public void enable(Entity entity) {
        requestEnablement(entity, Enablement.ENABLED);
    }

    @Override
    public void disable(Entity entity) {
        requestEnablement(entity, Enablement.DISABLED);
    }

    @Override
    public void destroy(Entity entity) {
        requireActive();
        requireMutationRequestOpen();
        InternalEntity target = requireOwned(entity);
        if (target.isDestroyed() || target.isPendingDestruction()) {
            return;
        }
        List<InternalEntity> subtree = target.subtree();
        target.markPendingDestruction();
        requestedEnablement.keySet().removeIf(subtree::contains);
        requestedDestruction.removeIf(subtree::contains);
        requestedDestruction.add(target);
        commitWhenIdle();
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
        if (endpointRouter.isDispatching()) {
            throw new IllegalStateException("world signal dispatch is in progress");
        }
        if (committingMutation) {
            throw new IllegalStateException("world structural mutation is in progress");
        }
        endpointRouter.deactivate();
        @Nullable RuntimeException failure = null;
        try {
            lifecycle.close();
        } catch (RuntimeException lifecycleFailure) {
            failure = lifecycleFailure;
        }
        failure = closeModules(failure);
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
    void complete(List<WorldComponentEntry> values) {
        requireBuilding();
        entities.values().stream().map(InternalEntity.class::cast).forEach(InternalEntity::complete);
        lifecycle.complete(values);
        schedule = new WorldSchedule(values, this::commitMutations);
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

    /** Records one final local enablement value and commits it immediately when no phase is executing. */
    private void requestEnablement(Entity entity, Enablement enablement) {
        requireActive();
        requireMutationRequestOpen();
        InternalEntity target = requireOwned(entity);
        if (target.isDestroyed() || target.isPendingDestruction()) {
            throw new IllegalStateException("entity is pending or completely destroyed: " + target.id());
        }
        if (target.isLocallyEnabled() == enablement.value()) {
            requestedEnablement.remove(target);
        } else {
            requestedEnablement.put(target, enablement);
        }
        commitWhenIdle();
    }

    /** Applies queued mutations immediately when the caller is already between phases. */
    private void commitWhenIdle() {
        if (!schedule.isExecuting() && !endpointRouter.isDispatching() && !committingMutation) {
            commitMutations();
        }
    }

    /** Applies final enablement values and committed destruction as one structural phase boundary. */
    private void commitMutations() {
        if (requestedEnablement.isEmpty() && requestedDestruction.isEmpty()) {
            return;
        }
        committingMutation = true;
        try {
            applyRequestedEnablement();
            refreshEnablement();
            lifecycle.synchronizeActivation();
            commitRequestedDestruction();
        } catch (RuntimeException failure) {
            terminateAfterMutationFailure(failure);
            throw failure;
        } finally {
            requestedEnablement.clear();
            requestedDestruction.clear();
            committingMutation = false;
        }
    }

    /** Applies coalesced local enablement changes without exposing intermediate transitions. */
    private void applyRequestedEnablement() {
        requestedEnablement.forEach((entity, enablement) -> entity.setLocallyEnabled(enablement.value()));
    }

    /** Recomputes effective enablement through every remaining ownership root. */
    private void refreshEnablement() {
        roots.forEach(root -> ((InternalEntity) root).refreshEnabled());
    }

    /** Releases and removes every requested destruction root after effective deactivation. */
    private void commitRequestedDestruction() {
        if (requestedDestruction.isEmpty()) {
            return;
        }
        List<InternalEntity> destructionRoots = List.copyOf(requestedDestruction);
        Set<InternalEntity> destroyed = new LinkedHashSet<>();
        destructionRoots.forEach(root -> destroyed.addAll(root.subtree()));
        @Nullable RuntimeException failure = destroyComponents(destroyed);
        removeDestroyedEntities(destructionRoots, destroyed);
        if (failure != null) {
            throw failure;
        }
    }

    /** Completes all targeted lifecycle cleanup and returns any accumulated implementation failure. */
    private @Nullable RuntimeException destroyComponents(Set<InternalEntity> destroyed) {
        try {
            lifecycle.destroy(destroyed);
            return null;
        } catch (RuntimeException failure) {
            return failure;
        }
    }

    /** Removes destroyed entities from every world-owned graph and runtime index. */
    private void removeDestroyedEntities(List<InternalEntity> destructionRoots, Set<InternalEntity> destroyed) {
        Set<RuntimeEntityId> identities = new LinkedHashSet<>();
        destroyed.forEach(entity -> identities.add(entity.id()));
        endpointRouter.removeEntities(identities);
        schedule.removeEntities(destroyed);
        for (InternalEntity root : destructionRoots) {
            root.parent()
                    .ifPresentOrElse(parent -> ((InternalEntity) parent).removeChild(root), () -> roots.remove(root));
        }
        destroyed.forEach(entity -> entities.remove(entity.id()));
        List<InternalEntity> reverse = new ArrayList<>(destroyed);
        for (int index = reverse.size() - 1; index >= 0; index--) {
            reverse.get(index).completeDestruction();
        }
    }

    /** Closes the complete world after a lifecycle mutation leaves component behavior unreliable. */
    private void terminateAfterMutationFailure(RuntimeException failure) {
        endpointRouter.deactivate();
        try {
            lifecycle.close();
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        closeModules(failure);
    }

    /** Closes owned world modules and retains any earlier component or lifecycle failure. */
    private @Nullable RuntimeException closeModules(@Nullable RuntimeException existing) {
        try {
            modules.close();
            return existing;
        } catch (RuntimeException moduleFailure) {
            if (existing == null) {
                return moduleFailure;
            }
            existing.addSuppressed(moduleFailure);
            return existing;
        }
    }

    /** Resolves one entity implementation and rejects cross-world mutation. */
    private InternalEntity requireOwned(Entity entity) {
        Entity target = Objects.requireNonNull(entity, "entity");
        if (!(target instanceof InternalEntity internal) || internal.world() != this) {
            throw new IllegalArgumentException("entity belongs to another world");
        }
        return internal;
    }

    /** Prevents lifecycle callbacks from recursively modifying a committing structure. */
    private void requireMutationRequestOpen() {
        if (committingMutation) {
            throw new IllegalStateException("world structural mutation is already in progress");
        }
    }

    /** Prevents scheduling from reentering signal or lifecycle behavior. */
    private void requireAdvancementOpen() {
        if (endpointRouter.isDispatching()) {
            throw new IllegalStateException("world signal dispatch is in progress");
        }
        if (committingMutation) {
            throw new IllegalStateException("world structural mutation is in progress");
        }
    }

    /** Closed local enablement commands retained until the next structural commit. */
    private enum Enablement {
        ENABLED(true),
        DISABLED(false);

        private final boolean value;

        /** Stores the resulting local enabled flag. */
        Enablement(boolean value) {
            this.value = value;
        }

        /** Returns the resulting local enabled flag. */
        private boolean value() {
            return value;
        }
    }
}
