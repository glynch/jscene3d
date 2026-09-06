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
     * @param contract nullable exported contract
     * @param connections nullable internal connections
     * @param root nullable local root entry
     */
    public record EntityDocument(
            @JsonProperty("$schema") @Nullable String schema,
            @Nullable String assetId,
            @Nullable String assetType,
            int formatVersion,
            @Nullable String name,
            @Nullable Contract contract,
            @Nullable List<@Nullable Connection> connections,
            @Nullable Entry root) {}

    /** Complete world-definition document.
     *
     * @param schema optional schema URI
     * @param assetId nullable stable asset identity
     * @param assetType nullable asset kind
     * @param formatVersion asset-format version
     * @param name nullable display name
     * @param connections nullable internal connections
     * @param roots nullable root entries
     */
    public record WorldDocument(
            @JsonProperty("$schema") @Nullable String schema,
            @Nullable String assetId,
            @Nullable String assetType,
            int formatVersion,
            @Nullable String name,
            @Nullable List<@Nullable Connection> connections,
            @Nullable List<@Nullable Entry> roots) {}

    /** Exported reusable-definition contract.
     *
     * @param parameters nullable exported parameters
     * @param signals nullable exported signals
     * @param actions nullable exported actions
     * @param capabilities nullable declared capabilities
     * @param attachments nullable exported attachments
     * @param resourceBindings nullable exported resource bindings
     */
    public record Contract(
            @Nullable List<@Nullable Parameter> parameters,
            @Nullable List<@Nullable Endpoint> signals,
            @Nullable List<@Nullable Endpoint> actions,
            @Nullable List<@Nullable String> capabilities,
            @Nullable List<@Nullable Attachment> attachments,
            @Nullable List<@Nullable ResourceBinding> resourceBindings) {}

    /** Exported parameter declaration.
     *
     * @param id nullable public property identity
     * @param valueKind nullable structural value kind
     * @param required optional required flag
     * @param target nullable private property target
     */
    public record Parameter(
            @Nullable String id,
            @Nullable String valueKind,
            @Nullable Boolean required,
            @Nullable PropertyTarget target) {}

    /** Exported signal or action declaration.
     *
     * @param id nullable public endpoint identity
     * @param payload optional registered payload type
     * @param target nullable private endpoint target
     */
    public record Endpoint(
            @Nullable String id,
            @Nullable RegisteredType payload,
            @Nullable EndpointTarget target) {}

    /** Registered payload type reference.
     *
     * @param id nullable registered type identity
     * @param version nullable positive type version
     */
    public record RegisteredType(
            @Nullable String id, @Nullable Integer version) {}

    /** Exported attachment declaration.
     *
     * @param id nullable public attachment identity
     * @param target nullable private spatial target
     */
    public record Attachment(@Nullable String id, @Nullable SpatialTarget target) {}

    /** Exported resource-binding declaration.
     *
     * @param id nullable public property identity
     * @param required optional required flag
     * @param acceptedKinds nullable accepted resource-reference kinds
     * @param target nullable private property target
     */
    public record ResourceBinding(
            @Nullable String id,
            @Nullable Boolean required,
            @Nullable List<@Nullable String> acceptedKinds,
            @Nullable PropertyTarget target) {}

    /** Stable property target.
     *
     * @param entityId nullable entity or placement identity
     * @param componentId optional local component identity
     * @param propertyId nullable component property or contract argument identity
     */
    public record PropertyTarget(
            @Nullable String entityId,
            @Nullable String componentId,
            @Nullable String propertyId) {}

    /** Stable signal or action target.
     *
     * @param entityId nullable entity or placement identity
     * @param componentId optional local component identity
     * @param endpointId nullable endpoint identity
     */
    public record EndpointTarget(
            @Nullable String entityId,
            @Nullable String componentId,
            @Nullable String endpointId) {}

    /** Stable spatial target.
     *
     * @param entityId nullable entity or placement identity
     * @param componentId optional local component identity
     * @param attachmentId optional attachment identity
     */
    public record SpatialTarget(
            @Nullable String entityId,
            @Nullable String componentId,
            @Nullable String attachmentId) {}

    /** Internal signal-to-action connection.
     *
     * @param signal nullable signal source
     * @param action nullable action destination
     */
    public record Connection(
            @Nullable EndpointTarget signal, @Nullable EndpointTarget action) {}

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
