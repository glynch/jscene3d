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
        @Nullable Map<String, RawInputMap.@Nullable Action> actions) {
    /** Nullable semantic action fields.
     *
     * @param valueType nullable semantic value type
     * @param bindings nullable ordered physical bindings
     */
    public record Action(
            @Nullable String valueType, @Nullable List<RawInputMap.@Nullable Binding> bindings) {}

    /** Nullable physical binding fields.
     *
     * @param device nullable device name
     * @param control nullable device control or composite name
     * @param up nullable positive vertical keyboard control
     * @param down nullable negative vertical keyboard control
     * @param left nullable negative horizontal keyboard control
     * @param right nullable positive horizontal keyboard control
     * @param deadZone nullable gamepad dead-zone magnitude
     * @param scale nullable one-dimensional output multiplier
     * @param scaleX nullable horizontal output multiplier
     * @param scaleY nullable vertical output multiplier
     * @param invertY nullable gamepad-stick vertical inversion
     */
    public record Binding(
            @Nullable String device,
            @Nullable String control,
            @Nullable String up,
            @Nullable String down,
            @Nullable String left,
            @Nullable String right,
            @Nullable Float deadZone,
            @Nullable Float scale,
            @Nullable Float scaleX,
            @Nullable Float scaleY,
            @Nullable Boolean invertY) {}
}
