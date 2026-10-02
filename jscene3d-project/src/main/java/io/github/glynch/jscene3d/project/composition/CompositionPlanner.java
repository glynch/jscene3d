/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Authoritative runtime-free interpretation of validated authored composition. */
public final class CompositionPlanner {
    /** Prevents construction of this stateless planning entry point. */
    private CompositionPlanner() {
        throw new AssertionError("CompositionPlanner cannot be instantiated");
    }

    /**
     * Plans one validated in-memory world definition without reloading its root.
     *
     * @param source absolute logical source of the supplied root
     * @param root validated in-memory world definition
     * @param resolver resolver for referenced authored or generated entity definitions
     * @param types validated inert component descriptor catalog
     * @return complete plan or ordered structured diagnostics
     */
    public static CompositionPlanResult plan(
            URI source, WorldDefinition root, DefinitionResolver resolver, RegisteredTypeCatalog types) {
        Objects.requireNonNull(root, "root");
        Planner planner = Planner.loading(source, root.id(), resolver, types);
        return planner.execute(() -> planner.planWorld(root));
    }

    /**
     * Plans one validated reusable definition for later runtime resource preparation.
     *
     * <p>Required ordinary parameters are intentionally deferred until instantiation. Required resource bindings are
     * enforced now because prepared resources are fixed for the lifetime of the prepared handle.
     *
     * @param source absolute logical source of the supplied root
     * @param root validated in-memory reusable definition
     * @param resolver resolver for nested authored or generated definitions
     * @param types validated inert component descriptor catalog
     * @param resourceBindings fixed public resource bindings
     * @return complete prepared composition plan or ordered structured diagnostics
     */
    public static CompositionPlanResult prepare(
            URI source,
            EntityDefinition root,
            DefinitionResolver resolver,
            RegisteredTypeCatalog types,
            Map<PropertyId, ProjectValue> resourceBindings) {
        Objects.requireNonNull(root, "root");
        Planner planner = Planner.loading(source, root.id(), resolver, types);
        return planner.execute(() -> {
            planner.remember(root, source);
            planner.validateResourceBindings(root.contract(), resourceBindings);
            return planner.planEntity(root, Map.of(), resourceBindings);
        });
    }

    /**
     * Replans one prepared reusable-definition graph for a concrete runtime instance.
     *
     * <p>No definition is reloaded. The immutable graph captured by preparation remains authoritative while ordinary
     * parameters are applied independently for each instance.
     *
     * @param prepared prepared entity-definition graph
     * @param types validated inert component descriptor catalog
     * @param parameters concrete public ordinary parameters
     * @param resourceBindings fixed public resource bindings from preparation
     * @return complete instance plan or ordered structured diagnostics
     */
    public static CompositionPlanResult instantiate(
            CompositionPlan prepared,
            RegisteredTypeCatalog types,
            Map<PropertyId, ProjectValue> parameters,
            Map<PropertyId, ProjectValue> resourceBindings) {
        CompositionPlan validPlan = Objects.requireNonNull(prepared, "prepared");
        if (validPlan.kind() != CompositionPlan.Kind.ENTITY_DEFINITION) {
            throw new IllegalArgumentException("prepared plan must describe an entity definition");
        }
        EntityDefinition root = Optional.ofNullable(
                        validPlan.entityDefinitions().get(validPlan.rootDefinition()))
                .orElseThrow(() -> new IllegalArgumentException("prepared plan omits its root entity definition"));
        Planner planner = Planner.preloaded(validPlan, types);
        return planner.execute(() -> {
            planner.validateParameters(root.contract(), parameters);
            planner.validateResourceBindings(root.contract(), resourceBindings);
            return planner.planEntity(root, parameters, resourceBindings);
        });
    }

    /** One isolated mutable traversal producing an immutable public plan. */
    private static final class Planner {
        private final URI rootSource;
        private final AssetId rootId;
        private final RegisteredTypeCatalog types;
        private final DefinitionLoader loader;
        private final Map<AssetId, EntityDefinition> definitions = new LinkedHashMap<>();
        private final Map<AssetId, URI> sources = new LinkedHashMap<>();
        private final List<CompositionEntity> entities = new ArrayList<>();
        private final List<CompositionConnection> connections = new ArrayList<>();
        private final List<CompositionEndpointExport> exports = new ArrayList<>();

        /** Stores one root and its definition-loading adapter. */
        private Planner(
                URI rootSource,
                AssetId rootId,
                RegisteredTypeCatalog types,
                DefinitionLoader loader,
                Map<AssetId, EntityDefinition> definitions,
                Map<AssetId, URI> sources) {
            this.rootSource = Objects.requireNonNull(rootSource, "rootSource");
            this.rootId = Objects.requireNonNull(rootId, "rootId");
            this.types = Objects.requireNonNull(types, "types");
            this.loader = Objects.requireNonNull(loader, "loader");
            this.definitions.putAll(definitions);
            this.sources.putAll(sources);
            this.sources.putIfAbsent(rootId, rootSource);
        }

        /** Creates a traversal backed by the ordinary validated definition resolver. */
        private static Planner loading(
                URI source, AssetId rootId, DefinitionResolver resolver, RegisteredTypeCatalog types) {
            DefinitionResolver validResolver = Objects.requireNonNull(resolver, "resolver");
            return new Planner(
                    source,
                    rootId,
                    types,
                    reference -> {
                        DefinitionLoadResult<EntityDefinition> loaded = validResolver.loadEntity(reference, types);
                        if (!loaded.isValid()) {
                            throw new PlanningFailure(loaded.diagnostics());
                        }
                        return new LoadedDefinition(loaded.definition().orElseThrow(), loaded.source());
                    },
                    Map.of(),
                    Map.of());
        }

        /** Creates a traversal confined to the graph captured by successful preparation. */
        private static Planner preloaded(CompositionPlan prepared, RegisteredTypeCatalog types) {
            Map<AssetId, EntityDefinition> definitions = prepared.entityDefinitions();
            DefinitionLoader loader = reference -> {
                EntityDefinition definition = definitions.get(reference.id());
                if (definition == null) {
                    throw new PlanningFailure(List.of(diagnostic(
                            prepared.rootSource(),
                            CompositionDiagnosticCode.DEFINITION_MISSING,
                            "prepared definition graph omits nested definition " + reference.id(),
                            "")));
                }
                return new LoadedDefinition(
                        definition, prepared.definitionSources().getOrDefault(reference.id(), prepared.rootSource()));
            };
            return new Planner(
                    prepared.rootSource(),
                    prepared.rootDefinition(),
                    types,
                    loader,
                    definitions,
                    prepared.definitionSources());
        }

        /** Converts all expected planning failures into the public all-or-nothing result. */
        private CompositionPlanResult execute(PlanningOperation operation) {
            try {
                return new CompositionPlanResult(Optional.of(operation.plan()), List.of());
            } catch (PlanningFailure failure) {
                return new CompositionPlanResult(Optional.empty(), failure.diagnostics());
            } catch (RuntimeException failure) {
                String detail =
                        failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                return new CompositionPlanResult(
                        Optional.empty(),
                        List.of(diagnostic(rootSource, CompositionDiagnosticCode.PLANNING_FAILED, detail, "")));
            }
        }

        /** Plans world roots and their scoped connections. */
        private CompositionPlan planWorld(WorldDefinition root) {
            CompositionOccurrenceId anchor = new CompositionOccurrenceId(root.id(), List.of());
            CompositionScope scope = new CompositionScope(root.id(), anchor);
            List<EntityEntry> roots = root.roots();
            for (int index = 0; index < roots.size(); index++) {
                planEntry(scope, roots.get(index), anchor, Optional.empty(), true, "/roots/" + index);
            }
            planConnections(scope, root.connections(), "/connections");
            return complete(CompositionPlan.Kind.WORLD);
        }

        /** Plans one reusable definition with caller-supplied root arguments. */
        private CompositionPlan planEntity(
                EntityDefinition root,
                Map<PropertyId, ProjectValue> parameters,
                Map<PropertyId, ProjectValue> resourceBindings) {
            CompositionOccurrenceId anchor = new CompositionOccurrenceId(root.id(), List.of());
            CompositionScope argumentScope = new CompositionScope(root.id(), anchor);
            Map<PropertyId, CompositionValue> arguments = scoped(resourceBindings, argumentScope);
            arguments.putAll(scoped(parameters, argumentScope));
            CompositionScope scope = new CompositionScope(root.id(), anchor);
            Overrides overrides = Overrides.resolve(root.contract(), arguments);
            LocalEntity authoredRoot = root.root();
            CompositionOccurrenceId occurrence = anchor.child(authoredRoot.id());
            planLocal(
                    scope,
                    overrides,
                    authoredRoot,
                    new OccurrenceContext(
                            occurrence,
                            Optional.empty(),
                            true,
                            "/definition/root",
                            List.of(new CompositionEntityBinding(scope, authoredRoot.id()))));
            planConnections(scope, root.connections(), "/definition/connections");
            return complete(CompositionPlan.Kind.ENTITY_DEFINITION);
        }

        /** Creates the immutable plan accumulated by this traversal. */
        private CompositionPlan complete(CompositionPlan.Kind kind) {
            return new CompositionPlan(kind, rootId, rootSource, entities, connections, exports, definitions, sources);
        }

        /** Plans one local entity or directly expands one reusable-definition placement. */
        private void planEntry(
                CompositionScope scope,
                EntityEntry entry,
                CompositionOccurrenceId parentPath,
                Optional<CompositionOccurrenceId> parent,
                boolean parentEnabled,
                String location) {
            CompositionOccurrenceId occurrence = parentPath.child(entry.id());
            if (entry instanceof LocalEntity local) {
                planLocal(
                        scope,
                        currentOverrides(scope),
                        local,
                        new OccurrenceContext(
                                occurrence,
                                parent,
                                parentEnabled,
                                location,
                                List.of(new CompositionEntityBinding(scope, local.id()))));
                return;
            }
            planPlacement(scope, (EntityPlacement) entry, occurrence, parent, parentEnabled, location);
        }

        /** Plans one local entity and its complete owned subtree. */
        private void planLocal(
                CompositionScope scope, Overrides overrides, LocalEntity local, OccurrenceContext context) {
            boolean effectiveEnabled = context.parentEnabled() && local.isEnabled();
            List<CompositionComponent> components =
                    planComponents(scope, overrides, local, context.occurrence(), context.location());
            entities.add(new CompositionEntity(
                    context.occurrence(),
                    context.parent(),
                    context.bindings(),
                    scope.definition(),
                    source(scope),
                    local.id(),
                    Optional.empty(),
                    local.name(),
                    local.isEnabled(),
                    effectiveEnabled,
                    components,
                    context.location()));
            planChildren(
                    scope, overrides, local.children(), context.occurrence(), effectiveEnabled, context.location());
        }

        /** Expands a placement directly to its referenced definition root without a wrapper occurrence. */
        private void planPlacement(
                CompositionScope containingScope,
                EntityPlacement placement,
                CompositionOccurrenceId occurrence,
                Optional<CompositionOccurrenceId> parent,
                boolean parentEnabled,
                String location) {
            EntityDefinition definition = load(placement);
            Map<PropertyId, CompositionValue> arguments = scoped(placement.arguments(), containingScope);
            arguments.putAll(currentOverrides(containingScope).placement(placement.id()));
            CompositionScope definitionScope = new CompositionScope(definition.id(), occurrence);
            Overrides overrides = Overrides.resolve(definition.contract(), arguments);
            scopeOverrides.put(definitionScope, overrides);
            LocalEntity root = definition.root();
            boolean effectiveEnabled = parentEnabled && placement.isEnabled();
            List<CompositionEntityBinding> bindings = List.of(
                    new CompositionEntityBinding(containingScope, placement.id()),
                    new CompositionEntityBinding(definitionScope, root.id()));
            List<CompositionComponent> components =
                    planComponents(definitionScope, overrides, root, occurrence, location + "/definition/root");
            entities.add(new CompositionEntity(
                    occurrence,
                    parent,
                    bindings,
                    containingScope.definition(),
                    source(containingScope),
                    placement.id(),
                    Optional.of(definition.id()),
                    placement.name().isPresent() ? placement.name() : root.name(),
                    placement.isEnabled(),
                    effectiveEnabled,
                    components,
                    location));
            planChildren(
                    definitionScope,
                    overrides,
                    root.children(),
                    occurrence,
                    effectiveEnabled,
                    location + "/definition/root");
            planConnections(definitionScope, definition.connections(), location + "/definition/connections");
            planExports(containingScope, placement, definition.contract(), definitionScope, location);
        }

        private final Map<CompositionScope, Overrides> scopeOverrides = new LinkedHashMap<>();

        /** Returns already established overrides for one containing scope. */
        private Overrides currentOverrides(CompositionScope scope) {
            return scopeOverrides.getOrDefault(scope, Overrides.empty());
        }

        /** Plans children in deterministic authored order. */
        private void planChildren(
                CompositionScope scope,
                Overrides overrides,
                List<EntityEntry> children,
                CompositionOccurrenceId parent,
                boolean parentEnabled,
                String location) {
            scopeOverrides.putIfAbsent(scope, overrides);
            for (int index = 0; index < children.size(); index++) {
                planEntry(
                        scope,
                        children.get(index),
                        parent,
                        Optional.of(parent),
                        parentEnabled,
                        location + "/children/" + index);
            }
        }

        /** Computes descriptor-ordered effective properties for local components. */
        private List<CompositionComponent> planComponents(
                CompositionScope scope,
                Overrides overrides,
                LocalEntity source,
                CompositionOccurrenceId owner,
                String location) {
            List<CompositionComponent> result = new ArrayList<>();
            List<ComponentDefinition> components = source.components();
            for (int index = 0; index < components.size(); index++) {
                ComponentDefinition component = components.get(index);
                String componentLocation = location + "/components/" + index;
                ComponentType type = new ComponentType(component.type(), component.typeVersion());
                ComponentTypeDescriptor descriptor = types.findComponent(type)
                        .orElseThrow(() -> failure(
                                source(scope),
                                CompositionDiagnosticCode.TYPE_MISSING,
                                "component descriptor is absent after validation: " + type,
                                componentLocation));
                Map<PropertyId, CompositionValue> properties = effectiveProperties(
                        descriptor, component, overrides.component(source.id(), component.id()), scope);
                result.add(new CompositionComponent(
                        owner,
                        scope,
                        source.id(),
                        component,
                        descriptor,
                        properties,
                        source(scope),
                        componentLocation));
            }
            return List.copyOf(result);
        }

        /** Preserves contract override, authored value, then descriptor default precedence. */
        private static Map<PropertyId, CompositionValue> effectiveProperties(
                ComponentTypeDescriptor descriptor,
                ComponentDefinition definition,
                Map<PropertyId, CompositionValue> overrides,
                CompositionScope localScope) {
            Map<PropertyId, CompositionValue> result = new LinkedHashMap<>();
            for (Map.Entry<PropertyId, PropertyDescriptor> property :
                    descriptor.properties().entrySet()) {
                PropertyId id = property.getKey();
                if (overrides.containsKey(id)) {
                    result.put(id, Objects.requireNonNull(overrides.get(id), "override"));
                } else if (definition.properties().containsKey(id)) {
                    result.put(
                            id,
                            new CompositionValue(
                                    Objects.requireNonNull(
                                            definition.properties().get(id), "authored property"),
                                    localScope));
                } else {
                    property.getValue()
                            .defaultValue()
                            .ifPresent(value -> result.put(id, new CompositionValue(value, localScope)));
                }
            }
            return immutableMap(result);
        }

        /** Retains authored connections with the exact scope in which their targets were declared. */
        private void planConnections(CompositionScope scope, List<SignalConnection> values, String location) {
            for (int index = 0; index < values.size(); index++) {
                connections.add(new CompositionConnection(scope, values.get(index), location + "/" + index));
            }
        }

        /** Retains public endpoint mappings after all nested mappings on which they may depend. */
        private void planExports(
                CompositionScope containingScope,
                EntityPlacement placement,
                EntityContract contract,
                CompositionScope definitionScope,
                String location) {
            contract.signals()
                    .values()
                    .forEach(signal -> exports.add(new CompositionEndpointExport(
                            containingScope,
                            EndpointTarget.placement(placement.id(), signal.id()),
                            definitionScope,
                            signal.target(),
                            CompositionEndpointKind.SIGNAL,
                            location + "/definition/contract/signals/" + signal.id())));
            contract.actions()
                    .values()
                    .forEach(action -> exports.add(new CompositionEndpointExport(
                            containingScope,
                            EndpointTarget.placement(placement.id(), action.id()),
                            definitionScope,
                            action.target(),
                            CompositionEndpointKind.ACTION,
                            location + "/definition/contract/actions/" + action.id())));
        }

        /** Loads each referenced definition once while preserving resolver diagnostics and provenance. */
        private EntityDefinition load(EntityPlacement placement) {
            AssetId id = placement.definition().id();
            EntityDefinition existing = definitions.get(id);
            if (existing != null) {
                return existing;
            }
            LoadedDefinition loaded = loader.load(placement.definition());
            remember(loaded.definition(), loaded.source());
            return loaded.definition();
        }

        /** Adds one definition and its authoritative logical source. */
        private void remember(EntityDefinition definition, URI source) {
            definitions.putIfAbsent(definition.id(), definition);
            sources.putIfAbsent(definition.id(), source);
        }

        /** Returns the source associated with one distinct authored scope. */
        private URI source(CompositionScope scope) {
            return sources.getOrDefault(scope.definition(), rootSource);
        }

        /** Requires preparation arguments to be declared compatible resource bindings. */
        private void validateResourceBindings(EntityContract contract, Map<PropertyId, ProjectValue> resourceBindings) {
            Map<PropertyId, ProjectValue> values = immutableValues(resourceBindings, "resourceBindings");
            for (Map.Entry<PropertyId, ProjectValue> argument : values.entrySet()) {
                EntityContract.ResourceBinding binding =
                        contract.resourceBindings().get(argument.getKey());
                if (binding == null || !binding.accepts(argument.getValue())) {
                    throw invalidArgument("unknown or incompatible resource binding " + argument.getKey());
                }
            }
            contract.resourceBindings().values().stream()
                    .filter(EntityContract.ResourceBinding::isRequired)
                    .map(EntityContract.ResourceBinding::id)
                    .filter(id -> !values.containsKey(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw invalidArgument("required resource binding is absent: " + id);
                    });
        }

        /** Requires concrete instance arguments to be declared compatible ordinary parameters. */
        private void validateParameters(EntityContract contract, Map<PropertyId, ProjectValue> parameters) {
            Map<PropertyId, ProjectValue> values = immutableValues(parameters, "parameters");
            for (Map.Entry<PropertyId, ProjectValue> argument : values.entrySet()) {
                EntityContract.Parameter parameter = contract.parameters().get(argument.getKey());
                if (parameter == null || !parameter.accepts(argument.getValue())) {
                    throw invalidArgument("unknown or incompatible instance parameter " + argument.getKey());
                }
            }
            contract.parameters().values().stream()
                    .filter(EntityContract.Parameter::isRequired)
                    .map(EntityContract.Parameter::id)
                    .filter(id -> !values.containsKey(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw invalidArgument("required instance parameter is absent: " + id);
                    });
        }

        /** Creates one structured internal abort. */
        private static PlanningFailure failure(
                URI source, CompositionDiagnosticCode code, String detail, String location) {
            return new PlanningFailure(List.of(diagnostic(source, code, detail, location)));
        }

        /** Creates one project diagnostic with stable technical detail. */
        private static ProjectDiagnostic diagnostic(
                URI source, CompositionDiagnosticCode code, String detail, String location) {
            return new ProjectDiagnostic(
                    ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", detail));
        }

        /** Associates portable values with the scope in which targets were authored. */
        private static Map<PropertyId, CompositionValue> scoped(
                Map<PropertyId, ProjectValue> values, CompositionScope scope) {
            Map<PropertyId, CompositionValue> result = new LinkedHashMap<>();
            immutableValues(values, "values")
                    .forEach((property, value) -> result.put(property, new CompositionValue(value, scope)));
            return result;
        }

        /** Copies one ordered property map. */
        private static Map<PropertyId, ProjectValue> immutableValues(
                Map<PropertyId, ProjectValue> values, String name) {
            Objects.requireNonNull(values, name);
            Map<PropertyId, ProjectValue> result = new LinkedHashMap<>();
            values.forEach((property, value) -> result.put(
                    Objects.requireNonNull(property, name + " key"), Objects.requireNonNull(value, name + " value")));
            return Collections.unmodifiableMap(result);
        }

        /** Creates one stable structured argument failure. */
        private PlanningFailure invalidArgument(String detail) {
            return failure(rootSource, CompositionDiagnosticCode.ARGUMENT_INVALID, detail, "/arguments");
        }
    }

    /** Contract arguments resolved within one distinct definition occurrence. */
    private static final class Overrides {
        private static final Overrides EMPTY = new Overrides(Map.of(), Map.of());

        private final Map<ComponentTarget, Map<PropertyId, CompositionValue>> components;
        private final Map<EntityId, Map<PropertyId, CompositionValue>> placements;

        /** Stores deeply immutable ordered override indexes. */
        private Overrides(
                Map<ComponentTarget, Map<PropertyId, CompositionValue>> components,
                Map<EntityId, Map<PropertyId, CompositionValue>> placements) {
            this.components = immutableNestedMap(components);
            this.placements = immutableNestedMap(placements);
        }

        /** Returns the shared empty override index. */
        private static Overrides empty() {
            return EMPTY;
        }

        /** Resolves public contract arguments to private properties or nested placement arguments. */
        private static Overrides resolve(EntityContract contract, Map<PropertyId, CompositionValue> arguments) {
            Map<ComponentTarget, Map<PropertyId, CompositionValue>> components = new LinkedHashMap<>();
            Map<EntityId, Map<PropertyId, CompositionValue>> placements = new LinkedHashMap<>();
            arguments.forEach((argument, value) -> {
                PropertyTarget target = target(contract, argument);
                if (target.component().isPresent()) {
                    ComponentTarget component = new ComponentTarget(
                            target.entity(), target.component().orElseThrow());
                    components
                            .computeIfAbsent(component, ignored -> new LinkedHashMap<>())
                            .put(target.property(), value);
                } else {
                    placements
                            .computeIfAbsent(target.entity(), ignored -> new LinkedHashMap<>())
                            .put(target.property(), value);
                }
            });
            return new Overrides(components, placements);
        }

        /** Returns effective overrides for one component. */
        private Map<PropertyId, CompositionValue> component(EntityId entity, ComponentId component) {
            return components.getOrDefault(new ComponentTarget(entity, component), Map.of());
        }

        /** Returns effective public arguments forwarded to one nested placement. */
        private Map<PropertyId, CompositionValue> placement(EntityId placement) {
            return placements.getOrDefault(placement, Map.of());
        }

        /** Finds one unique target already established by definition validation. */
        private static PropertyTarget target(EntityContract contract, PropertyId argument) {
            EntityContract.Parameter parameter = contract.parameters().get(argument);
            if (parameter != null) {
                return parameter.target();
            }
            EntityContract.ResourceBinding binding = contract.resourceBindings().get(argument);
            if (binding == null) {
                throw new IllegalStateException("validated contract argument has no target: " + argument);
            }
            return binding.target();
        }
    }

    /** Stable private component target inside one definition occurrence. */
    private record ComponentTarget(EntityId entity, ComponentId component) {
        /** Validates one target key. */
        private ComponentTarget {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(component, "component");
        }
    }

    /** Traversal context for one local entity occurrence. */
    private record OccurrenceContext(
            CompositionOccurrenceId occurrence,
            Optional<CompositionOccurrenceId> parent,
            boolean parentEnabled,
            String location,
            List<CompositionEntityBinding> bindings) {
        /** Validates and copies one local occurrence context. */
        private OccurrenceContext {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(location, "location");
            bindings = List.copyOf(bindings);
        }
    }

    /** Loads one validated entity definition with its logical source. */
    @FunctionalInterface
    private interface DefinitionLoader {
        /** Returns the referenced definition or raises a structured planning failure. */
        LoadedDefinition load(AssetRef<EntityDefinition> reference);
    }

    /** One loaded definition and its diagnostic source. */
    private record LoadedDefinition(EntityDefinition definition, URI source) {
        /** Validates one loaded definition. */
        private LoadedDefinition {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(source, "source");
        }
    }

    /** Internal operation producing one plan. */
    @FunctionalInterface
    private interface PlanningOperation {
        /** Executes one isolated traversal. */
        CompositionPlan plan();
    }

    /** Internal structured abort never exposed through the public interface. */
    private static final class PlanningFailure extends RuntimeException {
        private final List<ProjectDiagnostic> diagnostics;

        /** Stores ordered diagnostics without synthesizing an exception message. */
        private PlanningFailure(List<ProjectDiagnostic> diagnostics) {
            this.diagnostics = List.copyOf(diagnostics);
        }

        /** Returns ordered diagnostics. */
        private List<ProjectDiagnostic> diagnostics() {
            return diagnostics;
        }
    }

    /** Deeply copies a two-level ordered override map. */
    private static <K> Map<K, Map<PropertyId, CompositionValue>> immutableNestedMap(
            Map<K, Map<PropertyId, CompositionValue>> source) {
        Map<K, Map<PropertyId, CompositionValue>> result = new LinkedHashMap<>();
        source.forEach((key, values) -> result.put(key, immutableMap(values)));
        return Collections.unmodifiableMap(result);
    }

    /** Copies one ordered map. */
    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
