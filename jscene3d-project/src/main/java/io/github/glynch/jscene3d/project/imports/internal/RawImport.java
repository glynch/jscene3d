/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model for one source-import definition.
 *
 * @param schema optional schema identifier
 * @param schemaVersion authoritative import schema version
 * @param id nullable project-local import identity
 * @param source nullable asset reference
 * @param importer nullable registered importer identity
 * @param selection nullable source-item selection
 * @param settings nullable importer-wide settings
 * @param itemSettings nullable per-source-item settings
 */
public record RawImport(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable String id,
        @Nullable String source,
        @Nullable String importer,
        @Nullable JsonNode selection,
        @Nullable JsonNode settings,
        @Nullable JsonNode itemSettings) {}
