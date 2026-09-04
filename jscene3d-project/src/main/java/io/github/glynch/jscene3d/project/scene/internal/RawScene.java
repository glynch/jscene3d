/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model kept behind the validated public scene definition.
 *
 * @param schema optional schema identifier
 * @param schemaVersion authoritative scene schema version
 * @param id nullable scene identifier
 * @param root nullable root node
 * @param connections nullable scene connections
 */
public record RawScene(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable String id,
        @Nullable Node root,
        @Nullable List<@Nullable Connection> connections) {
    /** Nullable raw scene node. */
    record Node(
            @Nullable String id,
            @Nullable String name,
            @Nullable Boolean enabled,
            @Nullable String type,
            @Nullable Integer typeVersion,
            @Nullable String instance,
            @Nullable JsonNode properties,
            @Nullable JsonNode overrides,
            @Nullable Controller controller,
            @Nullable List<@Nullable Node> children) {}

    /** Nullable raw controller definition. */
    record Controller(
            @Nullable String type,
            @Nullable Integer typeVersion,
            @Nullable JsonNode properties) {}

    /** Nullable raw signal endpoint. */
    record SignalEndpoint(@Nullable String node, @Nullable String signal) {}

    /** Nullable raw action endpoint. */
    record ActionEndpoint(@Nullable String node, @Nullable String action) {}

    /** Nullable raw scene connection. */
    record Connection(
            @Nullable SignalEndpoint from, @Nullable ActionEndpoint to) {}
}
