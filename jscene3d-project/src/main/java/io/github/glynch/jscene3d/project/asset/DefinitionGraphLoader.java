/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.asset.ComponentDefinitionValidator.Validation;
import io.github.glynch.jscene3d.project.asset.internal.DefinitionDocumentReader;
import io.github.glynch.jscene3d.project.component.AttachmentPointId;
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
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Resolves typed assets and validates their transitive entity-definition graph. */
final class DefinitionGraphLoader {
    private final AssetCatalog catalog;
    private final @Nullable RegisteredTypeCatalog componentTypes;
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();
    private final Map<AssetId, EntityDefinition> loadedEntities = new HashMap<>();
    private final Set<AssetId> validatedEntities = new HashSet<>();
    private final LinkedHashSet<AssetId> visitingEntities = new LinkedHashSet<>();

    /** Stores one catalog-local graph load. */
    private DefinitionGraphLoader(AssetCatalog catalog, @Nullable RegisteredTypeCatalog componentTypes) {
        this.catalog = catalog;
        this.componentTypes = componentTypes;
    }

    /** Loads one entity definition and validates its complete inclusion graph. */
    static DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetCatalog catalog, AssetRef<EntityDefinition> reference) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog, null);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.ENTITY_DEFINITION, catalog.root(), "");
        Optional<EntityDefinition> definition = metadata.flatMap(loader::readEntity);
        metadata.ifPresent(value -> definition.ifPresent(asset -> loader.validateEntityGraph(value, asset)));
        return loader.result(definition);
    }

    /** Loads one entity graph and validates its components through safe descriptor metadata. */
    static DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetCatalog catalog, AssetRef<EntityDefinition> reference, RegisteredTypeCatalog componentTypes) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog, componentTypes);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.ENTITY_DEFINITION, catalog.root(), "");
        Optional<EntityDefinition> definition = metadata.flatMap(loader::readEntity);
        metadata.ifPresent(value -> definition.ifPresent(asset -> loader.validateEntityGraph(value, asset)));
        return loader.result(definition);
    }

    /** Loads one world and validates every transitively placed entity definition. */
    static DefinitionLoadResult<WorldDefinition> loadWorld(AssetCatalog catalog, AssetRef<WorldDefinition> reference) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog, null);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.WORLD_DEFINITION, catalog.root(), "");
        Optional<WorldDefinition> definition = metadata.flatMap(loader::readWorld);
        if (metadata.isPresent() && definition.isPresent()) {
            WorldDefinition world = definition.orElseThrow();
            Path source = metadata.orElseThrow().path();
            loader.validateEntries(world.roots(), EntityContract.empty(), source);
            Validation components = loader.validateWorldComponents(world.roots(), world.connections(), source);
            loader.validateConnections(world.connections(), loader.indexPlacements(world.roots()), components, source);
        }
        return loader.result(definition);
    }

    /** Loads one world graph and validates its components through safe descriptor metadata. */
    static DefinitionLoadResult<WorldDefinition> loadWorld(
            AssetCatalog catalog, AssetRef<WorldDefinition> reference, RegisteredTypeCatalog componentTypes) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog, componentTypes);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.WORLD_DEFINITION, catalog.root(), "");
        Optional<WorldDefinition> definition = metadata.flatMap(loader::readWorld);
        if (metadata.isPresent() && definition.isPresent()) {
            WorldDefinition world = definition.orElseThrow();
            Path source = metadata.orElseThrow().path();
            loader.validateEntries(world.roots(), EntityContract.empty(), source);
            Validation components = loader.validateWorldComponents(world.roots(), world.connections(), source);
            loader.validateConnections(world.connections(), loader.indexPlacements(world.roots()), components, source);
        }
        return loader.result(definition);
    }

    /** Reads one entity definition and appends its source-local diagnostics. */
    private Optional<EntityDefinition> readEntity(AssetMetadata metadata) {
        DefinitionDocumentReader.ReadResult<EntityDefinition> result =
                DefinitionDocumentReader.readEntity(catalog.root(), metadata);
        diagnostics.addAll(result.diagnostics());
        return result.value();
    }

    /** Reads one world definition and appends its source-local diagnostics. */
    private Optional<WorldDefinition> readWorld(AssetMetadata metadata) {
        DefinitionDocumentReader.ReadResult<WorldDefinition> result =
                DefinitionDocumentReader.readWorld(catalog.root(), metadata);
        diagnostics.addAll(result.diagnostics());
        return result.value();
    }

    /** Validates one entity's transitive placement graph with cycle detection and memoization. */
    private void validateEntityGraph(AssetMetadata metadata, EntityDefinition definition) {
        AssetId id = metadata.id();
        if (validatedEntities.contains(id)) {
            return;
        }
        if (!visitingEntities.add(id)) {
            addCycle(metadata.path(), id);
            return;
        }
        loadedEntities.put(id, definition);
        validateEntries(List.of(definition.root()), definition.contract(), metadata.path());
        Map<EntityId, EntityPlacement> placements = indexPlacements(List.of(definition.root()));
        validateContractSeams(definition.contract(), placements, metadata.path());
        Validation components = validateEntityComponents(definition, metadata.path());
        validateConnections(definition.connections(), placements, components, metadata.path());
        visitingEntities.remove(id);
        validatedEntities.add(id);
    }

    /** Validates components when a safe component catalog was supplied. */
    private @Nullable Validation validateEntityComponents(EntityDefinition definition, Path source) {
        if (componentTypes == null) {
            return null;
        }
        Validation validation = ComponentDefinitionValidator.validateEntity(
                definition.root(), definition.contract(), definition.connections(), componentTypes, source);
        diagnostics.addAll(validation.diagnostics());
        return validation;
    }

    /** Validates world components when a safe component catalog was supplied. */
    private @Nullable Validation validateWorldComponents(
            List<? extends EntityEntry> roots, List<SignalConnection> connections, Path source) {
        if (componentTypes == null) {
            return null;
        }
        Validation validation = ComponentDefinitionValidator.validateWorld(roots, connections, componentTypes, source);
        diagnostics.addAll(validation.diagnostics());
        return validation;
    }

    /** Traverses placements in deterministic authored hierarchy order. */
    private void validateEntries(List<? extends EntityEntry> entries, EntityContract contract, Path source) {
        for (EntityEntry entry : entries) {
            if (entry instanceof EntityPlacement placement) {
                validatePlacement(placement, contract, source);
            } else if (entry instanceof LocalEntity local) {
                validateEntries(local.children(), contract, source);
            }
        }
    }

    /** Resolves and recursively validates one placed reusable entity definition. */
    private void validatePlacement(EntityPlacement placement, EntityContract containingContract, Path source) {
        AssetId id = placement.definition().id();
        Optional<AssetMetadata> metadata = resolve(id, AssetKind.ENTITY_DEFINITION, source, "");
        if (metadata.isEmpty()) {
            return;
        }
        EntityDefinition definition = loadedEntities.get(id);
        if (definition == null) {
            Optional<EntityDefinition> loaded = readEntity(metadata.orElseThrow());
            if (loaded.isEmpty()) {
                return;
            }
            definition = loaded.orElseThrow();
            loadedEntities.put(id, definition);
        }
        validatePlacementArguments(placement, definition.contract(), containingContract, source);
        if (visitingEntities.contains(id)) {
            addCycle(source, id);
            return;
        }
        if (validatedEntities.contains(id)) {
            return;
        }
        validateEntityGraph(metadata.orElseThrow(), definition);
    }

    /** Validates supplied and required arguments against one placed definition's exported contract. */
    private void validatePlacementArguments(
            EntityPlacement placement, EntityContract target, EntityContract containingContract, Path source) {
        for (Map.Entry<PropertyId, ProjectValue> argument :
                placement.arguments().entrySet()) {
            EntityContract.Parameter parameter = target.parameters().get(argument.getKey());
            EntityContract.ResourceBinding binding = target.resourceBindings().get(argument.getKey());
            if (parameter == null && binding == null) {
                addError(
                        source,
                        AssetDiagnosticCode.CONTRACT_ARGUMENT_UNKNOWN,
                        "placement " + placement.id() + " supplies undeclared argument " + argument.getKey(),
                        "");
            } else if (parameter != null && !parameter.accepts(argument.getValue())
                    || binding != null && !binding.accepts(argument.getValue())) {
                addError(
                        source,
                        AssetDiagnosticCode.CONTRACT_ARGUMENT_TYPE,
                        "placement " + placement.id() + " supplies an incompatible value for " + argument.getKey(),
                        "");
            }
        }
        Set<PropertyId> reexported = reexportedArguments(containingContract, placement.id());
        target.parameters().values().stream()
                .filter(EntityContract.Parameter::isRequired)
                .map(EntityContract.Parameter::id)
                .forEach(id -> requireArgument(placement, id, reexported, source));
        target.resourceBindings().values().stream()
                .filter(EntityContract.ResourceBinding::isRequired)
                .map(EntityContract.ResourceBinding::id)
                .forEach(id -> requireArgument(placement, id, reexported, source));
    }

    /** Returns nested placement arguments supplied through the containing definition's public contract. */
    private Set<PropertyId> reexportedArguments(EntityContract contract, EntityId placement) {
        Set<PropertyId> result = new HashSet<>();
        contract.parameters().values().stream()
                .map(EntityContract.Parameter::target)
                .filter(target -> isPlacementTarget(target, placement))
                .map(PropertyTarget::property)
                .forEach(result::add);
        contract.resourceBindings().values().stream()
                .map(EntityContract.ResourceBinding::target)
                .filter(target -> isPlacementTarget(target, placement))
                .map(PropertyTarget::property)
                .forEach(result::add);
        return result;
    }

    /** Reports one omitted argument unless it is supplied by a containing public contract. */
    private void requireArgument(EntityPlacement placement, PropertyId id, Set<PropertyId> reexported, Path source) {
        if (!placement.arguments().containsKey(id) && !reexported.contains(id)) {
            addError(
                    source,
                    AssetDiagnosticCode.CONTRACT_ARGUMENT_REQUIRED,
                    "placement " + placement.id() + " omits required argument " + id,
                    "");
        }
    }

    /** Validates public members which deliberately re-export a nested placement contract. */
    private void validateContractSeams(
            EntityContract contract, Map<EntityId, EntityPlacement> placements, Path source) {
        contract.parameters().values().forEach(parameter -> validateReexportedParameter(parameter, placements, source));
        contract.resourceBindings()
                .values()
                .forEach(binding -> validateReexportedResourceBinding(binding, placements, source));
        contract.signals().values().forEach(signal -> validateReexportedSignal(signal, placements, source));
        contract.actions().values().forEach(action -> validateReexportedAction(action, placements, source));
        contract.attachments()
                .values()
                .forEach(attachment -> validateReexportedAttachment(attachment, placements, source));
    }

    /** Validates one parameter re-exported from a nested placement. */
    private void validateReexportedParameter(
            EntityContract.Parameter parameter, Map<EntityId, EntityPlacement> placements, Path source) {
        PropertyTarget target = parameter.target();
        if (target.component().isPresent()) {
            return;
        }
        EntityContract.Parameter nested = nestedContract(target.entity(), placements)
                .map(EntityContract::parameters)
                .map(parameters -> parameters.get(target.property()))
                .orElse(null);
        if (nested == null) {
            missingMember(source, target.entity(), target.property());
        } else if (nested.valueKind() != parameter.valueKind()) {
            incompatiblePayload(source, parameter.id(), target.property());
        }
    }

    /** Validates one resource binding re-exported from a nested placement. */
    private void validateReexportedResourceBinding(
            EntityContract.ResourceBinding binding, Map<EntityId, EntityPlacement> placements, Path source) {
        PropertyTarget target = binding.target();
        if (target.component().isPresent()) {
            return;
        }
        EntityContract.ResourceBinding nested = nestedContract(target.entity(), placements)
                .map(EntityContract::resourceBindings)
                .map(bindings -> bindings.get(target.property()))
                .orElse(null);
        if (nested == null) {
            missingMember(source, target.entity(), target.property());
        } else if (!acceptsAll(nested.acceptedKinds(), binding.acceptedKinds())) {
            incompatiblePayload(source, binding.id(), target.property());
        }
    }

    /** Validates one attachment re-exported from a nested placement. */
    private void validateReexportedAttachment(
            EntityContract.Attachment attachment, Map<EntityId, EntityPlacement> placements, Path source) {
        SpatialTarget target = attachment.target();
        if (target.component().isPresent() || target.attachment().isEmpty()) {
            return;
        }
        AttachmentPointId id = target.attachment().orElseThrow();
        boolean exists = nestedContract(target.entity(), placements)
                .map(EntityContract::attachments)
                .map(attachments -> attachments.containsKey(id))
                .orElse(false);
        if (!exists) {
            missingMember(source, target.entity(), id);
        }
    }

    /** Validates one re-exported signal and its payload declaration. */
    private void validateReexportedSignal(
            EntityContract.Signal signal, Map<EntityId, EntityPlacement> placements, Path source) {
        EndpointTarget target = signal.target();
        if (target.component().isPresent()) {
            return;
        }
        EntityContract.Signal nested = nestedContract(target.entity(), placements)
                .map(EntityContract::signals)
                .map(signals -> signals.get(target.endpoint()))
                .orElse(null);
        if (nested == null) {
            missingMember(source, target.entity(), target.endpoint());
        } else if (!nested.payload().equals(signal.payload())) {
            incompatiblePayload(source, signal.id(), target.endpoint());
        }
    }

    /** Validates one re-exported action and its payload declaration. */
    private void validateReexportedAction(
            EntityContract.Action action, Map<EntityId, EntityPlacement> placements, Path source) {
        EndpointTarget target = action.target();
        if (target.component().isPresent()) {
            return;
        }
        EntityContract.Action nested = nestedContract(target.entity(), placements)
                .map(EntityContract::actions)
                .map(actions -> actions.get(target.endpoint()))
                .orElse(null);
        if (nested == null) {
            missingMember(source, target.entity(), target.endpoint());
        } else if (!nested.payload().equals(action.payload())) {
            incompatiblePayload(source, action.id(), target.endpoint());
        }
    }

    /** Validates placed-definition endpoints used by authored signal/action connections. */
    private void validateConnections(
            List<SignalConnection> connections,
            Map<EntityId, EntityPlacement> placements,
            @Nullable Validation components,
            Path source) {
        for (SignalConnection connection : connections) {
            if (connection.signal().component().isPresent()
                    && connection.action().component().isPresent()) {
                continue;
            }
            Optional<EndpointSignature> signal = resolveSignal(connection.signal(), placements, components, source);
            Optional<EndpointSignature> action = resolveAction(connection.action(), placements, components, source);
            if (signal.isPresent()
                    && action.isPresent()
                    && !signal.orElseThrow().equals(action.orElseThrow())) {
                incompatiblePayload(
                        source,
                        connection.signal().endpoint(),
                        connection.action().endpoint());
            }
        }
    }

    /** Resolves a placed signal declaration, reporting a missing exported member. */
    private Optional<EndpointSignature> resolveSignal(
            EndpointTarget target,
            Map<EntityId, EntityPlacement> placements,
            @Nullable Validation components,
            Path source) {
        if (target.component().isPresent()) {
            return components == null
                    ? Optional.empty()
                    : components.signal(target).map(EndpointSignature::from);
        }
        if (!isPlacedEndpoint(target, placements)) {
            return Optional.empty();
        }
        EntityContract.Signal signal = nestedContract(target.entity(), placements)
                .map(EntityContract::signals)
                .map(signals -> signals.get(target.endpoint()))
                .orElse(null);
        if (signal == null) {
            missingMember(source, target.entity(), target.endpoint());
            return Optional.empty();
        }
        return Optional.of(EndpointSignature.from(signal));
    }

    /** Resolves a placed action declaration, reporting a missing exported member. */
    private Optional<EndpointSignature> resolveAction(
            EndpointTarget target,
            Map<EntityId, EntityPlacement> placements,
            @Nullable Validation components,
            Path source) {
        if (target.component().isPresent()) {
            return components == null
                    ? Optional.empty()
                    : components.action(target).map(EndpointSignature::from);
        }
        if (!isPlacedEndpoint(target, placements)) {
            return Optional.empty();
        }
        EntityContract.Action action = nestedContract(target.entity(), placements)
                .map(EntityContract::actions)
                .map(actions -> actions.get(target.endpoint()))
                .orElse(null);
        if (action == null) {
            missingMember(source, target.entity(), target.endpoint());
            return Optional.empty();
        }
        return Optional.of(EndpointSignature.from(action));
    }

    /** Looks up the resolved contract of one placement in the current asset. */
    private Optional<EntityContract> nestedContract(EntityId placementId, Map<EntityId, EntityPlacement> placements) {
        EntityPlacement placement = placements.get(placementId);
        if (placement == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadedEntities.get(placement.definition().id()))
                .map(EntityDefinition::contract);
    }

    /** Returns all placements within an authored hierarchy by stable identity. */
    private Map<EntityId, EntityPlacement> indexPlacements(List<? extends EntityEntry> entries) {
        Map<EntityId, EntityPlacement> placements = new HashMap<>();
        indexPlacements(entries, placements);
        return placements;
    }

    /** Accumulates placements in authored hierarchy order. */
    private void indexPlacements(List<? extends EntityEntry> entries, Map<EntityId, EntityPlacement> placements) {
        for (EntityEntry entry : entries) {
            if (entry instanceof EntityPlacement placement) {
                placements.put(placement.id(), placement);
            } else if (entry instanceof LocalEntity local) {
                indexPlacements(local.children(), placements);
            }
        }
    }

    /** Returns whether a property target addresses one nested placement contract. */
    private boolean isPlacementTarget(PropertyTarget target, EntityId placement) {
        return target.component().isEmpty() && target.entity().equals(placement);
    }

    /** Returns whether an endpoint target addresses a placement rather than a local component. */
    private boolean isPlacedEndpoint(EndpointTarget target, Map<EntityId, EntityPlacement> placements) {
        return target.component().isEmpty() && placements.containsKey(target.entity());
    }

    /** Returns whether every value accepted by an outer binding is accepted by its nested target. */
    private boolean acceptsAll(Set<?> nested, Set<?> outer) {
        return nested.isEmpty() || !outer.isEmpty() && nested.containsAll(outer);
    }

    /** Reports an absent public contract member. */
    private void missingMember(Path source, EntityId placement, Object member) {
        addError(
                source,
                AssetDiagnosticCode.CONTRACT_MEMBER_MISSING,
                "placement " + placement + " does not export " + member,
                "");
    }

    /** Reports incompatible public contract declarations. */
    private void incompatiblePayload(Path source, Object exported, Object target) {
        addError(
                source,
                AssetDiagnosticCode.CONTRACT_PAYLOAD_INVALID,
                "contract member " + exported + " is incompatible with target " + target,
                "");
    }

    /** Resolves one stable identity and validates its expected kind. */
    private Optional<AssetMetadata> resolve(AssetId id, AssetKind expectedKind, Path source, String location) {
        Optional<AssetMetadata> metadata = catalog.find(id);
        if (metadata.isEmpty()) {
            addError(source, AssetDiagnosticCode.REFERENCE_MISSING, "asset ID was not found: " + id, location);
            return Optional.empty();
        }
        if (metadata.orElseThrow().kind() != expectedKind) {
            addError(
                    source,
                    AssetDiagnosticCode.REFERENCE_KIND_INVALID,
                    "asset " + id + " has kind " + metadata.orElseThrow().kind() + " but " + expectedKind
                            + " is required",
                    location);
            return Optional.empty();
        }
        return metadata;
    }

    /** Reports one inclusion cycle with its stable identity chain. */
    private void addCycle(Path source, AssetId repeatedId) {
        List<String> chain = visitingEntities.stream().map(AssetId::toString).collect(Collectors.toList());
        chain.add(repeatedId.toString());
        addError(
                source,
                AssetDiagnosticCode.DEFINITION_CYCLE,
                "definition inclusion cycle: " + String.join(" -> ", chain),
                "");
    }

    /** Appends one structured graph diagnostic. */
    private void addError(Path source, AssetDiagnosticCode code, String detail, String location) {
        DiagnosticCollector collector = new DiagnosticCollector(source);
        collector.error(code, detail, location);
        diagnostics.addAll(collector.diagnostics());
    }

    /** Produces the public result, withholding the root definition after any transitive error. */
    private <T> DefinitionLoadResult<T> result(Optional<T> value) {
        boolean hasErrors =
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        return new DefinitionLoadResult<>(hasErrors ? Optional.empty() : value, diagnostics);
    }

    /** Exact optional payload declaration used to compare local and placed endpoints uniformly.
     *
     * @param payload registered payload type, or {@code null} for a payload-free endpoint
     */
    private record EndpointSignature(@Nullable RegisteredType payload) {
        /** Creates a signature from one local component endpoint. */
        private static EndpointSignature from(EndpointDescriptor endpoint) {
            return new EndpointSignature(endpoint.payload().orElse(null));
        }

        /** Creates a signature from one exported signal. */
        private static EndpointSignature from(EntityContract.Signal signal) {
            return new EndpointSignature(signal.payload().orElse(null));
        }

        /** Creates a signature from one exported action. */
        private static EndpointSignature from(EntityContract.Action action) {
            return new EndpointSignature(action.payload().orElse(null));
        }
    }
}
