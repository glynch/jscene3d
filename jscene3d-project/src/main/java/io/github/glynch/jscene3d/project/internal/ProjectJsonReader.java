/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Strict JSON deserialization shared by versioned project formats. */
public final class ProjectJsonReader {
    private static final ProjectJsonReader STRICT = new ProjectJsonReader(createObjectMapper());

    private final ObjectMapper objectMapper;

    /** Stores one fully configured reader service. */
    private ProjectJsonReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the shared strict project-format reader.
     *
     * @return strict reader service
     */
    public static ProjectJsonReader strict() {
        return STRICT;
    }

    /**
     * Reads one JSON document with duplicate, unknown, null-primitive, and trailing-token checks.
     *
     * @param input JSON input stream
     * @param type deserialization target type
     * @param <T> deserialization target type
     * @return deserialized value
     * @throws IOException when the document cannot be read or parsed
     */
    public <T> T read(InputStream input, Class<T> type) throws IOException {
        return objectMapper.readValue(Objects.requireNonNull(input, "input"), Objects.requireNonNull(type, "type"));
    }

    /** Creates the shared strictly configured mapper. */
    private static ObjectMapper createObjectMapper() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        return new ObjectMapper(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
}
