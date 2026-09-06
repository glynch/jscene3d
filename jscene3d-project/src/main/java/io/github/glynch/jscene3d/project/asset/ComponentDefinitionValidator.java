/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Validates locally authored components through the registered component descriptor seam. */
final class ComponentDefinitionValidator {
    private final RegisteredTypeCatalog catalog;
    private final DiagnosticCollector diagnostics;
    private final Map<EntityId, EntityComponents> entities = new LinkedHashMap<>();
    private final Set<EntityId> addressableEntities = new HashSet<>();

    /** Stores one source-local component validation context. */
    private ComponentDefinitionValidator(RegisteredTypeCatalog catalog, Path source) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        diagnostics = new DiagnosticCollector(source);
    }

    /** Validates one reusable entity definition and its local connections. */
    static Validation validateEntity(
            LocalEntity root,
            EntityContract contract,
            List<SignalConnection> connections,
            RegisteredTypeCatalog catalog,
            Path source) {
        ComponentDefinitionValidator validator = new ComponentDefinitionValidator(catalog, source);
        validator.validateLocalEntity(root, "/root");
        validator.validateEntries(root.children(), "/root/children");
        validator.validateLocalTargetValues(root, "/root");
        validator.validateContract(contract);
        validator.validateConnections(connections);
        return new Validation(validator.entities, validator.diagnostics.diagnostics());
    }

    /** Validates one world definition and its local connections. */
    static Validation validateWorld(
            List<? extends EntityEntry> roots,
            List<SignalConnection> connections,
            RegisteredTypeCatalog catalog,
            Path source) {
        ComponentDefinitionValidator validator = new ComponentDefinitionValidator(catalog, source);
        validator.validateEntries(roots, "/roots");
        validator.validateTargetValues(roots, "/roots");
        validator.validateConnections(connections);
        return new Validation(validator.entities, validator.diagnostics.diagnostics());
    }

    /** Validates authored entries in deterministic hierarchy order. */
    private void validateEntries(List<? extends EntityEntry> entries, String location) {
        for (int index = 0; index < entries.size(); index++) {
            EntityEntry entry = entries.get(index);
            if (entry instanceof LocalEntity entity) {
                String entityLocation = location + "/" + index;
                validateLocalEntity(entity, entityLocation);
                validateEntries(entity.children(), entityLocation + "/children");
            } else {
                addressableEntities.add(entry.id());
            }
        }
    }

    /** Resolves and validates every component on one entity. */
    private void validateLocalEntity(LocalEntity entity, String location) {
        addressableEntities.add(entity.id());
        Map<ComponentId, ResolvedComponent> resolved = new LinkedHashMap<>();
        List<ComponentDefinition> components = entity.components();
        for (int index = 0; index < components.size(); index++) {
            ComponentDefinition component = components.get(index);
            String componentLocation = location + "/components/" + index;
            resolve(component, componentLocation).ifPresent(value -> resolved.put(component.id(), value));
        }
        EntityComponents entityComponents = new EntityComponents(resolved);
        entities.put(entity.id(), entityComponents);
        validateMultiplicity(entityComponents, location);
        validateConflicts(entityComponents, location);
        validateCapabilities(entityComponents, location);
    }

    /** Resolves an exact descriptor and validates authored property values. */
    private Optional<ResolvedComponent> resolve(ComponentDefinition component, String location) {
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        Optional<ComponentTypeDescriptor> descriptor = catalog.findComponent(type);
        if (descriptor.isEmpty()) {
            error(
                    AssetDiagnosticCode.COMPONENT_TYPE_MISSING,
                    "component type is not registered: " + type,
                    location + "/type");
            return Optional.empty();
        }
        ComponentTypeDescriptor resolved = descriptor.orElseThrow();
        validateProperties(component, resolved, location + "/properties");
        return Optional.of(new ResolvedComponent(component, resolved));
    }

    /** Validates every authored target after the complete source-local identity index exists. */
    private void validateTargetValues(List<? extends EntityEntry> entries, String location) {
        for (int index = 0; index < entries.size(); index++) {
            EntityEntry entry = entries.get(index);
            String entryLocation = location + "/" + index;
            if (entry instanceof LocalEntity local) {
                validateLocalTargetValues(local, entryLocation);
            } else if (entry instanceof EntityPlacement placement) {
                validateValueTargets(placement.arguments(), entryLocation + "/arguments");
            }
        }
    }

    /** Validates target-valued properties and descendants of one local entity. */
    private void validateLocalTargetValues(LocalEntity local, String location) {
        validateComponentTargetValues(local.components(), location + "/components");
        validateTargetValues(local.children(), location + "/children");
    }

    /** Validates target-valued properties on locally authored components. */
    private void validateComponentTargetValues(List<ComponentDefinition> components, String location) {
        for (int index = 0; index < components.size(); index++) {
            validateValueTargets(components.get(index).properties(), location + "/" + index + "/properties");
        }
    }

    /** Validates target values in one named value map while preserving diagnostic locations. */
    private void validateValueTargets(Map<?, ProjectValue> values, String location) {
        values.forEach((key, value) -> validateValueTarget(value, location + "/" + key));
    }

    /** Recursively validates one target value without interpreting ordinary object data. */
    private void validateValueTarget(ProjectValue value, String location) {
        switch (value) {
            case ProjectValue.EntityTargetValue target -> validateEntityTarget(target.entity(), location);
            case ProjectValue.ComponentTargetValue target -> validateComponentTarget(target.target(), location);
            case ProjectValue.ArrayValue array -> {
                for (int index = 0; index < array.values().size(); index++) {
                    validateValueTarget(array.values().get(index), location + "/" + index);
                }
            }
            case ProjectValue.ObjectValue object -> validateValueTargets(object.values(), location);
            default -> {
                // Scalar and resource-reference values carry no local entity identity.
            }
        }
    }

    /** Requires an entity target to identify a local entity or placement in the same authored asset. */
    private void validateEntityTarget(EntityId entity, String location) {
        if (!addressableEntities.contains(entity)) {
            error(
                    AssetDiagnosticCode.TARGET_INVALID,
                    "entity target does not exist in this asset: " + entity,
                    location);
        }
    }

    /** Requires a component target to identify a locally authored component without crossing a placement seam. */
    private void validateComponentTarget(ComponentTarget target, String location) {
        if (localComponent(target.entity(), target.component()).isEmpty()) {
            error(
                    AssetDiagnosticCode.TARGET_INVALID,
                    "component target does not identify a local component: " + target,
                    location);
        }
    }

    /** Validates required, unknown, and structurally invalid component properties. */
    private void validateProperties(
            ComponentDefinition component, ComponentTypeDescriptor descriptor, String location) {
        for (Map.Entry<PropertyId, ProjectValue> entry : component.properties().entrySet()) {
            PropertyDescriptor property = descriptor.properties().get(entry.getKey());
            if (property == null) {
                error(
                        AssetDiagnosticCode.COMPONENT_PROPERTY_UNKNOWN,
                        "component property is not declared: " + entry.getKey(),
                        location + "/" + entry.getKey());
            } else if (!property.accepts(entry.getValue())) {
                error(
                        AssetDiagnosticCode.COMPONENT_PROPERTY_VALUE_INVALID,
                        "component property value is invalid: " + entry.getKey(),
                        location + "/" + entry.getKey());
            }
        }
        for (Map.Entry<PropertyId, PropertyDescriptor> entry :
                descriptor.properties().entrySet()) {
            if (entry.getValue().isRequired() && !component.properties().containsKey(entry.getKey())) {
                error(
                        AssetDiagnosticCode.COMPONENT_PROPERTY_REQUIRED,
                        "required component property is missing: " + entry.getKey(),
                        location + "/" + entry.getKey());
            }
        }
    }

    /** Rejects repeated single-instance component types. */
    private void validateMultiplicity(EntityComponents entity, String location) {
        Map<ComponentTypeId, Integer> counts = new HashMap<>();
        for (ResolvedComponent component : entity.components()) {
            counts.merge(component.descriptor().type().id(), 1, Integer::sum);
        }
        for (ResolvedComponent component : entity.components()) {
            ComponentTypeDescriptor descriptor = component.descriptor();
            if (descriptor.multiplicity() == ComponentMultiplicity.SINGLE
                    && counts.getOrDefault(descriptor.type().id(), 0) > 1) {
                error(
                        AssetDiagnosticCode.COMPONENT_MULTIPLICITY_INVALID,
                        "component type permits only one instance per entity: "
                                + descriptor.type().id(),
                        location + "/components");
                counts.put(descriptor.type().id(), 0);
            }
        }
    }

    /** Rejects descriptor-declared type conflicts on one entity. */
    private void validateConflicts(EntityComponents entity, String location) {
        Set<ComponentTypeId> present = new HashSet<>();
        entity.components().stream()
                .map(component -> component.descriptor().type().id())
                .forEach(present::add);
        Set<String> reported = new HashSet<>();
        for (ResolvedComponent component : entity.components()) {
            ComponentTypeId type = component.descriptor().type().id();
            for (ComponentTypeId conflict : component.descriptor().conflicts()) {
                if (present.contains(conflict) && reported.add(conflictKey(type, conflict))) {
                    error(
                            AssetDiagnosticCode.COMPONENT_CONFLICT,
                            "component types conflict: " + type + " and " + conflict,
                            location + "/components");
                }
            }
        }
    }

    /** Requires each declared dependency capability to have exactly one sibling provider. */
    private void validateCapabilities(EntityComponents entity, String location) {
        Map<CapabilityId, Integer> providers = new HashMap<>();
        for (ResolvedComponent component : entity.components()) {
            component
                    .descriptor()
                    .providedCapabilities()
                    .forEach(capability -> providers.merge(capability, 1, Integer::sum));
        }
        for (ResolvedComponent component : entity.components()) {
            for (CapabilityId capability : component.descriptor().requiredCapabilities()) {
                int count = providers.getOrDefault(capability, 0);
                if (count == 0) {
                    error(
                            AssetDiagnosticCode.COMPONENT_CAPABILITY_MISSING,
                            "required capability has no sibling provider: " + capability,
                            location + "/components");
                } else if (count > 1) {
                    error(
                            AssetDiagnosticCode.COMPONENT_CAPABILITY_AMBIGUOUS,
                            "required capability has multiple sibling providers: " + capability,
                            location + "/components");
                }
            }
        }
    }

    /** Validates public declarations backed by locally authored components. */
    private void validateContract(EntityContract contract) {
        contract.parameters().values().forEach(this::validateParameter);
        contract.resourceBindings().values().forEach(this::validateResourceBinding);
        contract.signals().values().forEach(this::validateSignal);
        contract.actions().values().forEach(this::validateAction);
        contract.attachments().values().forEach(this::validateAttachment);
    }

    /** Validates one locally backed public parameter. */
    private void validateParameter(EntityContract.Parameter parameter) {
        localProperty(parameter.target()).ifPresent(property -> {
            if (property.valueKind() != parameter.valueKind()) {
                incompatibleContractMember(parameter.id(), parameter.target().property());
            }
        });
    }

    /** Validates one locally backed public resource binding. */
    private void validateResourceBinding(EntityContract.ResourceBinding binding) {
        localProperty(binding.target()).ifPresent(property -> {
            if (property.valueKind() != ProjectValueKind.REFERENCE
                    || !acceptsAll(property.acceptedReferenceKinds(), binding.acceptedKinds())) {
                incompatibleContractMember(binding.id(), binding.target().property());
            }
        });
    }

    /** Validates one locally backed public signal. */
    private void validateSignal(EntityContract.Signal signal) {
        localSignal(signal.target(), true).ifPresent(endpoint -> {
            if (!endpoint.payload().equals(signal.payload())) {
                incompatibleContractMember(signal.id(), signal.target().endpoint());
            }
        });
    }

    /** Validates one locally backed public action. */
    private void validateAction(EntityContract.Action action) {
        localAction(action.target(), true).ifPresent(endpoint -> {
            if (!endpoint.payload().equals(action.payload())) {
                incompatibleContractMember(action.id(), action.target().endpoint());
            }
        });
    }

    /** Validates one locally backed public spatial attachment. */
    private void validateAttachment(EntityContract.Attachment attachment) {
        SpatialTarget target = attachment.target();
        if (target.component().isEmpty()) {
            return;
        }
        ResolvedComponent component = localComponent(
                        target.entity(), target.component().orElseThrow())
                .orElse(null);
        if (component == null) {
            return;
        }
        if (target.attachment().isPresent()
                && !component
                        .descriptor()
                        .attachments()
                        .contains(target.attachment().orElseThrow())) {
            error(
                    AssetDiagnosticCode.COMPONENT_ATTACHMENT_MISSING,
                    "component attachment is not declared: "
                            + target.attachment().orElseThrow(),
                    "");
        } else if (target.attachment().isEmpty()
                && component.descriptor().spatialDomain() == ComponentSpatialDomain.NONE) {
            error(AssetDiagnosticCode.TARGET_INVALID, "component does not supply spatial state", "");
        }
    }

    /** Validates local endpoints used by authored connections. */
    private void validateConnections(List<SignalConnection> connections) {
        for (int index = 0; index < connections.size(); index++) {
            SignalConnection connection = connections.get(index);
            Optional<EndpointDescriptor> signal = localSignal(connection.signal(), true);
            Optional<EndpointDescriptor> action = localAction(connection.action(), true);
            if (signal.isPresent()
                    && action.isPresent()
                    && !signal.orElseThrow()
                            .payload()
                            .equals(action.orElseThrow().payload())) {
                error(
                        AssetDiagnosticCode.CONTRACT_PAYLOAD_INVALID,
                        "connected component endpoint payloads are incompatible",
                        "/connections/" + index);
            }
        }
    }

    /** Resolves one locally backed contract property and reports an absent declaration. */
    private Optional<PropertyDescriptor> localProperty(PropertyTarget target) {
        if (target.component().isEmpty()) {
            return Optional.empty();
        }
        Optional<ResolvedComponent> component =
                localComponent(target.entity(), target.component().orElseThrow());
        if (component.isEmpty()) {
            return Optional.empty();
        }
        PropertyDescriptor property =
                component.orElseThrow().descriptor().properties().get(target.property());
        if (property == null) {
            error(
                    AssetDiagnosticCode.COMPONENT_PROPERTY_MISSING,
                    "component property target is not declared: " + target.property(),
                    "");
            return Optional.empty();
        }
        return Optional.of(property);
    }

    /** Resolves one local signal, optionally reporting an absent declaration. */
    private Optional<EndpointDescriptor> localSignal(EndpointTarget target, boolean reportMissing) {
        return localEndpoint(target, true, reportMissing);
    }

    /** Resolves one local action, optionally reporting an absent declaration. */
    private Optional<EndpointDescriptor> localAction(EndpointTarget target, boolean reportMissing) {
        return localEndpoint(target, false, reportMissing);
    }

    /** Resolves one component endpoint through its exact descriptor. */
    private Optional<EndpointDescriptor> localEndpoint(EndpointTarget target, boolean signal, boolean reportMissing) {
        if (target.component().isEmpty()) {
            return Optional.empty();
        }
        Optional<ResolvedComponent> component =
                localComponent(target.entity(), target.component().orElseThrow());
        if (component.isEmpty()) {
            return Optional.empty();
        }
        Map<EndpointId, EndpointDescriptor> endpoints = signal
                ? component.orElseThrow().descriptor().signals()
                : component.orElseThrow().descriptor().actions();
        EndpointDescriptor endpoint = endpoints.get(target.endpoint());
        if (endpoint == null && reportMissing) {
            error(
                    AssetDiagnosticCode.COMPONENT_ENDPOINT_MISSING,
                    (signal ? "signal" : "action") + " is not declared: " + target.endpoint(),
                    "");
        }
        return Optional.ofNullable(endpoint);
    }

    /** Resolves one already indexed local component. */
    private Optional<ResolvedComponent> localComponent(EntityId entity, ComponentId component) {
        EntityComponents entityComponents = entities.get(entity);
        return entityComponents == null ? Optional.empty() : entityComponents.find(component);
    }

    /** Returns whether every exported reference kind is accepted by its private property. */
    private static boolean acceptsAll(Set<ResourceReference.Kind> target, Set<ResourceReference.Kind> exported) {
        return target.isEmpty() || !exported.isEmpty() && target.containsAll(exported);
    }

    /** Produces a stable unordered key for a conflicting type pair. */
    private static String conflictKey(ComponentTypeId first, ComponentTypeId second) {
        String left = first.value();
        String right = second.value();
        return left.compareTo(right) <= 0 ? left + '\n' + right : right + '\n' + left;
    }

    /** Reports incompatible public and private declaration types. */
    private void incompatibleContractMember(Object exported, Object target) {
        error(
                AssetDiagnosticCode.CONTRACT_PAYLOAD_INVALID,
                "contract member " + exported + " is incompatible with target " + target,
                "");
    }

    /** Adds one source-local structured error. */
    private void error(AssetDiagnosticCode code, String detail, String location) {
        diagnostics.error(code, detail, location);
    }

    /** Catalog-aware validation result retained for cross-seam endpoint checks. */
    static final class Validation {
        private final Map<EntityId, EntityComponents> entities;
        private final List<ProjectDiagnostic> diagnostics;

        /** Copies one completed validation result. */
        private Validation(Map<EntityId, EntityComponents> entities, List<ProjectDiagnostic> diagnostics) {
            this.entities = Collections.unmodifiableMap(new LinkedHashMap<>(entities));
            this.diagnostics = List.copyOf(diagnostics);
        }

        /** Returns ordered diagnostics. */
        List<ProjectDiagnostic> diagnostics() {
            return diagnostics;
        }

        /** Resolves one local signal without adding another diagnostic. */
        Optional<EndpointDescriptor> signal(EndpointTarget target) {
            return endpoint(target, true);
        }

        /** Resolves one local action without adding another diagnostic. */
        Optional<EndpointDescriptor> action(EndpointTarget target) {
            return endpoint(target, false);
        }

        /** Resolves one local endpoint from the immutable validation index. */
        private Optional<EndpointDescriptor> endpoint(EndpointTarget target, boolean signal) {
            if (target.component().isEmpty()) {
                return Optional.empty();
            }
            EntityComponents entity = entities.get(target.entity());
            if (entity == null) {
                return Optional.empty();
            }
            return entity.find(target.component().orElseThrow()).flatMap(component -> {
                Map<EndpointId, EndpointDescriptor> endpoints = signal
                        ? component.descriptor().signals()
                        : component.descriptor().actions();
                return Optional.ofNullable(endpoints.get(target.endpoint()));
            });
        }
    }

    /** Descriptor-resolved components belonging to one local entity. */
    private static final class EntityComponents {
        private final Map<ComponentId, ResolvedComponent> components;

        /** Copies one component index. */
        private EntityComponents(Map<ComponentId, ResolvedComponent> components) {
            this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
        }

        /** Returns components in authored order. */
        private List<ResolvedComponent> components() {
            return List.copyOf(components.values());
        }

        /** Finds a component by stable local identity. */
        private Optional<ResolvedComponent> find(ComponentId id) {
            return Optional.ofNullable(components.get(id));
        }
    }

    /** One authored component paired with its exact safe descriptor. */
    private record ResolvedComponent(ComponentDefinition definition, ComponentTypeDescriptor descriptor) {
        /** Validates resolved component state. */
        private ResolvedComponent {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }
}
