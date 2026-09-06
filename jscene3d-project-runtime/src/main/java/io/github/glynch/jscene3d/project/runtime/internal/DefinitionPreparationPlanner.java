/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads and plans one complete reusable-definition graph without allocating live entities. */
final class DefinitionPreparationPlanner {
    private final DefinitionResolver resolver;
    private final RegisteredTypeCatalog types;
    private final Map<AssetId, EntityDefinition> definitions = new LinkedHashMap<>();
    private final List<ComponentPreparationPlan> components = new ArrayList<>();

    /** Stores the validated catalog collaborators used by this one preparation. */
    DefinitionPreparationPlanner(DefinitionResolver resolver, RegisteredTypeCatalog types) {
        this.resolver = resolver;
        this.types = types;
    }

    /** Loads the transitive graph and plans each component in deterministic authored order. */
    PreparedGraph plan(AssetRef<EntityDefinition> reference, Map<PropertyId, ProjectValue> resourceBindings) {
        DefinitionLoadResult<EntityDefinition> loaded = resolver.loadEntity(reference, types);
        if (!loaded.isValid()) {
            throw new RuntimeDiagnosticsException(loaded.diagnostics());
        }
        EntityDefinition definition = loaded.definition().orElseThrow();
        definitions.put(definition.id(), definition);
        validateResourceBindings(definition.contract(), resourceBindings);
        EntityInstanceScope argumentScope = new EntityInstanceScope(definition.id(), InstanceOverrides.empty());
        Map<PropertyId, ScopedProjectValue> scopedBindings = scoped(resourceBindings, argumentScope);
        EntityInstanceScope definitionScope = new EntityInstanceScope(
                definition.id(), InstanceOverrides.resolve(definition.contract(), scopedBindings));
        planLocal(definitionScope, definition.root(), "/definition/root");
        return new PreparedGraph(definition, definitions, components, loaded.source());
    }

    /** Plans one local entity and each owned child entry. */
    private void planLocal(EntityInstanceScope scope, LocalEntity entity, String location) {
        List<ComponentDefinition> authoredComponents = entity.components();
        for (int index = 0; index < authoredComponents.size(); index++) {
            ComponentDefinition component = authoredComponents.get(index);
            components.add(new ComponentPreparationPlan(
                    scope,
                    entity.id(),
                    component,
                    scope.overrides().component(entity.id(), component.id()),
                    location + "/components/" + index));
        }
        List<EntityEntry> children = entity.children();
        for (int index = 0; index < children.size(); index++) {
            planEntry(scope, children.get(index), location + "/children/" + index);
        }
    }

    /** Plans one inline child or nested reusable-definition placement. */
    private void planEntry(EntityInstanceScope scope, EntityEntry entry, String location) {
        if (entry instanceof LocalEntity local) {
            planLocal(scope, local, location);
            return;
        }
        EntityPlacement placement = (EntityPlacement) entry;
        EntityDefinition nested = load(placement);
        Map<PropertyId, ScopedProjectValue> arguments = effectiveArguments(scope, placement);
        EntityInstanceScope nestedScope =
                new EntityInstanceScope(nested.id(), InstanceOverrides.resolve(nested.contract(), arguments));
        planLocal(nestedScope, nested.root(), location + "/definition/root");
    }

    /** Loads one nested definition exactly once for this prepared graph. */
    private EntityDefinition load(EntityPlacement placement) {
        EntityDefinition existing = definitions.get(placement.definition().id());
        if (existing != null) {
            return existing;
        }
        DefinitionLoadResult<EntityDefinition> loaded = resolver.loadEntity(placement.definition(), types);
        if (!loaded.isValid()) {
            throw new RuntimeDiagnosticsException(loaded.diagnostics());
        }
        EntityDefinition definition = loaded.definition().orElseThrow();
        definitions.put(definition.id(), definition);
        return definition;
    }

    /** Resolves authored nested-placement arguments and containing resource overrides. */
    private static Map<PropertyId, ScopedProjectValue> effectiveArguments(
            EntityInstanceScope containingScope, EntityPlacement placement) {
        Map<PropertyId, ScopedProjectValue> result = scoped(placement.arguments(), containingScope);
        result.putAll(containingScope.overrides().placement(placement.id()));
        return result;
    }

    /** Associates portable argument values with the scope in which their targets were authored. */
    private static Map<PropertyId, ScopedProjectValue> scoped(
            Map<PropertyId, ProjectValue> values, EntityInstanceScope scope) {
        Map<PropertyId, ScopedProjectValue> result = new LinkedHashMap<>();
        values.forEach((property, value) -> result.put(property, new ScopedProjectValue(value, scope)));
        return result;
    }

    /** Requires preparation arguments to be declared compatible resource bindings. */
    private static void validateResourceBindings(
            EntityContract contract, Map<PropertyId, ProjectValue> resourceBindings) {
        for (Map.Entry<PropertyId, ProjectValue> argument : resourceBindings.entrySet()) {
            EntityContract.ResourceBinding binding = contract.resourceBindings().get(argument.getKey());
            if (binding == null || !binding.accepts(argument.getValue())) {
                throw invalidArgument("unknown or incompatible resource binding " + argument.getKey());
            }
        }
        contract.resourceBindings().values().stream()
                .filter(EntityContract.ResourceBinding::isRequired)
                .map(EntityContract.ResourceBinding::id)
                .filter(id -> !resourceBindings.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw invalidArgument("required resource binding is absent: " + id);
                });
    }

    /** Creates one stable structured preparation-argument failure. */
    private static RuntimeCompositionException invalidArgument(String detail) {
        return new RuntimeCompositionException(RuntimeDiagnosticCode.SPAWN_ARGUMENT_INVALID, detail, "/arguments");
    }

    /** Immutable loaded graph and component preparation work. */
    record PreparedGraph(
            EntityDefinition definition,
            Map<AssetId, EntityDefinition> definitions,
            List<ComponentPreparationPlan> components,
            URI source) {
        /** Copies the complete preparation result. */
        PreparedGraph {
            definitions = Map.copyOf(definitions);
            components = List.copyOf(components);
        }
    }
}
