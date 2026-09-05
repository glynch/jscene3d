/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model for one input-map document.
 *
 * @param schema optional schema identifier
 * @param schemaVersion authoritative input-map schema version
 * @param actions nullable physical bindings indexed by semantic action
 */
public record RawInputMap(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable Map<String, @Nullable List<RawInputMap.@Nullable Binding>> actions) {
    /** Nullable physical binding fields.
     *
     * @param device nullable device name
     * @param key nullable keyboard key
     * @param button nullable mouse button
     */
    public record Binding(
            @Nullable String device,
            @Nullable String key,
            @Nullable String button) {}
}
