/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model for one extension descriptor.
 *
 * @param schema optional schema URI
 * @param schemaVersion descriptor schema version
 * @param id extension identifier
 * @param version extension semantic version
 * @param engineRequires compatible engine versions
 * @param displayName human-readable extension name
 * @param description optional extension description
 * @param types contributed registered types
 */
public record RawExtensionDescriptor(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable String id,
        @Nullable String version,
        @Nullable String engineRequires,
        @Nullable String displayName,
        @Nullable String description,
        @Nullable List<@Nullable Type> types) {
    /** Nullable raw registered-type descriptor. */
    record Type(
            @Nullable String id,
            @Nullable Integer typeVersion,
            @Nullable String scope,
            @Nullable String displayName,
            @Nullable String description,
            @Nullable List<@Nullable Property> properties,
            @Nullable List<@Nullable Endpoint> signals,
            @Nullable List<@Nullable Endpoint> actions,
            @Nullable List<@Nullable String> requiredCapabilities) {}

    /** Nullable raw property descriptor. */
    record Property(
            @Nullable String id,
            @Nullable String valueKind,
            @Nullable Boolean required,
            @Nullable JsonNode defaultValue,
            @Nullable String displayName,
            @Nullable String description,
            @Nullable JsonNode editor,
            @Nullable List<@Nullable String> acceptedReferences) {}

    /** Nullable raw signal or action descriptor. */
    record Endpoint(
            @Nullable String id,
            @Nullable RegisteredType payload,
            @Nullable String displayName,
            @Nullable String description) {}

    /** Nullable raw registered-type reference. */
    record RegisteredType(@Nullable String type, @Nullable Integer typeVersion) {}
}
