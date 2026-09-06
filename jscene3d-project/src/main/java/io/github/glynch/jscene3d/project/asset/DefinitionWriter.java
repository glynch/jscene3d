/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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

    /** Serializes one entity definition into a complete in-memory document. */
    private static byte[] serializeEntity(EntityDefinition definition) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator json = JSON_FACTORY.createGenerator(output).useDefaultPrettyPrinter()) {
            json.writeStartObject();
            writeEnvelope(json, ENTITY_SCHEMA, definition.id(), AssetKind.ENTITY_DEFINITION);
            json.writeStringField("name", definition.name());
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
                writeValues(json, placement.arguments());
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
        writeValues(json, component.properties());
        json.writeEndObject();
        json.writeEndObject();
    }

    /** Writes a declaration-ordered map of portable values. */
    private static void writeValues(JsonGenerator json, Map<String, ProjectValue> values) throws IOException {
        for (Map.Entry<String, ProjectValue> entry : values.entrySet()) {
            json.writeFieldName(entry.getKey());
            writeValue(json, entry.getValue());
        }
    }

    /** Writes one value from the closed portable-value family. */
    private static void writeValue(JsonGenerator json, ProjectValue value) throws IOException {
        switch (value) {
            case ProjectValue.NullValue ignored -> json.writeNull();
            case ProjectValue.BooleanValue booleanValue -> json.writeBoolean(booleanValue.value());
            case ProjectValue.NumberValue numberValue -> json.writeNumber(numberValue.value());
            case ProjectValue.TextValue textValue -> json.writeString(textValue.value());
            case ProjectValue.ArrayValue arrayValue -> {
                json.writeStartArray();
                for (ProjectValue element : arrayValue.values()) {
                    writeValue(json, element);
                }
                json.writeEndArray();
            }
            case ProjectValue.ObjectValue objectValue -> {
                json.writeStartObject();
                writeValues(json, objectValue.values());
                json.writeEndObject();
            }
            case ProjectValue.ReferenceValue referenceValue -> {
                json.writeStartObject();
                json.writeStringField("$ref", referenceValue.reference().toString());
                json.writeEndObject();
            }
        }
    }

    /** Creates parent directories as needed and replaces one complete target file. */
    private static void writeBytes(Path target, byte[] content) throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(absoluteTarget, content);
    }

    /** Ensures every canonical document ends with exactly one LF. */
    private static byte[] appendNewline(byte[] content) {
        byte[] terminated = Arrays.copyOf(content, content.length + 1);
        terminated[content.length] = '\n';
        return terminated;
    }
}
