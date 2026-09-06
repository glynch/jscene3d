/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.asset.AssetDiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
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
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectIdentifiers;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Strict JSON decoding and structural validation for authored definition assets. */
public final class DefinitionDocumentReader {
    private static final int FORMAT_VERSION = 1;
    private static final String ENTITY_SCHEMA = "https://jscene3d.org/schemas/entity-definition-1.json";
    private static final String WORLD_SCHEMA = "https://jscene3d.org/schemas/world-definition-1.json";
    private static final AssetId INVALID_ASSET_ID = new AssetId(new UUID(0, 0));

    private final Path projectRoot;
    private final AssetMetadata metadata;
    private final DiagnosticCollector diagnostics;
    private final ProjectValueDecoder values;
    private final Set<EntityId> entityIds = new HashSet<>();
    private long placeholderSequence = 1;

    /** Stores one source-local validation context. */
    private DefinitionDocumentReader(Path projectRoot, AssetMetadata metadata) {
        this.projectRoot = projectRoot;
        this.metadata = metadata;
        diagnostics = new DiagnosticCollector(metadata.path());
        values = ProjectValueDecoder.withReferences(this::decodeReferenceValue);
    }

    /**
     * Reads one complete entity-definition document matching catalog metadata.
     *
     * @param projectRoot normalized project root
     * @param metadata catalog metadata
     * @return immutable definition and ordered diagnostics
     */
    public static ReadResult<EntityDefinition> readEntity(Path projectRoot, AssetMetadata metadata) {
        DefinitionDocumentReader reader = new DefinitionDocumentReader(projectRoot, metadata);
        try (InputStream input = Files.newInputStream(metadata.path())) {
            RawDefinitionDocuments.EntityDocument raw =
                    ProjectJsonReader.strict().read(input, RawDefinitionDocuments.EntityDocument.class);
            return reader.validateEntity(raw);
        } catch (JsonProcessingException exception) {
            reader.diagnostics.error(
                    AssetDiagnosticCode.JSON_INVALID,
                    "entity definition is invalid JSON: " + exception.getOriginalMessage(),
                    "");
        } catch (IOException exception) {
            reader.diagnostics.error(
                    AssetDiagnosticCode.FILE_READ_FAILED,
                    "entity definition cannot be read: " + exception.getMessage(),
                    "");
        }
        return reader.failure();
    }

    /**
     * Reads one complete world-definition document matching catalog metadata.
     *
     * @param projectRoot normalized project root
     * @param metadata catalog metadata
     * @return immutable definition and ordered diagnostics
     */
    public static ReadResult<WorldDefinition> readWorld(Path projectRoot, AssetMetadata metadata) {
        DefinitionDocumentReader reader = new DefinitionDocumentReader(projectRoot, metadata);
        try (InputStream input = Files.newInputStream(metadata.path())) {
            RawDefinitionDocuments.WorldDocument raw =
                    ProjectJsonReader.strict().read(input, RawDefinitionDocuments.WorldDocument.class);
            return reader.validateWorld(raw);
        } catch (JsonProcessingException exception) {
            reader.diagnostics.error(
                    AssetDiagnosticCode.JSON_INVALID,
                    "world definition is invalid JSON: " + exception.getOriginalMessage(),
                    "");
        } catch (IOException exception) {
            reader.diagnostics.error(
                    AssetDiagnosticCode.FILE_READ_FAILED,
                    "world definition cannot be read: " + exception.getMessage(),
                    "");
        }
        return reader.failure();
    }

    /** Validates one reusable entity-definition document in source order. */
    private ReadResult<EntityDefinition> validateEntity(RawDefinitionDocuments.@Nullable EntityDocument raw) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.JSON_INVALID, "asset document must be a JSON object", "");
            return failure();
        }
        AssetId id = validateEnvelope(
                raw.schema(), raw.assetId(), raw.assetType(), raw.formatVersion(), AssetKind.ENTITY_DEFINITION);
        String name = requiredName(raw.name(), "/name");
        LocalEntity root = validateRoot(raw.root(), "/root");
        EntityContract contract = validateContract(raw.contract(), "/contract");
        List<SignalConnection> connections = validateConnections(raw.connections(), "/connections");
        if (diagnostics.hasErrors()) {
            return failure();
        }
        try {
            return success(new EntityDefinition(id, name, contract, connections, root));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.TARGET_INVALID, exception.toString(), "");
            return failure();
        }
    }

    /** Validates one world-definition document in source order. */
    private ReadResult<WorldDefinition> validateWorld(RawDefinitionDocuments.@Nullable WorldDocument raw) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.JSON_INVALID, "asset document must be a JSON object", "");
            return failure();
        }
        AssetId id = validateEnvelope(
                raw.schema(), raw.assetId(), raw.assetType(), raw.formatVersion(), AssetKind.WORLD_DEFINITION);
        String name = requiredName(raw.name(), "/name");
        List<EntityEntry> roots = validateEntries(raw.roots(), "/roots");
        List<SignalConnection> connections = validateConnections(raw.connections(), "/connections");
        if (diagnostics.hasErrors()) {
            return failure();
        }
        try {
            return success(new WorldDefinition(id, name, connections, roots));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.TARGET_INVALID, exception.toString(), "");
            return failure();
        }
    }

    /** Validates the shared asset envelope against immutable catalog metadata. */
    private AssetId validateEnvelope(
            @Nullable String schema,
            @Nullable String rawId,
            @Nullable String rawKind,
            int formatVersion,
            AssetKind expectedKind) {
        AssetId id = parseAssetId(rawId, "/assetId");
        if (!id.equals(INVALID_ASSET_ID) && !id.equals(metadata.id())) {
            diagnostics.error(
                    AssetDiagnosticCode.CATALOG_STALE,
                    "assetId changed from " + metadata.id() + " to " + id,
                    "/assetId");
        }
        if (!expectedKind.serializedName().equals(rawKind)) {
            diagnostics.error(
                    AssetDiagnosticCode.KIND_INVALID,
                    "assetType must be " + expectedKind.serializedName() + ": " + rawKind,
                    "/assetType");
        }
        if (metadata.kind() != expectedKind) {
            diagnostics.error(
                    AssetDiagnosticCode.CATALOG_STALE,
                    "catalog kind " + metadata.kind() + " does not match requested kind " + expectedKind,
                    "/assetType");
        }
        if (formatVersion != FORMAT_VERSION || metadata.formatVersion() != FORMAT_VERSION) {
            diagnostics.error(
                    AssetDiagnosticCode.FORMAT_UNSUPPORTED,
                    "formatVersion must be " + FORMAT_VERSION + ": " + formatVersion,
                    "/formatVersion");
        }
        String expectedSchema = expectedKind == AssetKind.ENTITY_DEFINITION ? ENTITY_SCHEMA : WORLD_SCHEMA;
        if (schema != null && !expectedSchema.equals(schema)) {
            diagnostics.warning(
                    AssetDiagnosticCode.SCHEMA_URI_INVALID,
                    "$schema does not identify the expected version-one schema",
                    "/$schema");
        }
        return id;
    }

    /** Requires the entity-definition root to be a local entity. */
    private LocalEntity validateRoot(RawDefinitionDocuments.@Nullable Entry raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "entity-definition root is required", location);
            return placeholderLocalEntity();
        }
        EntityEntry entry = validateEntry(raw, location);
        if (entry instanceof LocalEntity local) {
            return local;
        }
        diagnostics.error(AssetDiagnosticCode.ENTRY_INVALID, "entity-definition root must be local", location);
        return placeholderLocalEntity();
    }

    /** Validates nullable entry arrays while preserving declaration order. */
    private List<EntityEntry> validateEntries(
            @Nullable List<RawDefinitionDocuments.@Nullable Entry> rawEntries, String location) {
        if (rawEntries == null) {
            return List.of();
        }
        List<EntityEntry> entries = new ArrayList<>();
        for (int index = 0; index < rawEntries.size(); index++) {
            RawDefinitionDocuments.Entry raw = rawEntries.get(index);
            if (raw == null) {
                diagnostics.error(
                        AssetDiagnosticCode.FIELD_REQUIRED, "entity entry is required", location + "/" + index);
            } else {
                entries.add(validateEntry(raw, location + "/" + index));
            }
        }
        return List.copyOf(entries);
    }

    /** Validates one discriminated local entity or definition placement. */
    private EntityEntry validateEntry(RawDefinitionDocuments.Entry raw, String location) {
        EntityId id = parseEntityId(raw.entityId(), location + "/entityId");
        Optional<String> name = optionalName(raw.name(), location + "/name");
        boolean enabled = raw.enabled() == null || raw.enabled();
        if ("local".equals(raw.entryType())) {
            if (raw.definition() != null || raw.arguments() != null) {
                diagnostics.error(
                        AssetDiagnosticCode.ENTRY_INVALID,
                        "local entry cannot declare definition or arguments",
                        location);
            }
            List<ComponentDefinition> components = validateComponents(raw.components(), location + "/components");
            List<EntityEntry> children = validateEntries(raw.children(), location + "/children");
            return name.map(value -> new LocalEntity(id, value, enabled, components, children))
                    .orElseGet(() -> new LocalEntity(id, enabled, components, children));
        }
        if ("placement".equals(raw.entryType())) {
            if (raw.components() != null || raw.children() != null) {
                diagnostics.error(
                        AssetDiagnosticCode.ENTRY_INVALID, "placement cannot declare components or children", location);
            }
            AssetRef<EntityDefinition> reference = validateReference(raw.definition(), location + "/definition");
            Map<PropertyId, ProjectValue> arguments = validatePropertyValues(raw.arguments(), location + "/arguments");
            return name.map(value -> new EntityPlacement(id, value, enabled, reference, arguments))
                    .orElseGet(() -> new EntityPlacement(id, enabled, reference, arguments));
        }
        diagnostics.error(
                AssetDiagnosticCode.ENTRY_INVALID,
                "entryType must be local or placement: " + raw.entryType(),
                location + "/entryType");
        return name.map(value -> new LocalEntity(id, value, enabled, List.of(), List.of()))
                .orElseGet(() -> new LocalEntity(id, enabled, List.of(), List.of()));
    }

    /** Validates component records and rejects duplicate identities on one entity. */
    private List<ComponentDefinition> validateComponents(
            @Nullable List<RawDefinitionDocuments.@Nullable Component> rawComponents, String location) {
        if (rawComponents == null) {
            return List.of();
        }
        List<ComponentDefinition> components = new ArrayList<>();
        Set<ComponentId> ids = new HashSet<>();
        for (int index = 0; index < rawComponents.size(); index++) {
            RawDefinitionDocuments.Component raw = rawComponents.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "component is required", itemLocation);
                continue;
            }
            ComponentId id = parseComponentId(raw.componentId(), itemLocation + "/componentId");
            if (!ids.add(id)) {
                diagnostics.error(
                        AssetDiagnosticCode.COMPONENT_ID_DUPLICATE,
                        "componentId is duplicated on this entity: " + id,
                        itemLocation + "/componentId");
                id = placeholderComponentId();
            }
            ComponentTypeId type = validateComponentType(raw.type(), itemLocation + "/type");
            int typeVersion = validateComponentVersion(raw.typeVersion(), itemLocation + "/typeVersion");
            Map<PropertyId, ProjectValue> properties =
                    validatePropertyValues(raw.properties(), itemLocation + "/properties");
            components.add(new ComponentDefinition(id, type, typeVersion, properties));
        }
        return List.copyOf(components);
    }

    /** Validates one deliberately exported reusable-definition contract. */
    private EntityContract validateContract(RawDefinitionDocuments.@Nullable Contract raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "contract is required", location);
            return EntityContract.empty();
        }
        List<EntityContract.Parameter> parameters = validateParameters(raw.parameters(), location + "/parameters");
        List<EntityContract.Signal> signals = validateSignals(raw.signals(), location + "/signals");
        List<EntityContract.Action> actions = validateActions(raw.actions(), location + "/actions");
        List<CapabilityId> capabilities = validateCapabilities(raw.capabilities(), location + "/capabilities");
        List<EntityContract.Attachment> attachments = validateAttachments(raw.attachments(), location + "/attachments");
        List<EntityContract.ResourceBinding> bindings =
                validateResourceBindings(raw.resourceBindings(), location + "/resourceBindings");
        try {
            return new EntityContract(parameters, signals, actions, capabilities, attachments, bindings);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.CONTRACT_DUPLICATE, exception.toString(), location);
            return EntityContract.empty();
        }
    }

    /** Validates exported parameters in declaration order. */
    private List<EntityContract.Parameter> validateParameters(
            @Nullable List<RawDefinitionDocuments.@Nullable Parameter> rawParameters, String location) {
        if (rawParameters == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "parameters are required", location);
            return List.of();
        }
        List<EntityContract.Parameter> parameters = new ArrayList<>();
        for (int index = 0; index < rawParameters.size(); index++) {
            RawDefinitionDocuments.Parameter raw = rawParameters.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "parameter is required", itemLocation);
                continue;
            }
            Optional<PropertyId> id = parsePropertyId(raw.id(), itemLocation + "/id");
            Optional<ProjectValueKind> kind = parseValueKind(raw.valueKind(), itemLocation + "/valueKind");
            PropertyTarget target = validatePropertyTarget(raw.target(), itemLocation + "/target");
            if (id.isPresent() && kind.isPresent()) {
                parameters.add(new EntityContract.Parameter(
                        id.orElseThrow(),
                        kind.orElseThrow(),
                        requirement(raw.required(), itemLocation + "/required"),
                        target));
            }
        }
        return List.copyOf(parameters);
    }

    /** Validates exported signals in declaration order. */
    private List<EntityContract.Signal> validateSignals(
            @Nullable List<RawDefinitionDocuments.@Nullable Endpoint> rawSignals, String location) {
        if (rawSignals == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "signals are required", location);
            return List.of();
        }
        List<EntityContract.Signal> signals = new ArrayList<>();
        for (int index = 0; index < rawSignals.size(); index++) {
            RawDefinitionDocuments.Endpoint raw = rawSignals.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "signal is required", itemLocation);
                continue;
            }
            Optional<EndpointId> id = parseEndpointId(raw.id(), itemLocation + "/id");
            Optional<RegisteredType> payload = validatePayload(raw.payload(), itemLocation + "/payload");
            EndpointTarget target = validateEndpointTarget(raw.target(), itemLocation + "/target");
            if (id.isPresent()) {
                EndpointId value = id.orElseThrow();
                signals.add(
                        payload.isPresent()
                                ? new EntityContract.Signal(value, payload.orElseThrow(), target)
                                : new EntityContract.Signal(value, target));
            }
        }
        return List.copyOf(signals);
    }

    /** Validates exported actions in declaration order. */
    private List<EntityContract.Action> validateActions(
            @Nullable List<RawDefinitionDocuments.@Nullable Endpoint> rawActions, String location) {
        if (rawActions == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "actions are required", location);
            return List.of();
        }
        List<EntityContract.Action> actions = new ArrayList<>();
        for (int index = 0; index < rawActions.size(); index++) {
            RawDefinitionDocuments.Endpoint raw = rawActions.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "action is required", itemLocation);
                continue;
            }
            Optional<EndpointId> id = parseEndpointId(raw.id(), itemLocation + "/id");
            Optional<RegisteredType> payload = validatePayload(raw.payload(), itemLocation + "/payload");
            EndpointTarget target = validateEndpointTarget(raw.target(), itemLocation + "/target");
            if (id.isPresent()) {
                EndpointId value = id.orElseThrow();
                actions.add(
                        payload.isPresent()
                                ? new EntityContract.Action(value, payload.orElseThrow(), target)
                                : new EntityContract.Action(value, target));
            }
        }
        return List.copyOf(actions);
    }

    /** Validates declared capabilities in declaration order. */
    private List<CapabilityId> validateCapabilities(@Nullable List<@Nullable String> rawValues, String location) {
        if (rawValues == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "capabilities are required", location);
            return List.of();
        }
        List<CapabilityId> capabilities = new ArrayList<>();
        for (int index = 0; index < rawValues.size(); index++) {
            String raw = rawValues.get(index);
            if (raw == null || !ProjectIdentifiers.isRegisteredTypeId(raw)) {
                diagnostics.error(
                        AssetDiagnosticCode.CAPABILITY_ID_INVALID,
                        "capability must be an extension-qualified identifier: " + raw,
                        location + "/" + index);
            } else {
                capabilities.add(new CapabilityId(raw));
            }
        }
        return List.copyOf(capabilities);
    }

    /** Validates exported attachments in declaration order. */
    private List<EntityContract.Attachment> validateAttachments(
            @Nullable List<RawDefinitionDocuments.@Nullable Attachment> rawAttachments, String location) {
        if (rawAttachments == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "attachments are required", location);
            return List.of();
        }
        List<EntityContract.Attachment> attachments = new ArrayList<>();
        for (int index = 0; index < rawAttachments.size(); index++) {
            RawDefinitionDocuments.Attachment raw = rawAttachments.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "attachment is required", itemLocation);
                continue;
            }
            Optional<AttachmentPointId> id = parseAttachmentId(raw.id(), itemLocation + "/id");
            SpatialTarget target = validateSpatialTarget(raw.target(), itemLocation + "/target");
            id.ifPresent(value -> attachments.add(new EntityContract.Attachment(value, target)));
        }
        return List.copyOf(attachments);
    }

    /** Validates exported resource bindings in declaration order. */
    private List<EntityContract.ResourceBinding> validateResourceBindings(
            @Nullable List<RawDefinitionDocuments.@Nullable ResourceBinding> rawBindings, String location) {
        if (rawBindings == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "resourceBindings are required", location);
            return List.of();
        }
        List<EntityContract.ResourceBinding> bindings = new ArrayList<>();
        for (int index = 0; index < rawBindings.size(); index++) {
            RawDefinitionDocuments.ResourceBinding raw = rawBindings.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "resource binding is required", itemLocation);
                continue;
            }
            Optional<PropertyId> id = parsePropertyId(raw.id(), itemLocation + "/id");
            Set<ResourceReference.Kind> accepted =
                    validateReferenceKinds(raw.acceptedKinds(), itemLocation + "/acceptedKinds");
            PropertyTarget target = validatePropertyTarget(raw.target(), itemLocation + "/target");
            id.ifPresent(value -> bindings.add(new EntityContract.ResourceBinding(
                    value, requirement(raw.required(), itemLocation + "/required"), accepted, target)));
        }
        return List.copyOf(bindings);
    }

    /** Validates internal signal/action connections in declaration order. */
    private List<SignalConnection> validateConnections(
            @Nullable List<RawDefinitionDocuments.@Nullable Connection> rawConnections, String location) {
        if (rawConnections == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "connections are required", location);
            return List.of();
        }
        List<SignalConnection> connections = new ArrayList<>();
        for (int index = 0; index < rawConnections.size(); index++) {
            RawDefinitionDocuments.Connection raw = rawConnections.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "connection is required", itemLocation);
                continue;
            }
            EndpointTarget signal = validateEndpointTarget(raw.signal(), itemLocation + "/signal");
            EndpointTarget action = validateEndpointTarget(raw.action(), itemLocation + "/action");
            connections.add(new SignalConnection(signal, action));
        }
        return List.copyOf(connections);
    }

    /** Validates one stable property target. */
    private PropertyTarget validatePropertyTarget(
            RawDefinitionDocuments.@Nullable PropertyTarget raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "property target is required", location);
            return PropertyTarget.placement(placeholderReferenceEntityId(), new PropertyId("invalid"));
        }
        EntityId entity = parseEntityReferenceId(raw.entityId(), location + "/entityId");
        Optional<ComponentId> component = parseOptionalComponentId(raw.componentId(), location + "/componentId");
        PropertyId property =
                parsePropertyId(raw.propertyId(), location + "/propertyId").orElseGet(() -> new PropertyId("invalid"));
        return component
                .map(value -> PropertyTarget.component(entity, value, property))
                .orElseGet(() -> PropertyTarget.placement(entity, property));
    }

    /** Validates one stable endpoint target. */
    private EndpointTarget validateEndpointTarget(
            RawDefinitionDocuments.@Nullable EndpointTarget raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "endpoint target is required", location);
            return EndpointTarget.placement(placeholderReferenceEntityId(), new EndpointId("invalid"));
        }
        EntityId entity = parseEntityReferenceId(raw.entityId(), location + "/entityId");
        Optional<ComponentId> component = parseOptionalComponentId(raw.componentId(), location + "/componentId");
        EndpointId endpoint =
                parseEndpointId(raw.endpointId(), location + "/endpointId").orElseGet(() -> new EndpointId("invalid"));
        return component
                .map(value -> EndpointTarget.component(entity, value, endpoint))
                .orElseGet(() -> EndpointTarget.placement(entity, endpoint));
    }

    /** Validates one stable spatial target. */
    private SpatialTarget validateSpatialTarget(RawDefinitionDocuments.@Nullable SpatialTarget raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "spatial target is required", location);
            return SpatialTarget.entity(placeholderReferenceEntityId());
        }
        EntityId entity = parseEntityReferenceId(raw.entityId(), location + "/entityId");
        Optional<ComponentId> component = parseOptionalComponentId(raw.componentId(), location + "/componentId");
        Optional<AttachmentPointId> attachment = raw.attachmentId() == null
                ? Optional.empty()
                : parseAttachmentId(raw.attachmentId(), location + "/attachmentId");
        if (component.isPresent() && attachment.isPresent()) {
            return SpatialTarget.componentAttachment(entity, component.orElseThrow(), attachment.orElseThrow());
        }
        if (component.isPresent()) {
            return SpatialTarget.component(entity, component.orElseThrow());
        }
        return attachment
                .map(value -> SpatialTarget.placementAttachment(entity, value))
                .orElseGet(() -> SpatialTarget.entity(entity));
    }

    /** Validates one optional registered payload type. */
    private Optional<RegisteredType> validatePayload(
            RawDefinitionDocuments.@Nullable RegisteredType raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        boolean valid = true;
        if (raw.id() == null || !ProjectIdentifiers.isRegisteredTypeId(raw.id())) {
            diagnostics.error(
                    AssetDiagnosticCode.COMPONENT_TYPE_INVALID,
                    "payload id must be an extension-qualified registered type: " + raw.id(),
                    location + "/id");
            valid = false;
        }
        if (raw.version() == null || raw.version() < 1) {
            diagnostics.error(
                    AssetDiagnosticCode.COMPONENT_VERSION_INVALID,
                    "payload version must be positive: " + raw.version(),
                    location + "/version");
            valid = false;
        }
        if (!valid) {
            return Optional.empty();
        }
        String id = Objects.requireNonNull(raw.id(), "validated payload id");
        int version = Objects.requireNonNull(raw.version(), "validated payload version");
        return Optional.of(new RegisteredType(id, version));
    }

    /** Validates accepted resource-reference namespaces. */
    private Set<ResourceReference.Kind> validateReferenceKinds(
            @Nullable List<@Nullable String> rawKinds, String location) {
        if (rawKinds == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "acceptedKinds are required", location);
            return Set.of();
        }
        Set<ResourceReference.Kind> result = new HashSet<>();
        for (int index = 0; index < rawKinds.size(); index++) {
            String raw = rawKinds.get(index);
            try {
                ResourceReference.Kind kind = ResourceReference.Kind.valueOf(
                        Objects.requireNonNull(raw, "kind").toUpperCase(Locale.ROOT));
                if (!result.add(kind)) {
                    diagnostics.error(
                            AssetDiagnosticCode.CONTRACT_DUPLICATE,
                            "accepted resource-reference kind is duplicated: " + raw,
                            location + "/" + index);
                }
            } catch (IllegalArgumentException | NullPointerException exception) {
                diagnostics.error(
                        AssetDiagnosticCode.VALUE_REFERENCE_INVALID,
                        "unsupported resource-reference kind: " + raw,
                        location + "/" + index);
            }
        }
        return Set.copyOf(result);
    }

    /** Validates one stable entity-definition reference. */
    private AssetRef<EntityDefinition> validateReference(
            RawDefinitionDocuments.@Nullable Reference raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "definition reference is required", location);
            return AssetRef.to(INVALID_ASSET_ID);
        }
        AssetId id = parseAssetId(raw.assetId(), location + "/assetId");
        if (raw.pathHint() == null) {
            return AssetRef.to(id);
        }
        if (!ProjectIdentifiers.isPortableLocator(raw.pathHint())) {
            diagnostics.error(
                    AssetDiagnosticCode.REFERENCE_HINT_INVALID,
                    "pathHint must be a portable relative locator: " + raw.pathHint(),
                    location + "/pathHint");
            return AssetRef.to(id);
        }
        return AssetRef.to(id, raw.pathHint());
    }

    /** Converts an optional JSON object into portable project values. */
    private Map<String, ProjectValue> validateValueMap(@Nullable JsonNode raw, String location) {
        if (raw == null) {
            return Map.of();
        }
        if (!raw.isObject()) {
            diagnostics.error(AssetDiagnosticCode.VALUE_NOT_OBJECT, "value must be an object", location);
            return Map.of();
        }
        return new LinkedHashMap<>(values.decodeObject(raw, location).values());
    }

    /** Converts a JSON object into values keyed by stable property identity. */
    private Map<PropertyId, ProjectValue> validatePropertyValues(@Nullable JsonNode raw, String location) {
        Map<String, ProjectValue> decoded = validateValueMap(raw, location);
        Map<PropertyId, ProjectValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, ProjectValue> entry : decoded.entrySet()) {
            Optional<PropertyId> id = parsePropertyId(entry.getKey(), location + "/" + entry.getKey());
            id.ifPresent(value -> result.put(value, entry.getValue()));
        }
        return result;
    }

    /** Decodes one reserved project-value reference without exposing Jackson types publicly. */
    private ProjectValue.ReferenceValue decodeReferenceValue(JsonNode raw, String location) {
        JsonNode reference = raw.get("$ref");
        if (raw.size() != 1 || reference == null || !reference.isTextual()) {
            diagnostics.error(
                    AssetDiagnosticCode.VALUE_REFERENCE_INVALID,
                    "reference object must contain only one textual $ref field",
                    location);
            return invalidReferenceValue();
        }
        String text = reference.textValue();
        try {
            if (text.startsWith(ResourceReference.Kind.PROJECT.prefix())) {
                String locator =
                        text.substring(ResourceReference.Kind.PROJECT.prefix().length());
                Path resolved = projectRoot.resolve(locator).normalize();
                if (!resolved.startsWith(projectRoot)) {
                    throw new IllegalArgumentException("project reference escapes the project root: " + text);
                }
                return new ProjectValue.ReferenceValue(ResourceReference.project(locator, resolved));
            }
            if (text.startsWith(ResourceReference.Kind.ASSET.prefix())) {
                return new ProjectValue.ReferenceValue(ResourceReference.asset(
                        text.substring(ResourceReference.Kind.ASSET.prefix().length())));
            }
            if (text.startsWith(ResourceReference.Kind.IMPORT.prefix())) {
                return new ProjectValue.ReferenceValue(ResourceReference.imported(
                        text.substring(ResourceReference.Kind.IMPORT.prefix().length())));
            }
            throw new IllegalArgumentException("unsupported reference scheme: " + text);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.VALUE_REFERENCE_INVALID, exception.toString(), location);
            return invalidReferenceValue();
        }
    }

    /** Creates a constructible placeholder after a reference diagnostic. */
    private static ProjectValue.ReferenceValue invalidReferenceValue() {
        return new ProjectValue.ReferenceValue(ResourceReference.asset("invalid"));
    }

    /** Requires one non-blank top-level display name. */
    private String requiredName(@Nullable String raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "name is required", location);
            return "Invalid";
        }
        if (raw.isBlank()) {
            diagnostics.error(AssetDiagnosticCode.FIELD_BLANK, "name must not be blank", location);
            return "Invalid";
        }
        return raw;
    }

    /** Validates one optional non-blank display name. */
    private Optional<String> optionalName(@Nullable String raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw.isBlank()) {
            diagnostics.error(AssetDiagnosticCode.FIELD_BLANK, "name must not be blank", location);
            return Optional.empty();
        }
        return Optional.of(raw);
    }

    /** Decodes whether a public argument is required while retaining a safe validation fallback. */
    private EntityContract.Requirement requirement(@Nullable Boolean raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "boolean value is required", location);
            return EntityContract.Requirement.OPTIONAL;
        }
        return raw ? EntityContract.Requirement.REQUIRED : EntityContract.Requirement.OPTIONAL;
    }

    /** Parses a canonical asset UUID while retaining a constructible placeholder. */
    private AssetId parseAssetId(@Nullable String raw, String location) {
        if (raw == null) {
            diagnostics.error(AssetDiagnosticCode.FIELD_REQUIRED, "assetId is required", location);
            return INVALID_ASSET_ID;
        }
        try {
            return AssetId.from(raw);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ID_INVALID, exception.toString(), location);
            return INVALID_ASSET_ID;
        }
    }

    /** Parses and deduplicates a canonical entity UUID within the containing asset. */
    private EntityId parseEntityId(@Nullable String raw, String location) {
        EntityId id;
        try {
            if (raw == null) {
                throw new IllegalArgumentException("entityId is required");
            }
            id = EntityId.from(raw);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ID_INVALID, exception.toString(), location);
            return placeholderEntityId();
        }
        if (!entityIds.add(id)) {
            diagnostics.error(
                    AssetDiagnosticCode.ENTITY_ID_DUPLICATE,
                    "entityId is duplicated within this asset: " + id,
                    location);
            return placeholderEntityId();
        }
        return id;
    }

    /** Parses one canonical component UUID. */
    private ComponentId parseComponentId(@Nullable String raw, String location) {
        try {
            if (raw == null) {
                throw new IllegalArgumentException("componentId is required");
            }
            return ComponentId.from(raw);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ID_INVALID, exception.toString(), location);
            return placeholderComponentId();
        }
    }

    /** Parses an entity identity used as a reference without treating it as a declaration. */
    private EntityId parseEntityReferenceId(@Nullable String raw, String location) {
        try {
            if (raw == null) {
                throw new IllegalArgumentException("entityId is required");
            }
            return EntityId.from(raw);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ID_INVALID, exception.toString(), location);
            return placeholderReferenceEntityId();
        }
    }

    /** Parses an optional referenced component identity. */
    private Optional<ComponentId> parseOptionalComponentId(@Nullable String raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ComponentId.from(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ID_INVALID, exception.toString(), location);
            return Optional.empty();
        }
    }

    /** Parses one stable local property identity. */
    private Optional<PropertyId> parsePropertyId(@Nullable String raw, String location) {
        try {
            if (raw == null) {
                throw new IllegalArgumentException("property id is required");
            }
            return Optional.of(new PropertyId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.PROPERTY_ID_INVALID, exception.toString(), location);
            return Optional.empty();
        }
    }

    /** Parses one stable local endpoint identity. */
    private Optional<EndpointId> parseEndpointId(@Nullable String raw, String location) {
        try {
            if (raw == null) {
                throw new IllegalArgumentException("endpoint id is required");
            }
            return Optional.of(new EndpointId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ATTACHMENT_ID_INVALID, exception.toString(), location);
            return Optional.empty();
        }
    }

    /** Parses one stable local attachment-point identity. */
    private Optional<AttachmentPointId> parseAttachmentId(@Nullable String raw, String location) {
        try {
            if (raw == null) {
                throw new IllegalArgumentException("attachment id is required");
            }
            return Optional.of(new AttachmentPointId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(AssetDiagnosticCode.ENDPOINT_ID_INVALID, exception.toString(), location);
            return Optional.empty();
        }
    }

    /** Parses one serialized project-value kind. */
    private Optional<ProjectValueKind> parseValueKind(@Nullable String raw, String location) {
        try {
            return Optional.of(ProjectValueKind.valueOf(
                    Objects.requireNonNull(raw, "valueKind").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            diagnostics.error(
                    AssetDiagnosticCode.CONTRACT_ARGUMENT_TYPE, "unsupported project value kind: " + raw, location);
            return Optional.empty();
        }
    }

    /** Validates one extension-qualified component type. */
    private ComponentTypeId validateComponentType(@Nullable String raw, String location) {
        if (raw == null || !ProjectIdentifiers.isRegisteredTypeId(raw)) {
            diagnostics.error(
                    AssetDiagnosticCode.COMPONENT_TYPE_INVALID,
                    "type must be an extension-qualified registered type: " + raw,
                    location);
            return new ComponentTypeId("invalid.extension/invalid");
        }
        return new ComponentTypeId(raw);
    }

    /** Validates one positive component configuration-schema version. */
    private int validateComponentVersion(@Nullable Integer raw, String location) {
        if (raw == null || raw < 1) {
            diagnostics.error(
                    AssetDiagnosticCode.COMPONENT_VERSION_INVALID, "typeVersion must be positive: " + raw, location);
            return 1;
        }
        return raw;
    }

    /** Creates a unique placeholder entity identity after reporting invalid source data. */
    private EntityId placeholderEntityId() {
        EntityId id = new EntityId(new UUID(0, placeholderSequence++));
        entityIds.add(id);
        return id;
    }

    /** Creates a unique placeholder component identity after reporting invalid source data. */
    private ComponentId placeholderComponentId() {
        return new ComponentId(new UUID(0, placeholderSequence++));
    }

    /** Creates a placeholder for an invalid entity reference without declaring it in the hierarchy. */
    private EntityId placeholderReferenceEntityId() {
        return new EntityId(new UUID(0, placeholderSequence++));
    }

    /** Creates a safe placeholder local entity for missing or invalid roots. */
    private LocalEntity placeholderLocalEntity() {
        return new LocalEntity(placeholderEntityId(), true, List.of(), List.of());
    }

    /** Creates a successful immutable read result. */
    private <T> ReadResult<T> success(T value) {
        return new ReadResult<>(Optional.of(value), diagnostics.diagnostics());
    }

    /** Creates a failed immutable read result. */
    private <T> ReadResult<T> failure() {
        return new ReadResult<>(Optional.empty(), diagnostics.diagnostics());
    }

    /** Complete result of reading one definition document.
     *
     * @param value validated definition, when no errors were produced
     * @param diagnostics ordered errors and warnings
     * @param <T> definition kind
     */
    public record ReadResult<T>(Optional<T> value, List<ProjectDiagnostic> diagnostics) {
        /** Copies result values. */
        public ReadResult {
            Objects.requireNonNull(value, "value");
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
