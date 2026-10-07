/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import java.util.Objects;
import java.util.Optional;

/** Revision-qualified result of one Scene View pointer pick. */
record SceneViewSelectionResult(Status status, long revision, Optional<CompositionOccurrenceId> occurrence) {
    enum Status {
        SELECTED,
        CLEARED,
        STALE
    }

    SceneViewSelectionResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurrence, "occurrence");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if ((status == Status.SELECTED) != occurrence.isPresent()) {
            throw new IllegalArgumentException("only a selected result contains an occurrence");
        }
    }

    static SceneViewSelectionResult selected(long revision, CompositionOccurrenceId occurrence) {
        return new SceneViewSelectionResult(Status.SELECTED, revision, Optional.of(occurrence));
    }

    static SceneViewSelectionResult cleared(long revision) {
        return new SceneViewSelectionResult(Status.CLEARED, revision, Optional.empty());
    }

    static SceneViewSelectionResult stale(long revision) {
        return new SceneViewSelectionResult(Status.STALE, revision, Optional.empty());
    }
}
