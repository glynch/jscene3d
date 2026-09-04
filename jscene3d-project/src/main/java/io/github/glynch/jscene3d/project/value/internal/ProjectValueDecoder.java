/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.value.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.internal.JsonPointers;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Converts Jackson values into the renderer-independent project value model. */
public final class ProjectValueDecoder {
    private final Optional<ReferenceDecoder> referenceDecoder;

    /** Stores optional resource-reference decoding behavior. */
    private ProjectValueDecoder(Optional<ReferenceDecoder> referenceDecoder) {
        this.referenceDecoder = referenceDecoder;
    }

    /**
     * Creates a decoder that treats every JSON object as ordinary object data.
     *
     * @return plain project-value decoder
     */
    public static ProjectValueDecoder plain() {
        return new ProjectValueDecoder(Optional.empty());
    }

    /**
     * Creates a decoder that delegates objects containing {@code $ref} to a reference decoder.
     *
     * @param referenceDecoder project-specific resource-reference decoder
     * @return reference-aware project-value decoder
     */
    public static ProjectValueDecoder withReferences(ReferenceDecoder referenceDecoder) {
        return new ProjectValueDecoder(Optional.of(referenceDecoder));
    }

    /**
     * Decodes one arbitrary JSON value.
     *
     * @param raw JSON value
     * @param location JSON Pointer location
     * @return decoded project value
     */
    public ProjectValue decode(JsonNode raw, String location) {
        JsonNode value = Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(location, "location");
        if (value.isNull()) {
            return ProjectValue.NullValue.INSTANCE;
        }
        if (value.isBoolean()) {
            return new ProjectValue.BooleanValue(value.booleanValue());
        }
        if (value.isNumber()) {
            return new ProjectValue.NumberValue(value.decimalValue());
        }
        if (value.isTextual()) {
            return new ProjectValue.TextValue(value.textValue());
        }
        if (value.isArray()) {
            return decodeArray(value, location);
        }
        if (value.has("$ref") && referenceDecoder.isPresent()) {
            return referenceDecoder.orElseThrow().decode(value, location);
        }
        return decodeObject(value, location);
    }

    /**
     * Decodes one JSON object.
     *
     * @param raw JSON object
     * @param location JSON Pointer location
     * @return decoded project object
     */
    public ProjectValue.ObjectValue decodeObject(JsonNode raw, String location) {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        raw.properties()
                .forEach(entry -> values.put(
                        entry.getKey(),
                        decode(entry.getValue(), location + "/" + JsonPointers.escapeSegment(entry.getKey()))));
        return new ProjectValue.ObjectValue(values);
    }

    /** Decodes one JSON array. */
    private ProjectValue.ArrayValue decodeArray(JsonNode raw, String location) {
        List<ProjectValue> values = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            values.add(decode(raw.get(index), location + "/" + index));
        }
        return new ProjectValue.ArrayValue(values);
    }

    /** Decodes a reserved resource-reference object. */
    @FunctionalInterface
    public interface ReferenceDecoder {
        /**
         * Decodes one object containing a {@code $ref} property.
         *
         * @param raw reference object
         * @param location JSON Pointer location
         * @return decoded reference value
         */
        ProjectValue.ReferenceValue decode(JsonNode raw, String location);
    }
}
