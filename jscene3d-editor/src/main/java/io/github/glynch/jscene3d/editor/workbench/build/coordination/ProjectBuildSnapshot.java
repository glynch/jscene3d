/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Immutable observable state of one project build coordinator.
 *
 * @param phase current lifecycle and freshness phase
 * @param savedRevision newest known saved build-relevant revision
 * @param successfulRevision most recent successfully built revision, when one exists
 * @param automaticBuild whether relevant saves request builds automatically
 * @param followUpQueued whether one coalesced follow-up is waiting for the active build
 */
public record ProjectBuildSnapshot(
        ProjectBuildPhase phase,
        long savedRevision,
        OptionalLong successfulRevision,
        boolean automaticBuild,
        boolean followUpQueued) {
    /** Validates one observable snapshot. */
    public ProjectBuildSnapshot {
        Objects.requireNonNull(phase, "phase");
        if (savedRevision < 0) {
            throw new IllegalArgumentException("savedRevision must not be negative");
        }
        Objects.requireNonNull(successfulRevision, "successfulRevision");
        if (successfulRevision.isPresent() && successfulRevision.orElseThrow() > savedRevision) {
            throw new IllegalArgumentException("successfulRevision must not exceed savedRevision");
        }
    }
}
