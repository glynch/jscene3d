/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.internal.AtomicProjectFileWriter;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueJsonWriter;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Deterministic UTF-8 JSON writer for authored entity and world definitions. */
public final class DefinitionWriter {
    private static final String ENTITY_SCHEMA = "https://jscene3d.org/schemas/entity-definition-1.json";
    private static final String WORLD_SCHEMA = "https://jscene3d.org/schemas/world-definition-1.json";
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /** Prevents construction of this stateless writer. */
    private DefinitionWriter() {
        throw new AssertionError("DefinitionWriter cannot be instantiated");
    }

    /**
     * Writes one reusable entity definition, replacing the target only after serialization succeeds.
     *
     * @param target output file
     * @param definition authored entity definition
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(Path target, EntityDefinition definition) throws IOException {
        Path validTarget = Objects.requireNonNull(target, "target");
        EntityDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        writeBytes(validTarget, serializeEntity(validDefinition));
    }

    /**
     * Writes one reusable entity definition to caller-owned output.
     *
     * <p>This method consumes but does not close {@code output}. It is intended for generated definitions whose
     * physical storage is owned by another subsystem, such as the project import cache.
     *
     * @param output destination owned by the caller
     * @param definition generated entity definition
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(OutputStream output, EntityDefinition definition) throws IOException {
        OutputStream validOutput = Objects.requireNonNull(output, "output");
        EntityDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        validOutput.write(serializeEntity(validDefinition));
    }

    /**
     * Writes one world definition, replacing the target only after serialization succeeds.
     *
     * @param target output file
     * @param definition authored world definition
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(Path target, WorldDefinition definition) throws IOException {
        Path validTarget = Objects.requireNonNull(target, "target");
        WorldDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        writeBytes(validTarget, serializeWorld(validDefinition));
    }

    /**
     * Writes one world definition to caller-owned output.
     *
     * <p>This method consumes but does not close {@code output}.
     *
     * @param output destination owned by the caller
     * @param definition generated world definition
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(OutputStream output, WorldDefinition definition) throws IOException {
        OutputStream validOutput = Objects.requireNonNull(output, "output");
        WorldDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        validOutput.write(serializeWorld(validDefinition));
    }

    /** Serializes one entity definition into a complete in-memory document. */
    private static byte[] serializeEntity(EntityDefinition definition) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator json = JSON_FACTORY.createGenerator(output).useDefaultPrettyPrinter()) {
            json.writeStartObject();
            writeEnvelope(json, ENTITY_SCHEMA, definition.id(), AssetKind.ENTITY_DEFINITION);
            json.writeStringField("name", definition.name());
            writeContract(json, definition.contract());
            writeConnections(json, definition.connections());
            json.writeFieldName("root");
            writeEntry(json, definition.root());
            json.writeEndObject();
        }
        return appendNewline(output.toByteArray());
    }

    /** Serializes one world definition into a complete in-memory document. */
    private static byte[] serializeWorld(WorldDefinition definition) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator json = JSON_FACTORY.createGenerator(output).useDefaultPrettyPrinter()) {
            json.writeStartObject();
            writeEnvelope(json, WORLD_SCHEMA, definition.id(), AssetKind.WORLD_DEFINITION);
            json.writeStringField("name", definition.name());
            writeConnections(json, definition.connections());
            json.writeArrayFieldStart("roots");
            for (EntityEntry root : definition.roots()) {
                writeEntry(json, root);
            }
            json.writeEndArray();
            json.writeEndObject();
        }
        return appendNewline(output.toByteArray());
    }

    /** Writes the shared version-one asset envelope in canonical field order. */
    private static void writeEnvelope(JsonGenerator json, String schema, AssetId id, AssetKind kind)
            throws IOException {
        json.writeStringField("$schema", schema);
        json.writeStringField("assetId", id.toString());
        json.writeStringField("assetType", kind.serializedName());
        json.writeNumberField("formatVersion", AssetCatalog.FORMAT_VERSION);
    }

    /** Writes one complete exported entity-definition contract. */
    private static void writeContract(JsonGenerator json, EntityContract contract) throws IOException {
        json.writeObjectFieldStart("contract");
        json.writeArrayFieldStart("parameters");
        for (EntityContract.Parameter parameter : contract.parameters().values()) {
            json.writeStartObject();
            json.writeStringField("id", parameter.id().toString());
            json.writeStringField("valueKind", parameter.valueKind().name().toLowerCase(Locale.ROOT));
            json.writeBooleanField("required", parameter.isRequired());
            json.writeFieldName("target");
            writePropertyTarget(json, parameter.target());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeArrayFieldStart("signals");
        for (EntityContract.Signal signal : contract.signals().values()) {
            writeContractEndpoint(json, signal.id().toString(), signal.payload(), signal.target());
        }
        json.writeEndArray();
        json.writeArrayFieldStart("actions");
        for (EntityContract.Action action : contract.actions().values()) {
            writeContractEndpoint(json, action.id().toString(), action.payload(), action.target());
        }
        json.writeEndArray();
        json.writeArrayFieldStart("capabilities");
        for (CapabilityId capability : contract.capabilities()) {
            json.writeString(capability.toString());
        }
        json.writeEndArray();
        json.writeArrayFieldStart("attachments");
        for (EntityContract.Attachment attachment : contract.attachments().values()) {
            json.writeStartObject();
            json.writeStringField("id", attachment.id().toString());
            json.writeFieldName("target");
            writeSpatialTarget(json, attachment.target());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeArrayFieldStart("resourceBindings");
        for (EntityContract.ResourceBinding binding :
                contract.resourceBindings().values()) {
            json.writeStartObject();
            json.writeStringField("id", binding.id().toString());
            json.writeBooleanField("required", binding.isRequired());
            json.writeArrayFieldStart("acceptedKinds");
            for (var kind : binding.acceptedKinds().stream().sorted().toList()) {
                json.writeString(kind.name().toLowerCase(Locale.ROOT));
            }
            json.writeEndArray();
            json.writeFieldName("target");
            writePropertyTarget(json, binding.target());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeEndObject();
    }

    /** Writes one exported signal or action. */
    private static void writeContractEndpoint(
            JsonGenerator json, String id, Optional<RegisteredType> payload, EndpointTarget target) throws IOException {
        json.writeStartObject();
        json.writeStringField("id", id);
        if (payload.isPresent()) {
            RegisteredType type = payload.orElseThrow();
            json.writeObjectFieldStart("payload");
            json.writeStringField("id", type.id());
            json.writeNumberField("version", type.version());
            json.writeEndObject();
        }
        json.writeFieldName("target");
        writeEndpointTarget(json, target);
        json.writeEndObject();
    }

    /** Writes internal signal/action connections in declaration order. */
    private static void writeConnections(JsonGenerator json, List<SignalConnection> connections) throws IOException {
        json.writeArrayFieldStart("connections");
        for (SignalConnection connection : connections) {
            json.writeStartObject();
            json.writeFieldName("signal");
            writeEndpointTarget(json, connection.signal());
            json.writeFieldName("action");
            writeEndpointTarget(json, connection.action());
            json.writeEndObject();
        }
        json.writeEndArray();
    }

    /** Writes one stable property target. */
    private static void writePropertyTarget(JsonGenerator json, PropertyTarget target) throws IOException {
        json.writeStartObject();
        json.writeStringField("entityId", target.entity().toString());
        if (target.component().isPresent()) {
            json.writeStringField(
                    "componentId", target.component().orElseThrow().toString());
        }
        json.writeStringField("propertyId", target.property().toString());
        json.writeEndObject();
    }

    /** Writes one stable signal or action target. */
    private static void writeEndpointTarget(JsonGenerator json, EndpointTarget target) throws IOException {
        json.writeStartObject();
        json.writeStringField("entityId", target.entity().toString());
        if (target.component().isPresent()) {
            json.writeStringField(
                    "componentId", target.component().orElseThrow().toString());
        }
        json.writeStringField("endpointId", target.endpoint().toString());
        json.writeEndObject();
    }

    /** Writes one stable spatial target. */
    private static void writeSpatialTarget(JsonGenerator json, SpatialTarget target) throws IOException {
        json.writeStartObject();
        json.writeStringField("entityId", target.entity().toString());
        if (target.component().isPresent()) {
            json.writeStringField(
                    "componentId", target.component().orElseThrow().toString());
        }
        if (target.attachment().isPresent()) {
            json.writeStringField(
                    "attachmentId", target.attachment().orElseThrow().toString());
        }
        json.writeEndObject();
    }

    /** Writes one discriminated entity entry. */
    private static void writeEntry(JsonGenerator json, EntityEntry entry) throws IOException {
        json.writeStartObject();
        switch (entry) {
            case LocalEntity local -> {
                writeCommonEntry(json, "local", local);
                json.writeArrayFieldStart("components");
                for (ComponentDefinition component : local.components()) {
                    writeComponent(json, component);
                }
                json.writeEndArray();
                json.writeArrayFieldStart("children");
                for (EntityEntry child : local.children()) {
                    writeEntry(json, child);
                }
                json.writeEndArray();
            }
            case EntityPlacement placement -> {
                writeCommonEntry(json, "placement", placement);
                json.writeObjectFieldStart("definition");
                json.writeStringField("assetId", placement.definition().id().toString());
                if (placement.definition().pathHint().isPresent()) {
                    json.writeStringField(
                            "pathHint", placement.definition().pathHint().orElseThrow());
                }
                json.writeEndObject();
                json.writeObjectFieldStart("arguments");
                ProjectValueJsonWriter.writeValues(json, placement.arguments());
                json.writeEndObject();
            }
        }
        json.writeEndObject();
    }

    /** Writes fields shared by local entities and placements. */
    private static void writeCommonEntry(JsonGenerator json, String entryType, EntityEntry entry) throws IOException {
        json.writeStringField("entryType", entryType);
        json.writeStringField("entityId", entry.id().toString());
        if (entry.name().isPresent()) {
            json.writeStringField("name", entry.name().orElseThrow());
        }
        json.writeBooleanField("enabled", entry.isEnabled());
    }

    /** Writes one authored component. */
    private static void writeComponent(JsonGenerator json, ComponentDefinition component) throws IOException {
        json.writeStartObject();
        json.writeStringField("componentId", component.id().toString());
        json.writeStringField("type", component.type().toString());
        json.writeNumberField("typeVersion", component.typeVersion());
        json.writeObjectFieldStart("properties");
        ProjectValueJsonWriter.writeValues(json, component.properties());
        json.writeEndObject();
        json.writeEndObject();
    }

    /** Creates parent directories as needed and atomically replaces one complete target file. */
    private static void writeBytes(Path target, byte[] content) throws IOException {
        AtomicProjectFileWriter.write(target, content);
    }

    /** Ensures every canonical document ends with exactly one LF. */
    private static byte[] appendNewline(byte[] content) {
        byte[] terminated = Arrays.copyOf(content, content.length + 1);
        terminated[content.length] = '\n';
        return terminated;
    }
}
