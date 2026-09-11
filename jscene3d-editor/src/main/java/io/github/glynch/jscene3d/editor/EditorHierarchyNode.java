/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable read-only projection of one authored hierarchy entry. */
final class EditorHierarchyNode {
    private final Kind kind;
    private final String label;
    private final Optional<EntityId> entityId;
    private final Optional<AssetId> definitionId;
    private final boolean enabled;
    private final EditorSelection selection;
    private final List<EditorHierarchyNode> children;

    /** Stores one hierarchy projection while retaining its stable identities. */
    EditorHierarchyNode(
            Kind kind,
            String label,
            Optional<EntityId> entityId,
            Optional<AssetId> definitionId,
            boolean enabled,
            EditorSelection selection,
            List<EditorHierarchyNode> children) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.label = Objects.requireNonNull(label, "label");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
        this.enabled = enabled;
        this.selection = Objects.requireNonNull(selection, "selection");
        this.children = List.copyOf(children);
    }

    /** Returns the projected hierarchy kind. */
    Kind kind() {
        return kind;
    }

    /** Returns the editor label. */
    String label() {
        return label;
    }

    /** Returns the stable entity identity when this is an entity entry. */
    Optional<EntityId> entityId() {
        return entityId;
    }

    /** Returns the referenced definition identity for a world or placement. */
    Optional<AssetId> definitionId() {
        return definitionId;
    }

    /** Returns whether the authored entry starts locally enabled. */
    boolean isEnabled() {
        return enabled;
    }

    /** Returns the stable shared selection represented by this hierarchy entry. */
    EditorSelection selection() {
        return selection;
    }

    /** Returns projected child entries in authored order. */
    List<EditorHierarchyNode> children() {
        return children;
    }

    /** Adds a disabled marker without exposing stable IDs in the ordinary tree label. */
    @Override
    public String toString() {
        return enabled ? label : label + " (disabled)";
    }

    /** Kinds displayed by the editor hierarchy. */
    enum Kind {
        /** Opened world definition. */
        WORLD,
        /** Locally authored entity. */
        LOCAL_ENTITY,
        /** Reusable entity-definition placement. */
        PLACEMENT,
        /** Local entity projected from inside a placed reusable definition. */
        GENERATED_ENTITY
    }
}
