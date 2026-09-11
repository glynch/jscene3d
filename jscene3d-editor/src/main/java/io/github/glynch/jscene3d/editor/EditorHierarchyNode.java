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
@SuppressWarnings("exports") // Public only across this module's implementation packages; the package is not exported.
public final class EditorHierarchyNode {
    private final Kind kind;
    private final String label;
    private final Optional<EntityId> entityId;
    private final Optional<AssetId> definitionId;
    private final boolean enabled;
    private final EditorSelection selection;
    private final List<EditorHierarchyNode> children;

    /**
     * Stores one hierarchy projection while retaining its stable identities.
     *
     * @param kind hierarchy entry kind
     * @param label human-readable label
     * @param entityId optional entity identity
     * @param definitionId optional referenced definition identity
     * @param enabled whether the entry starts locally enabled
     * @param selection shared editor selection
     * @param children ordered child entries
     */
    public EditorHierarchyNode(
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

    /**
     * Returns the projected hierarchy kind.
     *
     * @return hierarchy kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the editor label.
     *
     * @return human-readable label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the stable entity identity when this is an entity entry.
     *
     * @return optional entity identity
     */
    public Optional<EntityId> entityId() {
        return entityId;
    }

    /**
     * Returns the referenced definition identity for a world or placement.
     *
     * @return optional definition identity
     */
    public Optional<AssetId> definitionId() {
        return definitionId;
    }

    /**
     * Returns whether the authored entry starts locally enabled.
     *
     * @return whether the entry is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the stable shared selection represented by this hierarchy entry.
     *
     * @return shared selection
     */
    public EditorSelection selection() {
        return selection;
    }

    /**
     * Returns projected child entries in authored order.
     *
     * @return immutable child entries
     */
    public List<EditorHierarchyNode> children() {
        return children;
    }

    /** Adds a disabled marker without exposing stable IDs in the ordinary tree label. */
    @Override
    public String toString() {
        return enabled ? label : label + " (disabled)";
    }

    /** Kinds displayed by the editor hierarchy. */
    public enum Kind {
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
