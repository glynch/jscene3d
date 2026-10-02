/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.composition.CompositionConnection;
import io.github.glynch.jscene3d.project.composition.CompositionEndpointExport;
import io.github.glynch.jscene3d.project.composition.CompositionEndpointKind;
import io.github.glynch.jscene3d.project.composition.CompositionEntity;
import io.github.glynch.jscene3d.project.composition.CompositionEntityBinding;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.composition.CompositionPlan;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Realizes one immutable safe composition plan as live runtime entity indexes and deferred components. */
final class EntityGraphAllocator {
    private final InternalWorld world;
    private final CompositionPlan plan;
    private final RuntimeCompositionScopes scopes = new RuntimeCompositionScopes();
    private final Map<CompositionOccurrenceId, InternalEntity> occurrences = new LinkedHashMap<>();
    private final List<InternalEntity> entities = new ArrayList<>();
    private final List<ComponentPlan> components = new ArrayList<>();
    private final List<RuntimeConnectionPlan> connections = new ArrayList<>();

    /** Stores the target world and already completed safe plan. */
    EntityGraphAllocator(InternalWorld world, CompositionPlan plan) {
        this.world = Objects.requireNonNull(world, "world");
        this.plan = Objects.requireNonNull(plan, "plan");
    }

    /** Allocates one complete initial world without repeating authored traversal. */
    AllocatedWorld allocate() {
        if (plan.kind() != CompositionPlan.Kind.SCENE_DEFINITION) {
            throw new IllegalArgumentException("initial world allocation requires a Scene composition plan");
        }
        allocateEntities(null, true, false);
        resolveEndpointExports();
        resolveConnections();
        return new AllocatedWorld(world, components, connections);
    }

    /** Allocates one detached reusable-definition instance under an existing live owner. */
    AllocatedInstance allocateSpawn(InternalEntity parent) {
        if (plan.kind() != CompositionPlan.Kind.ENTITY_DEFINITION) {
            throw new IllegalArgumentException("spawn allocation requires an entity-definition composition plan");
        }
        allocateEntities(Objects.requireNonNull(parent, "parent"), false, true);
        resolveEndpointExports();
        resolveConnections();
        InternalEntity root = plan.entities().stream()
                .filter(entity -> entity.parent().isEmpty())
                .findFirst()
                .map(entity -> occurrences.get(entity.occurrence()))
                .orElseThrow(() -> new IllegalStateException("entity-definition plan has no root occurrence"));
        return new AllocatedInstance(world, root, entities, components, connections);
    }

    /** Allocates every planned occurrence in deterministic owner-first order. */
    private void allocateEntities(@Nullable InternalEntity externalParent, boolean publish, boolean spawnedRoot) {
        for (CompositionEntity planned : plan.entities()) {
            boolean root = planned.parent().isEmpty();
            @Nullable
            InternalEntity parent =
                    root ? externalParent : requireOccurrence(planned.parent().orElseThrow(), planned.location());
            EntityProvenance provenance = provenance(planned, root && spawnedRoot);
            InternalEntity entity = new InternalEntity(
                    world, world.allocateEntityId(), provenance, planned.name(), planned.locallyEnabled(), parent);
            if (occurrences.putIfAbsent(planned.occurrence(), entity) != null) {
                throw new IllegalStateException("composition occurrence is duplicated: " + planned.occurrence());
            }
            entities.add(entity);
            bind(planned, entity);
            planned.components().forEach(component -> components.add(new ComponentPlan(entity, component, scopes)));
            if (publish) {
                world.addEntity(entity);
                if (parent == null) {
                    world.addRoot(entity);
                } else {
                    parent.addChild(entity);
                }
            } else if (!root) {
                Objects.requireNonNull(parent, "detached child parent").addChild(entity);
            }
        }
    }

    /** Preserves runtime provenance while keeping occurrence identity independent of runtime identity. */
    private static EntityProvenance provenance(CompositionEntity entity, boolean spawnedRoot) {
        if (spawnedRoot) {
            return EntityProvenance.spawn(entity.authoredAsset(), entity.authoredId());
        }
        return entity.instantiatedDefinition()
                .map(definition -> EntityProvenance.placement(entity.authoredAsset(), entity.authoredId(), definition))
                .orElseGet(() -> EntityProvenance.local(entity.authoredAsset(), entity.authoredId()));
    }

    /** Makes one occurrence addressable by every authored identity retained by the safe plan. */
    private void bind(CompositionEntity planned, InternalEntity entity) {
        for (CompositionEntityBinding binding : planned.bindings()) {
            scopes.require(binding.scope()).bind(binding.entity(), entity);
        }
    }

    /** Resolves planned public endpoint exports through newly allocated live identities. */
    private void resolveEndpointExports() {
        for (CompositionEndpointExport export : plan.endpointExports()) {
            EntityInstanceScope targetScope = scopes.require(export.targetScope());
            RuntimeEndpointAddress address =
                    requireEndpoint(targetScope, export.target(), export.kind(), export.location());
            EntityInstanceScope containingScope = scopes.require(export.containingScope());
            switch (export.kind()) {
                case SIGNAL -> containingScope.bindExportedSignal(export.exportedTarget(), address);
                case ACTION -> containingScope.bindExportedAction(export.exportedTarget(), address);
            }
        }
    }

    /** Resolves scoped authored connections after all occurrence identities and exports exist. */
    private void resolveConnections() {
        for (CompositionConnection planned : plan.connections()) {
            EntityInstanceScope scope = scopes.require(planned.scope());
            connections.add(new RuntimeConnectionPlan(
                    requireEndpoint(
                            scope,
                            planned.connection().signal(),
                            CompositionEndpointKind.SIGNAL,
                            planned.location() + "/signal"),
                    requireEndpoint(
                            scope,
                            planned.connection().action(),
                            CompositionEndpointKind.ACTION,
                            planned.location() + "/action"),
                    planned.location()));
        }
    }

    /** Resolves one direct or exported endpoint in its exact live definition occurrence. */
    private static RuntimeEndpointAddress requireEndpoint(
            EntityInstanceScope scope, EndpointTarget target, CompositionEndpointKind direction, String location) {
        Optional<RuntimeEndpointAddress> endpoint =
                switch (direction) {
                    case SIGNAL -> scope.findSignal(target);
                    case ACTION -> scope.findAction(target);
                };
        String displayName = direction == CompositionEndpointKind.SIGNAL ? "signal" : "action";
        return endpoint.orElseThrow(() -> new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_ENDPOINT_MISSING,
                displayName + " is absent from its live definition instance: " + target,
                location));
    }

    /** Finds a parent occurrence already allocated by owner-first plan ordering. */
    private InternalEntity requireOccurrence(CompositionOccurrenceId occurrence, String location) {
        InternalEntity entity = occurrences.get(occurrence);
        if (entity == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPOSITION_FAILED,
                    "composition plan names an unavailable parent occurrence " + occurrence,
                    location);
        }
        return entity;
    }
}
