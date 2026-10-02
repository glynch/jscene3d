/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable semantic projection of one authored hierarchy occurrence.
 *
 * @param identity stable domain and occurrence identity
 * @param label author-facing label
 * @param authoringState enabled, modified, and mutation state
 * @param inspectorTarget stable target for on-demand inspection
 * @param children child occurrences in authored order
 */
public record EditorHierarchyNode(
        Identity identity,
        AuthoringText label,
        AuthoringState authoringState,
        InspectorTarget inspectorTarget,
        List<EditorHierarchyNode> children) {
    /** Copies and validates the projected node. */
    public EditorHierarchyNode {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(authoringState, "authoringState");
        Objects.requireNonNull(inspectorTarget, "inspectorTarget");
        children = List.copyOf(children);
    }

    /**
     * Returns the stable occurrence identity.
     *
     * @return hierarchy occurrence
     */
    public HierarchyOccurrenceId occurrence() {
        return identity.occurrence();
    }

    /**
     * Returns the semantic hierarchy kind.
     *
     * @return hierarchy kind
     */
    public Kind kind() {
        return identity.kind();
    }

    /**
     * Returns the local entity or placement identity when applicable.
     *
     * @return optional entity identity
     */
    public Optional<EntityId> entityId() {
        return identity.entityId();
    }

    /**
     * Returns the opened Scene or referenced definition identity when applicable.
     *
     * @return optional definition identity
     */
    public Optional<AssetId> definitionId() {
        return identity.definitionId();
    }

    /**
     * Returns whether the authored entry starts locally enabled.
     *
     * @return whether the entry is enabled
     */
    public boolean isEnabled() {
        return authoringState.enabled();
    }

    /**
     * Returns whether this exact authored entry differs from its saved revision.
     *
     * @return whether this hierarchy entry has unsaved changes
     */
    public boolean isModified() {
        return authoringState.modified();
    }

    /**
     * Returns whether this occurrence supports authoring mutations in the containing Scene.
     *
     * @return whether the occurrence is editable
     */
    public boolean isEditable() {
        return authoringState.editable();
    }

    /** Semantic hierarchy entry kinds. */
    public enum Kind {
        /** Opened Scene definition. */
        SCENE,
        /** Locally authored entity. */
        LOCAL_ENTITY,
        /** Reusable entity-definition placement. */
        PLACEMENT,
        /** Entity projected from inside a placed reusable definition. */
        GENERATED_ENTITY
    }

    /**
     * Stable semantic and occurrence identity for one hierarchy node.
     *
     * @param occurrence occurrence-safe hierarchy identity
     * @param kind semantic hierarchy kind
     * @param entityId local entity or placement identity when applicable
     * @param definitionId Scene or referenced definition identity when applicable
     */
    public record Identity(
            HierarchyOccurrenceId occurrence, Kind kind, Optional<EntityId> entityId, Optional<AssetId> definitionId) {
        /** Validates hierarchy identity values. */
        public Identity {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(definitionId, "definitionId");
        }
    }

    /**
     * Current authoring state for one hierarchy occurrence.
     *
     * @param enabled whether the entry starts locally enabled
     * @param modified whether the entry differs from its saved revision
     * @param mutationTarget enabled-state mutation target when this occurrence is locally authored
     */
    public record AuthoringState(
            boolean enabled, boolean modified, Optional<InspectorMutationTarget.EntityEnabled> mutationTarget) {
        /** Validates the optional mutation target. */
        public AuthoringState {
            Objects.requireNonNull(mutationTarget, "mutationTarget");
        }

        /**
         * Returns whether mutations may target this occurrence.
         *
         * @return whether the occurrence is editable
         */
        public boolean editable() {
            return mutationTarget.isPresent();
        }
    }
}
