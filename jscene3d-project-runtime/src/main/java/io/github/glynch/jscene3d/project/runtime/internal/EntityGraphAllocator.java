/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Allocates a complete ownership graph and defers every component factory invocation. */
final class EntityGraphAllocator {
    private final AssetCatalog assets;
    private final RegisteredTypeCatalog types;
    private final InternalWorld world;
    private final Map<AssetId, EntityDefinition> definitions = new LinkedHashMap<>();
    private final List<ComponentPlan> components = new ArrayList<>();
    private long nextEntityId = 1L;

    /** Stores immutable composition dependencies and creates the empty world shell. */
    EntityGraphAllocator(
            AssetCatalog assets,
            RegisteredTypeCatalog types,
            WorldDefinition definition,
            RuntimeResourceLookup resources) {
        this.assets = assets;
        this.types = types;
        world = new InternalWorld(definition, resources);
    }

    /** Expands every root and returns a complete graph with deferred component plans. */
    AllocatedWorld allocate() {
        WorldDefinition definition = world.definition();
        EntityInstanceScope scope = new EntityInstanceScope(definition.id(), InstanceOverrides.empty());
        List<EntityEntry> roots = definition.roots();
        for (int index = 0; index < roots.size(); index++) {
            allocateEntry(scope, roots.get(index), null, "/roots/" + index);
        }
        return new AllocatedWorld(world, components);
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
        InternalEntity entity = createEntity(scope.asset(), local.id(), local.name(), local.isEnabled(), parent);
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
        Map<PropertyId, ProjectValue> arguments = effectiveArguments(containingScope, placement);
        EntityInstanceScope definitionScope =
                new EntityInstanceScope(definition.id(), InstanceOverrides.resolve(definition.contract(), arguments));
        LocalEntity root = definition.root();
        Optional<String> name = placement.name().isPresent() ? placement.name() : root.name();
        InternalEntity entity =
                createEntity(containingScope.asset(), placement.id(), name, placement.isEnabled(), parent);
        containingScope.bind(placement.id(), entity);
        definitionScope.bind(root.id(), entity);
        planComponents(definitionScope, root, entity, location + "/definition/root");
        allocateChildren(definitionScope, root.children(), entity, location + "/definition/root");
        return entity;
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
            Map<PropertyId, ProjectValue> overrides = scope.overrides().component(source.id(), component.id());
            components.add(new ComponentPlan(owner, component, overrides, location + "/components/" + index));
        }
    }

    /** Creates and attaches one entity shell with a fresh world-local identity. */
    private InternalEntity createEntity(
            AssetId authoredAsset,
            EntityId authoredId,
            Optional<String> name,
            boolean locallyEnabled,
            @Nullable InternalEntity parent) {
        InternalEntity entity = new InternalEntity(
                world, new RuntimeEntityId(nextEntityId++), authoredAsset, authoredId, name, locallyEnabled, parent);
        world.addEntity(entity);
        if (parent == null) {
            world.addRoot(entity);
        } else {
            parent.addChild(entity);
        }
        return entity;
    }

    /** Loads each referenced definition once while preserving catalog diagnostics on unexpected source changes. */
    private EntityDefinition load(EntityPlacement placement) {
        AssetId id = placement.definition().id();
        EntityDefinition existing = definitions.get(id);
        if (existing != null) {
            return existing;
        }
        DefinitionLoadResult<EntityDefinition> result = assets.loadEntity(placement.definition(), types);
        if (!result.isValid()) {
            throw new RuntimeDiagnosticsException(result.diagnostics());
        }
        EntityDefinition definition = result.definition().orElseThrow();
        definitions.put(id, definition);
        return definition;
    }

    /** Merges authored placement arguments with overrides supplied through its containing contract. */
    private static Map<PropertyId, ProjectValue> effectiveArguments(
            EntityInstanceScope containingScope, EntityPlacement placement) {
        Map<PropertyId, ProjectValue> result = new LinkedHashMap<>(placement.arguments());
        result.putAll(containingScope.overrides().placement(placement.id()));
        return result;
    }
}
