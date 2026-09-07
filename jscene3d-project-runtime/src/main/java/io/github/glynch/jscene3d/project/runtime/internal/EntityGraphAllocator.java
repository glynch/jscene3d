/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Allocates a complete ownership graph and defers every component factory invocation. */
final class EntityGraphAllocator {
    private final DefinitionResolver definitionResolver;
    private final RegisteredTypeCatalog types;
    private final InternalWorld world;
    private final Map<AssetId, EntityDefinition> definitions = new LinkedHashMap<>();
    private final List<InternalEntity> entities = new ArrayList<>();
    private final List<ComponentPlan> components = new ArrayList<>();
    private final List<RuntimeConnectionPlan> connections = new ArrayList<>();
    private boolean preparedOnly;
    private boolean publish = true;

    /** Stores the world and its immutable composition dependencies. */
    EntityGraphAllocator(InternalWorld world) {
        this.world = world;
        definitionResolver = world.definitionResolver();
        types = world.types();
    }

    /** Expands every root and returns a complete graph with deferred component plans. */
    AllocatedWorld allocate() {
        WorldDefinition definition = world.definition();
        EntityInstanceScope scope = new EntityInstanceScope(definition.id(), InstanceOverrides.empty());
        List<EntityEntry> roots = definition.roots();
        for (int index = 0; index < roots.size(); index++) {
            allocateEntry(scope, roots.get(index), null, "/roots/" + index);
        }
        planConnections(scope, definition.connections(), "/connections");
        return new AllocatedWorld(world, components, connections);
    }

    /** Allocates one complete reusable-definition instance without publishing it into the live world. */
    AllocatedInstance allocateSpawn(
            InternalPreparedEntityDefinition prepared,
            InternalEntity parent,
            Map<PropertyId, ProjectValue> parameters) {
        preparedOnly = true;
        publish = false;
        definitions.putAll(prepared.definitions());
        EntityDefinition definition = prepared.definition();
        validateParameters(definition.contract(), parameters);
        EntityInstanceScope argumentScope = new EntityInstanceScope(definition.id(), InstanceOverrides.empty());
        Map<PropertyId, ScopedProjectValue> arguments = scoped(prepared.resourceBindings(), argumentScope);
        arguments.putAll(scoped(parameters, argumentScope));
        EntityInstanceScope scope =
                new EntityInstanceScope(definition.id(), InstanceOverrides.resolve(definition.contract(), arguments));
        LocalEntity authoredRoot = definition.root();
        InternalEntity root = createDetachedRoot(definition, authoredRoot, parent);
        scope.bind(authoredRoot.id(), root);
        planComponents(scope, authoredRoot, root, "/definition/root");
        allocateChildren(scope, authoredRoot.children(), root, "/definition/root");
        planConnections(scope, definition.connections(), "/definition/connections");
        return new AllocatedInstance(world, root, entities, components, connections);
    }

    /** Creates the unpublished root of one runtime definition instance. */
    private InternalEntity createDetachedRoot(
            EntityDefinition definition, LocalEntity authoredRoot, InternalEntity parent) {
        InternalEntity root = new InternalEntity(
                world,
                world.allocateEntityId(),
                EntityProvenance.spawn(definition.id(), authoredRoot.id()),
                authoredRoot.name(),
                authoredRoot.isEnabled(),
                parent);
        entities.add(root);
        return root;
    }

    /** Allocates a local entity or expands a reusable-definition placement. */
    private InternalEntity allocateEntry(
            EntityInstanceScope scope, EntityEntry entry, @Nullable InternalEntity parent, String location) {
        if (entry instanceof LocalEntity local) {
            return allocateLocal(scope, local, parent, location);
        }
        return allocatePlacement(scope, (EntityPlacement) entry, parent, location);
    }

    /** Allocates one locally authored entity and its complete owned subtree. */
    private InternalEntity allocateLocal(
            EntityInstanceScope scope, LocalEntity local, @Nullable InternalEntity parent, String location) {
        InternalEntity entity = createEntity(
                EntityProvenance.local(scope.asset(), local.id()), local.name(), local.isEnabled(), parent);
        scope.bind(local.id(), entity);
        planComponents(scope, local, entity, location);
        allocateChildren(scope, local.children(), entity, location);
        return entity;
    }

    /** Expands one placed definition directly into the placement's root entity. */
    private InternalEntity allocatePlacement(
            EntityInstanceScope containingScope,
            EntityPlacement placement,
            @Nullable InternalEntity parent,
            String location) {
        EntityDefinition definition = load(placement);
        Map<PropertyId, ScopedProjectValue> arguments = effectiveArguments(containingScope, placement);
        EntityInstanceScope definitionScope =
                new EntityInstanceScope(definition.id(), InstanceOverrides.resolve(definition.contract(), arguments));
        LocalEntity root = definition.root();
        Optional<String> name = placement.name().isPresent() ? placement.name() : root.name();
        InternalEntity entity = createEntity(
                EntityProvenance.placement(containingScope.asset(), placement.id(), definition.id()),
                name,
                placement.isEnabled(),
                parent);
        containingScope.bind(placement.id(), entity);
        definitionScope.bind(root.id(), entity);
        planComponents(definitionScope, root, entity, location + "/definition/root");
        allocateChildren(definitionScope, root.children(), entity, location + "/definition/root");
        planConnections(definitionScope, definition.connections(), location + "/definition/connections");
        bindContractEndpoints(containingScope, placement, definition.contract(), definitionScope, location);
        return entity;
    }

    /** Resolves one definition placement's exported endpoints through its private instance scope. */
    private static void bindContractEndpoints(
            EntityInstanceScope containingScope,
            EntityPlacement placement,
            EntityContract contract,
            EntityInstanceScope definitionScope,
            String location) {
        for (EntityContract.Signal signal : contract.signals().values()) {
            RuntimeEndpointAddress address = requireEndpoint(
                    definitionScope,
                    signal.target(),
                    EndpointDirection.SIGNAL,
                    location + "/definition/contract/signals/" + signal.id());
            containingScope.bindExportedSignal(EndpointTarget.placement(placement.id(), signal.id()), address);
        }
        for (EntityContract.Action action : contract.actions().values()) {
            RuntimeEndpointAddress address = requireEndpoint(
                    definitionScope,
                    action.target(),
                    EndpointDirection.ACTION,
                    location + "/definition/contract/actions/" + action.id());
            containingScope.bindExportedAction(EndpointTarget.placement(placement.id(), action.id()), address);
        }
    }

    /** Allocates owned child entries in deterministic authored order. */
    private void allocateChildren(
            EntityInstanceScope scope, List<EntityEntry> children, InternalEntity parent, String location) {
        for (int index = 0; index < children.size(); index++) {
            allocateEntry(scope, children.get(index), parent, location + "/children/" + index);
        }
    }

    /** Defers every local component until the complete entity graph exists. */
    private void planComponents(EntityInstanceScope scope, LocalEntity source, InternalEntity owner, String location) {
        List<ComponentDefinition> componentDefinitions = source.components();
        for (int index = 0; index < componentDefinitions.size(); index++) {
            ComponentDefinition component = componentDefinitions.get(index);
            Map<PropertyId, ScopedProjectValue> overrides = scope.overrides().component(source.id(), component.id());
            components.add(new ComponentPlan(
                    owner, scope, source.id(), component, overrides, location + "/components/" + index));
        }
    }

    /** Resolves authored connections to exact live addresses while retaining declaration order. */
    private void planConnections(EntityInstanceScope scope, List<SignalConnection> values, String location) {
        for (int index = 0; index < values.size(); index++) {
            SignalConnection connection = values.get(index);
            String connectionLocation = location + "/" + index;
            connections.add(new RuntimeConnectionPlan(
                    requireEndpoint(
                            scope, connection.signal(), EndpointDirection.SIGNAL, connectionLocation + "/signal"),
                    requireEndpoint(
                            scope, connection.action(), EndpointDirection.ACTION, connectionLocation + "/action"),
                    connectionLocation));
        }
    }

    /** Resolves one direct or exported endpoint after allocation of its complete definition instance. */
    private static RuntimeEndpointAddress requireEndpoint(
            EntityInstanceScope scope, EndpointTarget target, EndpointDirection direction, String location) {
        Optional<RuntimeEndpointAddress> endpoint =
                switch (direction) {
                    case SIGNAL -> scope.findSignal(target);
                    case ACTION -> scope.findAction(target);
                };
        return endpoint.orElseThrow(() -> new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_ENDPOINT_MISSING,
                direction.displayName() + " is absent from its live definition instance: " + target,
                location));
    }

    /** Closed endpoint directions used during authored connection resolution. */
    private enum EndpointDirection {
        SIGNAL("signal"),
        ACTION("action");

        private final String displayName;

        /** Stores the diagnostic display name. */
        EndpointDirection(String displayName) {
            this.displayName = displayName;
        }

        /** Returns the diagnostic display name. */
        private String displayName() {
            return displayName;
        }
    }

    /** Creates and attaches one entity shell with a fresh world-local identity. */
    private InternalEntity createEntity(
            EntityProvenance provenance,
            Optional<String> name,
            boolean locallyEnabled,
            @Nullable InternalEntity parent) {
        InternalEntity entity =
                new InternalEntity(world, world.allocateEntityId(), provenance, name, locallyEnabled, parent);
        entities.add(entity);
        if (publish) {
            world.addEntity(entity);
            if (parent == null) {
                world.addRoot(entity);
            } else {
                parent.addChild(entity);
            }
        } else {
            Objects.requireNonNull(parent, "detached child parent").addChild(entity);
        }
        return entity;
    }

    /** Loads each referenced definition once while preserving source diagnostics on unexpected content changes. */
    private EntityDefinition load(EntityPlacement placement) {
        AssetId id = placement.definition().id();
        EntityDefinition existing = definitions.get(id);
        if (existing != null) {
            return existing;
        }
        if (preparedOnly) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.ENTITY_PREPARATION_FAILED,
                    "prepared definition graph omits nested definition " + id,
                    "");
        }
        DefinitionLoadResult<EntityDefinition> result = definitionResolver.loadEntity(placement.definition(), types);
        if (!result.isValid()) {
            throw new RuntimeDiagnosticsException(result.diagnostics());
        }
        EntityDefinition definition = result.definition().orElseThrow();
        definitions.put(id, definition);
        return definition;
    }

    /** Merges authored placement arguments with overrides supplied through its containing contract. */
    private static Map<PropertyId, ScopedProjectValue> effectiveArguments(
            EntityInstanceScope containingScope, EntityPlacement placement) {
        Map<PropertyId, ScopedProjectValue> result = new LinkedHashMap<>();
        placement
                .arguments()
                .forEach((property, value) -> result.put(property, new ScopedProjectValue(value, containingScope)));
        result.putAll(containingScope.overrides().placement(placement.id()));
        return result;
    }

    /** Associates portable argument values with their caller's definition-instance scope. */
    private static Map<PropertyId, ScopedProjectValue> scoped(
            Map<PropertyId, ProjectValue> values, EntityInstanceScope scope) {
        Map<PropertyId, ScopedProjectValue> result = new LinkedHashMap<>();
        values.forEach((property, value) -> result.put(property, new ScopedProjectValue(value, scope)));
        return result;
    }

    /** Requires instance arguments to be declared compatible ordinary parameters. */
    private static void validateParameters(EntityContract contract, Map<PropertyId, ProjectValue> parameters) {
        for (Map.Entry<PropertyId, ProjectValue> argument : parameters.entrySet()) {
            EntityContract.Parameter parameter = contract.parameters().get(argument.getKey());
            if (parameter == null || !parameter.accepts(argument.getValue())) {
                throw new RuntimeCompositionException(
                        RuntimeDiagnosticCode.SPAWN_ARGUMENT_INVALID,
                        "unknown or incompatible spawn parameter " + argument.getKey(),
                        "/arguments");
            }
        }
        contract.parameters().values().stream()
                .filter(EntityContract.Parameter::isRequired)
                .map(EntityContract.Parameter::id)
                .filter(id -> !parameters.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new RuntimeCompositionException(
                            RuntimeDiagnosticCode.SPAWN_ARGUMENT_INVALID,
                            "required spawn parameter is absent: " + id,
                            "/arguments");
                });
    }
}
