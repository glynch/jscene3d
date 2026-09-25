/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Explicit wire snapshot of one retained structural definition.
 *
 * @param revision authoritative authoring revision
 * @param context definition/document context outside the entity tree
 * @param roots actual ordered hierarchy roots
 */
public record DefinitionSnapshot(long revision, DefinitionContext context, List<HierarchyNode> roots) {
    /** Copies and validates the complete snapshot. */
    public DefinitionSnapshot {
        Objects.requireNonNull(context, "context");
        roots = List.copyOf(roots);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    /** Definition-level identity, origin, and resource context.
     *
     * @param assetId authoritative structural-definition identity
     * @param kind stable definition kind
     * @param origin stable authored/generated origin
     * @param editable whether definition mutations are supported
     * @param source absolute logical source URI
     * @param label authored or semantic presentation text
     */
    public record DefinitionContext(
            String assetId, String kind, String origin, boolean editable, String source, AuthoringTextDto label) {
        /** Validates definition context values. */
        public DefinitionContext {
            Objects.requireNonNull(assetId, "assetId");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(label, "label");
        }
    }

    /** Authored literal or Java-owned localizable semantic text.
     *
     * @param kind {@code literal} or {@code message}
     * @param text authored value or complete English fallback text
     * @param messageCode Java-owned message code, only for message text
     * @param arguments ordered formatting arguments
     */
    public record AuthoringTextDto(
            String kind, String text, @Nullable String messageCode, List<String> arguments) {
        /** Copies and validates presentation text. */
        public AuthoringTextDto {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            arguments = List.copyOf(arguments);
            if (!(kind.equals("literal") || kind.equals("message"))) {
                throw new IllegalArgumentException("kind must be literal or message");
            }
            if (kind.equals("message") != (messageCode != null)) {
                throw new IllegalArgumentException("message text requires a messageCode");
            }
        }
    }

    /** Stable occurrence identity within one containing definition.
     *
     * @param definitionAssetId containing definition identity
     * @param entityPath ordered local entity/placement path
     */
    public record Occurrence(String definitionAssetId, List<String> entityPath) {
        /** Copies and validates occurrence identity. */
        public Occurrence {
            Objects.requireNonNull(definitionAssetId, "definitionAssetId");
            entityPath = List.copyOf(entityPath);
        }
    }

    /** Stable semantic target retained for the future Inspector boundary.
     *
     * @param kind semantic target kind
     * @param source absolute logical source URI
     * @param identity domain identity within the source
     * @param occurrence hierarchy occurrence when hierarchy-scoped
     */
    public record SemanticTarget(
            String kind,
            String source,
            String identity,
            @Nullable Occurrence occurrence) {
        /** Validates semantic target values. */
        public SemanticTarget {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(identity, "identity");
        }
    }

    /** One recursively ordered hierarchy occurrence.
     *
     * @param occurrence stable occurrence identity
     * @param kind semantic hierarchy kind
     * @param entityId local entity or placement identity, when applicable
     * @param definitionId referenced definition identity, when applicable
     * @param label authored or semantic presentation text
     * @param enabled authored enabled state
     * @param modified whether this occurrence differs from its saved revision
     * @param editable whether this occurrence supports mutations
     * @param target semantic Inspector target
     * @param children ordered child occurrences
     */
    public record HierarchyNode(
            Occurrence occurrence,
            String kind,
            @Nullable String entityId,
            @Nullable String definitionId,
            AuthoringTextDto label,
            boolean enabled,
            boolean modified,
            boolean editable,
            SemanticTarget target,
            List<HierarchyNode> children) {
        /** Copies and validates one hierarchy node. */
        public HierarchyNode {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(target, "target");
            children = List.copyOf(children);
        }
    }
}
