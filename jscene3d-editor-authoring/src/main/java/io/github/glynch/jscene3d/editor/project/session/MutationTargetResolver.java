/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.Objects;
import java.util.Optional;

/** Resolves and validates mutation targets against one registry's current authoritative revision. */
final class MutationTargetResolver {
    private final RetainedDefinitionRegistry definitions;

    MutationTargetResolver(RetainedDefinitionRegistry definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
    }

    Resolution resolve(AssetId definition, InspectorMutationTarget target, long expectedRevision) {
        AssetId validDefinition = Objects.requireNonNull(definition, "definition");
        InspectorMutationTarget validTarget = Objects.requireNonNull(target, "target");
        Optional<AuthoredDefinitionWorkingCopy> authored = definitions.workingCopy(validDefinition);
        if (authored.isEmpty()) {
            return new Resolution.Rejected(
                    unavailableOutcome(validDefinition),
                    currentRevision(validDefinition, expectedRevision),
                    Optional.empty());
        }
        AuthoredDefinitionWorkingCopy workingCopy = authored.orElseThrow();
        long currentRevision = workingCopy.state().revision();
        if (currentRevision != expectedRevision) {
            return new Resolution.Rejected(
                    AuthoringMutationResult.Outcome.STALE_REVISION, currentRevision, Optional.of(workingCopy));
        }
        if (!validTarget.occurrence().definition().equals(validDefinition)) {
            return new Resolution.Rejected(
                    AuthoringMutationResult.Outcome.INVALID_TARGET, currentRevision, Optional.of(workingCopy));
        }
        Optional<EditorHierarchyNode> node = editableNode(validDefinition, validTarget.occurrence());
        if (node.isEmpty() || !matchesEntity(node.orElseThrow(), validTarget)) {
            return new Resolution.Rejected(
                    AuthoringMutationResult.Outcome.INVALID_TARGET, currentRevision, Optional.of(workingCopy));
        }
        return new Resolution.Accepted(workingCopy);
    }

    AuthoringMutationResult.Outcome unavailableOutcome(AssetId definition) {
        return definitions.retainedKind(definition).isPresent()
                ? AuthoringMutationResult.Outcome.NON_EDITABLE
                : AuthoringMutationResult.Outcome.INVALID_TARGET;
    }

    long currentRevision(AssetId definition, long expectedRevision) {
        return definitions
                .snapshot(definition)
                .map(EditorRetainedDefinition::revision)
                .orElse(Math.max(0L, expectedRevision));
    }

    private Optional<EditorHierarchyNode> editableNode(AssetId definition, HierarchyOccurrenceId occurrence) {
        return definitions.snapshot(definition).stream()
                .flatMap(retained -> retained.hierarchy().roots().stream())
                .map(root -> findNode(root, occurrence))
                .flatMap(Optional::stream)
                .findFirst()
                .filter(EditorHierarchyNode::isEditable);
    }

    private static Optional<EditorHierarchyNode> findNode(EditorHierarchyNode node, HierarchyOccurrenceId occurrence) {
        if (node.occurrence().equals(occurrence)) {
            return Optional.of(node);
        }
        return node.children().stream()
                .map(child -> findNode(child, occurrence))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean matchesEntity(EditorHierarchyNode node, InspectorMutationTarget target) {
        EntityId targetEntity =
                switch (target) {
                    case InspectorMutationTarget.EntityEnabled enabled -> enabled.entity();
                    case InspectorMutationTarget.ComponentProperty property -> property.entity();
                };
        return node.entityId().filter(targetEntity::equals).isPresent();
    }

    /** Current authoritative result of resolving one requested mutation target. */
    sealed interface Resolution permits Resolution.Accepted, Resolution.Rejected {
        /** A mutable target and its sole authoritative working copy. */
        record Accepted(AuthoredDefinitionWorkingCopy workingCopy) implements Resolution {
            public Accepted {
                Objects.requireNonNull(workingCopy, "workingCopy");
            }
        }

        /** A target or revision rejection with the current authoritative revision. */
        record Rejected(
                AuthoringMutationResult.Outcome outcome,
                long revision,
                Optional<AuthoredDefinitionWorkingCopy> workingCopy)
                implements Resolution {
            public Rejected {
                Objects.requireNonNull(outcome, "outcome");
                Objects.requireNonNull(workingCopy, "workingCopy");
                if (revision < 0) {
                    throw new IllegalArgumentException("revision must be non-negative");
                }
            }
        }
    }
}
