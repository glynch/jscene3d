/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model for one native resource document.
 *
 * @param schema optional schema identifier
 * @param schemaVersion authoritative resource schema version
 * @param type nullable registered resource type
 * @param typeVersion nullable registered resource type version
 * @param properties nullable authored properties
 */
public record RawResource(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable String type,
        @Nullable Integer typeVersion,
        @Nullable JsonNode properties) {}
