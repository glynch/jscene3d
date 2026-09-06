/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueJsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Deterministic UTF-8 JSON writer for native project-resource definitions. */
public final class ResourceWriter {
    private static final String SCHEMA = "https://jscene3d.org/schemas/resource-1.json";
    private static final int SCHEMA_VERSION = 1;
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /** Prevents construction of this stateless writer. */
    private ResourceWriter() {
        throw new AssertionError("ResourceWriter cannot be instantiated");
    }

    /**
     * Writes one resource definition, replacing the target only after serialization succeeds.
     *
     * @param target output file
     * @param type exact registered resource type
     * @param properties declaration-ordered resource properties
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(Path target, RegisteredType type, Map<String, ProjectValue> properties)
            throws IOException {
        Path validTarget =
                Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        byte[] content = serialize(type, properties);
        Path parent = validTarget.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(validTarget, content);
    }

    /**
     * Writes one generated resource definition to caller-owned output.
     *
     * <p>This method consumes but does not close {@code output}.
     *
     * @param output caller-owned destination
     * @param type exact registered resource type
     * @param properties declaration-ordered resource properties
     * @throws IOException when the output cannot be serialized or written
     */
    public static void write(OutputStream output, RegisteredType type, Map<String, ProjectValue> properties)
            throws IOException {
        Objects.requireNonNull(output, "output").write(serialize(type, properties));
    }

    /** Serializes one complete canonical resource document. */
    private static byte[] serialize(RegisteredType type, Map<String, ProjectValue> properties) throws IOException {
        RegisteredType validType = Objects.requireNonNull(type, "type");
        Map<String, ProjectValue> validProperties =
                Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(properties, "properties")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator json = JSON_FACTORY.createGenerator(output).useDefaultPrettyPrinter()) {
            json.writeStartObject();
            json.writeStringField("$schema", SCHEMA);
            json.writeNumberField("schemaVersion", SCHEMA_VERSION);
            json.writeStringField("type", validType.id());
            json.writeNumberField("typeVersion", validType.version());
            json.writeObjectFieldStart("properties");
            ProjectValueJsonWriter.writeValues(json, validProperties);
            json.writeEndObject();
            json.writeEndObject();
        }
        byte[] content = output.toByteArray();
        byte[] terminated = Arrays.copyOf(content, content.length + 1);
        terminated[content.length] = '\n';
        return terminated;
    }
}
