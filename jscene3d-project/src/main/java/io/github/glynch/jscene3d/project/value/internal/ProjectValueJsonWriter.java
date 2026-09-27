/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.value.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Canonical JSON serialization shared by project documents containing portable values. */
public final class ProjectValueJsonWriter {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /** Prevents construction of this stateless writer. */
    private ProjectValueJsonWriter() {
        throw new AssertionError("ProjectValueJsonWriter cannot be instantiated");
    }

    /**
     * Writes one declaration-ordered map without opening or closing its containing object.
     *
     * @param <K> key type whose string form becomes the JSON field name
     * @param json destination generator
     * @param values values to write in iteration order
     * @throws IOException if the destination cannot be written
     */
    public static <K> void writeValues(JsonGenerator json, Map<K, ProjectValue> values) throws IOException {
        for (Map.Entry<K, ProjectValue> entry : values.entrySet()) {
            json.writeFieldName(entry.getKey().toString());
            writeValue(json, entry.getValue());
        }
    }

    /**
     * Writes one value from the closed portable-value family.
     *
     * @param json destination generator
     * @param value value to write
     * @throws IOException if the destination cannot be written
     */
    public static void writeValue(JsonGenerator json, ProjectValue value) throws IOException {
        switch (value) {
            case ProjectValue.NullValue ignored -> json.writeNull();
            case ProjectValue.BooleanValue booleanValue -> json.writeBoolean(booleanValue.value());
            case ProjectValue.NumberValue numberValue -> json.writeNumber(numberValue.value());
            case ProjectValue.TextValue textValue -> json.writeString(textValue.value());
            case ProjectValue.ArrayValue arrayValue -> writeArray(json, arrayValue);
            case ProjectValue.ObjectValue objectValue -> writeObject(json, objectValue);
            case ProjectValue.ReferenceValue referenceValue -> writeReference(json, referenceValue);
            case ProjectValue.EntityTargetValue targetValue -> writeEntityTarget(json, targetValue);
            case ProjectValue.ComponentTargetValue targetValue -> writeComponentTarget(json, targetValue);
        }
    }

    /**
     * Encodes one value into the same authored JSON shape used by project document writers.
     *
     * @param value structural project value
     * @return detached JSON tree value
     */
    public static JsonNode toJsonNode(ProjectValue value) {
        Objects.requireNonNull(value, "value");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (JsonGenerator json = JSON_FACTORY.createGenerator(output)) {
                writeValue(json, value);
            }
            JsonNode encoded = ProjectJsonReader.strict().readTree(new ByteArrayInputStream(output.toByteArray()));
            return Objects.requireNonNull(encoded, "encoded value");
        } catch (IOException exception) {
            throw new AssertionError("In-memory ProjectValue encoding must succeed", exception);
        }
    }

    /** Writes one portable array. */
    private static void writeArray(JsonGenerator json, ProjectValue.ArrayValue value) throws IOException {
        json.writeStartArray();
        for (ProjectValue element : value.values()) {
            writeValue(json, element);
        }
        json.writeEndArray();
    }

    /** Writes one portable object. */
    private static void writeObject(JsonGenerator json, ProjectValue.ObjectValue value) throws IOException {
        json.writeStartObject();
        writeValues(json, value.values());
        json.writeEndObject();
    }

    /** Writes one resource reference using its reserved discriminator. */
    private static void writeReference(JsonGenerator json, ProjectValue.ReferenceValue value) throws IOException {
        json.writeStartObject();
        json.writeStringField("$ref", value.reference().toString());
        json.writeEndObject();
    }

    /** Writes one entity target using its reserved discriminator. */
    private static void writeEntityTarget(JsonGenerator json, ProjectValue.EntityTargetValue value) throws IOException {
        json.writeStartObject();
        json.writeObjectFieldStart("$target");
        json.writeStringField("entityId", value.entity().toString());
        json.writeEndObject();
        json.writeEndObject();
    }

    /** Writes one component target using its reserved discriminator. */
    private static void writeComponentTarget(JsonGenerator json, ProjectValue.ComponentTargetValue value)
            throws IOException {
        ComponentTarget target = value.target();
        json.writeStartObject();
        json.writeObjectFieldStart("$target");
        json.writeStringField("entityId", target.entity().toString());
        json.writeStringField("componentId", target.component().toString());
        json.writeEndObject();
        json.writeEndObject();
    }
}
