/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Nullable Jackson records for version-one authored definition documents. */
public final class RawDefinitionDocuments {
    /** Prevents construction of this raw-model namespace. */
    private RawDefinitionDocuments() {
        throw new AssertionError("RawDefinitionDocuments cannot be instantiated");
    }

    /** Complete reusable entity-definition document.
     *
     * @param schema optional schema URI
     * @param assetId nullable stable asset identity
     * @param assetType nullable asset kind
     * @param formatVersion asset-format version
     * @param name nullable display name
     * @param root nullable local root entry
     */
    public record EntityDocument(
            @JsonProperty("$schema") @Nullable String schema,
            @Nullable String assetId,
            @Nullable String assetType,
            int formatVersion,
            @Nullable String name,
            @Nullable Entry root) {}

    /** Complete world-definition document.
     *
     * @param schema optional schema URI
     * @param assetId nullable stable asset identity
     * @param assetType nullable asset kind
     * @param formatVersion asset-format version
     * @param name nullable display name
     * @param roots nullable root entries
     */
    public record WorldDocument(
            @JsonProperty("$schema") @Nullable String schema,
            @Nullable String assetId,
            @Nullable String assetType,
            int formatVersion,
            @Nullable String name,
            @Nullable List<@Nullable Entry> roots) {}

    /** Local entity or reusable-definition placement.
     *
     * @param entryType nullable entry discriminator
     * @param entityId nullable stable entity identity
     * @param name optional display name
     * @param enabled optional local enabled state
     * @param components local-entity components
     * @param children local-entity children
     * @param definition placement definition reference
     * @param arguments placement contract arguments
     */
    public record Entry(
            @Nullable String entryType,
            @Nullable String entityId,
            @Nullable String name,
            @Nullable Boolean enabled,
            @Nullable List<@Nullable Component> components,
            @Nullable List<@Nullable Entry> children,
            @Nullable Reference definition,
            @Nullable JsonNode arguments) {}

    /** Authored component record.
     *
     * @param componentId nullable stable component identity
     * @param type nullable registered component type
     * @param typeVersion nullable component configuration version
     * @param properties optional component properties
     */
    public record Component(
            @Nullable String componentId,
            @Nullable String type,
            @Nullable Integer typeVersion,
            @Nullable JsonNode properties) {}

    /** Stable asset reference with an optional non-authoritative location hint.
     *
     * @param assetId nullable authoritative asset identity
     * @param pathHint optional diagnostic path hint
     */
    public record Reference(
            @Nullable String assetId, @Nullable String pathHint) {}
}
