/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.EntityPreparationException;
import io.github.glynch.jscene3d.project.runtime.PreparedEntityDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.SpawnOperation;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
    private final WorldCompositionServices services;
    private final WorldModules modules;
    private final WorldResources resources;
    private final List<Entity> roots = new ArrayList<>();
    private final Map<RuntimeEntityId, Entity> entities = new LinkedHashMap<>();
    private final WorldLifecycle lifecycle = new WorldLifecycle();
    private final EndpointRouter endpointRouter;
    private final Map<InternalEntity, Enablement> requestedEnablement = new LinkedHashMap<>();
    private final Set<InternalEntity> requestedDestruction = new LinkedHashSet<>();
    private final List<InternalSpawnOperation> requestedSpawns = new ArrayList<>();
    private final Set<Object> componentIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
    private WorldSchedule schedule = WorldSchedule.empty();
    private long nextEntityId = 1L;
    private boolean complete;
    private boolean committingMutation;

    /** Creates an empty world shell visible to factories while its complete graph is constructed. */
    InternalWorld(WorldDefinition definition, WorldCompositionServices services) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.services = Objects.requireNonNull(services, "services");
        modules = services.modules();
        resources = new WorldResources(services.resources());
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
    public PreparedEntityDefinition prepare(
            AssetRef<EntityDefinition> reference, Map<PropertyId, ProjectValue> resourceBindings) {
        requirePreparationOpen();
        AssetRef<EntityDefinition> validReference = Objects.requireNonNull(reference, "reference");
        Map<PropertyId, ProjectValue> validBindings = immutableValues(resourceBindings, "resourceBindings");
        try {
            DefinitionPreparationPlanner.PreparedGraph graph = new DefinitionPreparationPlanner(
                            services.definitions(), services.types())
                    .plan(validReference, validBindings);
            RuntimeComponentPreparer.prepare(graph.components(), services.types(), services.factories(), resources);
            return new InternalPreparedEntityDefinition(
                    this, validReference, graph.definition(), graph.definitions(), validBindings, graph.source());
        } catch (RuntimeDiagnosticsException failure) {
            throw new EntityPreparationException(failure.diagnostics());
        } catch (RuntimeCompositionException failure) {
            String detail =
                    Objects.toString(failure.getMessage(), failure.code().defaultMessage());
            throw new EntityPreparationException(
                    List.of(diagnostic(services.source(), failure.code(), detail, failure.location())));
        } catch (RuntimeException failure) {
            throw new EntityPreparationException(List.of(diagnostic(
                    services.source(),
                    RuntimeDiagnosticCode.ENTITY_PREPARATION_FAILED,
                    failureDetail("entity-definition preparation failed", failure),
                    "")));
        }
    }

    @Override
    public SpawnTarget spawnTarget(Entity owner) {
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("world is closed");
        }
        InternalEntity target = requireOwned(owner);
        if (target.isDestroyed() || target.isPendingDestruction()) {
            throw new IllegalStateException("entity is pending or completely destroyed: " + target.id());
        }
        return new InternalSpawnTarget(this, target);
    }

    @Override
    public void activate() {
        try {
            lifecycle.activate();
            endpointRouter.activate();
        } catch (RuntimeException failure) {
            closeResources(failure);
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

    /** Resolves a construction-time resource and attributes its lease to one entity. */
    <T> T resolveResource(
            InternalEntity owner, ResourceReference reference, Class<T> valueType, String componentLocation) {
        requireBuilding();
        return resources.resolve(owner, reference, valueType, componentLocation);
    }

    /** Resolves one resource retained during preparation for a spawned component. */
    <T> T resolvePreparedResource(
            InternalEntity owner, ResourceReference reference, Class<T> valueType, String componentLocation) {
        return resources.resolvePrepared(owner, reference, valueType, componentLocation);
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
        failure = closeResources(failure);
        failure = closeModules(failure);
        componentIdentities.clear();
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

    /** Allocates one fresh monotonically increasing world-local entity identity. */
    RuntimeEntityId allocateEntityId() {
        return new RuntimeEntityId(nextEntityId++);
    }

    /** Returns the stable definition resolver retained for runtime preparation. */
    DefinitionResolver definitionResolver() {
        return services.definitions();
    }

    /** Returns the safe registered-type catalog retained for runtime composition. */
    RegisteredTypeCatalog types() {
        return services.types();
    }

    /** Claims one factory-created component identity across the complete world. */
    void claimComponent(Object value, String location) {
        if (!componentIdentities.add(Objects.requireNonNull(value, "value"))) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.FACTORY_CREATE_FAILED,
                    "a component factory returned the same object for more than one component",
                    location);
        }
    }

    /** Releases component identities which never became or are no longer live. */
    void releaseComponentClaims(List<Object> values) {
        componentIdentities.removeAll(Objects.requireNonNull(values, "values"));
    }

    /** Freezes the successfully built graph and takes ownership of component values. */
    void complete(List<WorldComponentEntry> values) {
        requireBuilding();
        entities.values().stream().map(InternalEntity.class::cast).forEach(InternalEntity::complete);
        lifecycle.complete(values);
        schedule = new WorldSchedule(values, modules::advancePhysics, this::commitMutations);
        complete = true;
    }

    /** Returns the world-owned endpoint router during transactional composition. */
    EndpointRouter endpointRouter() {
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("world is closed");
        }
        return endpointRouter;
    }

    /** Marks failed composition terminal and releases leases after the caller closes constructed values. */
    void fail(RuntimeException failure) {
        endpointRouter.deactivate();
        lifecycle.failComposition();
        componentIdentities.clear();
        closeResources(Objects.requireNonNull(failure, "failure"));
    }

    /** Queues one validated owner-scoped spawn request and commits it immediately when idle. */
    SpawnOperation spawn(
            InternalEntity owner, PreparedEntityDefinition definition, Map<PropertyId, ProjectValue> parameters) {
        requireActive();
        requireMutationRequestOpen();
        InternalEntity validOwner = requireOwned(owner);
        if (validOwner.isDestroyed() || validOwner.isPendingDestruction()) {
            throw new IllegalStateException("entity is pending or completely destroyed: " + validOwner.id());
        }
        if (!(definition instanceof InternalPreparedEntityDefinition prepared) || prepared.world() != this) {
            throw new IllegalArgumentException("prepared definition belongs to another world");
        }
        InternalSpawnOperation operation =
                new InternalSpawnOperation(validOwner, prepared, immutableValues(parameters, "parameters"));
        requestedSpawns.add(operation);
        commitWhenIdle();
        return operation;
    }

    /** Requires that structural composition has not completed or failed. */
    private void requireBuilding() {
        if (complete || lifecycle.isClosed()) {
            throw new IllegalStateException("world composition is not open");
        }
    }

    /** Requires preparation after composition and before initial activation. */
    private void requirePreparationOpen() {
        if (!complete) {
            throw new IllegalStateException("world composition is incomplete");
        }
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("world is closed");
        }
        if (lifecycle.isActive()) {
            throw new IllegalStateException("entity preparation is permitted only before world activation");
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
        if (requestedEnablement.isEmpty() && requestedDestruction.isEmpty() && requestedSpawns.isEmpty()) {
            return;
        }
        committingMutation = true;
        try {
            applyRequestedEnablement();
            refreshEnablement();
            lifecycle.synchronizeActivation();
            commitRequestedSpawns();
            commitRequestedDestruction();
        } catch (RuntimeException failure) {
            terminateAfterMutationFailure(failure);
            throw failure;
        } finally {
            requestedEnablement.clear();
            requestedDestruction.clear();
            requestedSpawns.clear();
            committingMutation = false;
        }
    }

    /** Commits each pending spawn independently without making one rejected instance world-terminal. */
    private void commitRequestedSpawns() {
        for (InternalSpawnOperation operation : List.copyOf(requestedSpawns)) {
            if (!operation.isPending()) {
                continue;
            }
            if (operation.owner().isDestroyed() || operation.owner().isPendingDestruction()) {
                operation.fail(List.of(diagnostic(
                        operation.prepared().source(),
                        RuntimeDiagnosticCode.SPAWN_FAILED,
                        "spawn owner is pending or completely destroyed",
                        "")));
                continue;
            }
            commitSpawn(operation);
        }
    }

    /** Constructs, activates, and atomically publishes one prepared definition instance. */
    private void commitSpawn(InternalSpawnOperation operation) {
        @Nullable AllocatedInstance allocation = null;
        @Nullable List<WorldComponentEntry> entries = null;
        try {
            allocation = new EntityGraphAllocator(this)
                    .allocateSpawn(operation.prepared(), operation.owner(), operation.parameters());
            entries = RuntimeComponentConstructor.construct(allocation, services.types(), services.factories());
            publishSpawn(allocation);
            schedule.addComponents(entries);
            lifecycle.add(entries);
            operation.complete(allocation.root());
        } catch (RuntimeDiagnosticsException failure) {
            rollbackCommittedSpawn(allocation, entries, failure);
            operation.fail(failure.diagnostics());
        } catch (RuntimeCompositionException failure) {
            rollbackCommittedSpawn(allocation, entries, failure);
            String detail =
                    Objects.toString(failure.getMessage(), failure.code().defaultMessage());
            operation.fail(
                    List.of(diagnostic(operation.prepared().source(), failure.code(), detail, failure.location())));
        } catch (RuntimeException failure) {
            rollbackCommittedSpawn(allocation, entries, failure);
            operation.fail(List.of(diagnostic(
                    operation.prepared().source(),
                    RuntimeDiagnosticCode.SPAWN_FAILED,
                    failureDetail("spawn transaction failed", failure),
                    "")));
        }
    }

    /** Publishes one completely constructed hierarchy into world lookup and ownership traversal. */
    private void publishSpawn(AllocatedInstance allocation) {
        allocation.entities().forEach(entity -> {
            if (entities.putIfAbsent(entity.id(), entity) != null) {
                throw new IllegalStateException("runtime entity identity is duplicated: " + entity.id());
            }
        });
        InternalEntity parent =
                allocation.root().parent().map(InternalEntity.class::cast).orElseThrow();
        parent.attachChild(allocation.root());
    }

    /** Removes and releases a spawn which failed after component construction returned. */
    private void rollbackCommittedSpawn(
            @Nullable AllocatedInstance allocation,
            @Nullable List<WorldComponentEntry> entries,
            RuntimeException failure) {
        if (allocation == null || entries == null) {
            return;
        }
        Set<InternalEntity> targets = new LinkedHashSet<>(allocation.entities());
        if (entities.containsKey(allocation.root().id())) {
            schedule.removeEntities(targets);
            allocation
                    .root()
                    .parent()
                    .map(InternalEntity.class::cast)
                    .ifPresent(parent -> parent.removeChild(allocation.root()));
            targets.forEach(entity -> entities.remove(entity.id()));
        }
        rollbackSpawn(
                allocation.entities(),
                entries.stream().map(WorldComponentEntry::value).toList(),
                failure);
    }

    /** Releases every world integration owned by one detached or unpublished failed spawn. */
    void rollbackSpawn(List<InternalEntity> spawned, List<Object> values, RuntimeException failure) {
        Set<InternalEntity> targets = new LinkedHashSet<>(spawned);
        Set<RuntimeEntityId> identities = new LinkedHashSet<>();
        targets.forEach(entity -> identities.add(entity.id()));
        endpointRouter.removeEntities(identities);
        try {
            resources.release(targets);
        } catch (RuntimeException resourceFailure) {
            failure.addSuppressed(resourceFailure);
        }
        releaseComponentClaims(values);
        closeValues(values, failure);
        for (int index = spawned.size() - 1; index >= 0; index--) {
            spawned.get(index).completeDestruction();
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
        List<Object> values = destroyed.stream()
                .flatMap(entity -> entity.componentValues().stream())
                .toList();
        @Nullable RuntimeException failure = destroyComponents(destroyed);
        failure = releaseResources(destroyed, failure);
        releaseComponentClaims(values);
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
        closeResources(failure);
        closeModules(failure);
    }

    /** Releases leases owned only by one destroyed subtree while retaining an earlier cleanup failure. */
    private @Nullable RuntimeException releaseResources(
            Set<InternalEntity> destroyed, @Nullable RuntimeException existing) {
        try {
            resources.release(destroyed);
            return existing;
        } catch (RuntimeException resourceFailure) {
            if (existing == null) {
                return resourceFailure;
            }
            existing.addSuppressed(resourceFailure);
            return existing;
        }
    }

    /** Closes all world-owned resource leases while retaining an earlier cleanup failure. */
    private @Nullable RuntimeException closeResources(@Nullable RuntimeException existing) {
        try {
            resources.close();
            return existing;
        } catch (RuntimeException resourceFailure) {
            if (existing == null) {
                return resourceFailure;
            }
            existing.addSuppressed(resourceFailure);
            return existing;
        }
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

    /** Copies a public project-value map while rejecting null keys and values. */
    private static Map<PropertyId, ProjectValue> immutableValues(Map<PropertyId, ProjectValue> values, String name) {
        Map<PropertyId, ProjectValue> result = new LinkedHashMap<>();
        Objects.requireNonNull(values, name)
                .forEach((property, value) -> result.put(
                        Objects.requireNonNull(property, name + " key"),
                        Objects.requireNonNull(value, name + " value")));
        return Map.copyOf(result);
    }

    /** Creates one runtime diagnostic with stable language-neutral technical detail. */
    private static ProjectDiagnostic diagnostic(URI source, DiagnosticCode code, String detail, String location) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                source,
                location,
                Map.of("technicalDetail", Objects.toString(detail, code.defaultMessage())));
    }

    /** Appends an available implementation detail to a stable failure prefix. */
    private static String failureDetail(String prefix, RuntimeException failure) {
        return failure.getMessage() == null
                ? prefix + ": " + failure.getClass().getSimpleName()
                : prefix + ": " + failure.getMessage();
    }

    /** Closes newly created values in reverse order while retaining failure precedence. */
    private static void closeValues(List<Object> values, RuntimeException failure) {
        for (int index = values.size() - 1; index >= 0; index--) {
            Object value = values.get(index);
            if (value instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
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
