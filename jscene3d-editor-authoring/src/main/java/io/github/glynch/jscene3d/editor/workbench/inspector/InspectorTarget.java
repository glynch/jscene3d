/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Stable semantic target of an Inspector projection.
 *
 * @param kind semantic target kind
 * @param source authoritative source
 * @param identity domain identity within the source
 * @param occurrence hierarchy occurrence for hierarchy-scoped targets
 */
public record InspectorTarget(Kind kind, Path source, String identity, Optional<HierarchyOccurrenceId> occurrence) {
    /** Copies and validates the target identity. */
    public InspectorTarget {
        Objects.requireNonNull(kind, "kind");
        source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(occurrence, "occurrence");
    }

    /** Semantic target kinds independent of any editor view. */
    public enum Kind {
        /** Opened world root. */
        WORLD,
        /** Locally authored entity. */
        LOCAL_ENTITY,
        /** Read-only entity realized beneath a placement. */
        GENERATED_ENTITY,
        /** Reusable-definition placement. */
        PLACEMENT,
        /** Project asset. */
        ASSET
    }
}
