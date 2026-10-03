/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.sceneview;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable outcome of requesting a Scene View snapshot from one authoring session.
 *
 * @param outcome exact request outcome
 * @param scene requested authoritative Scene identity
 * @param currentRevision current retained revision when the identity names an open Scene
 * @param snapshot complete snapshot only when projection succeeded
 * @param diagnostics ordered planning or projection diagnostics
 */
public record SceneViewProjectionResult(
        Outcome outcome,
        AssetId scene,
        OptionalLong currentRevision,
        Optional<SceneViewSnapshot> snapshot,
        List<ProjectDiagnostic> diagnostics) {
    /** Copies and validates one projection result. */
    public SceneViewProjectionResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(currentRevision, "currentRevision");
        Objects.requireNonNull(snapshot, "snapshot");
        diagnostics = List.copyOf(diagnostics);
        if (currentRevision.isPresent() && currentRevision.orElseThrow() < 0) {
            throw new IllegalArgumentException("currentRevision must be non-negative");
        }
        if ((outcome == Outcome.PROJECTED) != snapshot.isPresent()) {
            throw new IllegalArgumentException("snapshot must be present exactly when projection succeeded");
        }
        snapshot.ifPresent(value -> {
            if (!scene.equals(value.scene()) || currentRevision.orElse(-1L) != value.revision()) {
                throw new IllegalArgumentException("snapshot identity must match the projection result");
            }
        });
        if (outcome == Outcome.SCENE_UNAVAILABLE && currentRevision.isPresent()) {
            throw new IllegalArgumentException("an unavailable Scene cannot have a current revision");
        }
        if (outcome != Outcome.SCENE_UNAVAILABLE && currentRevision.isEmpty()) {
            throw new IllegalArgumentException("a retained Scene outcome requires its current revision");
        }
    }

    /** Exact authoring-side outcomes for a Scene View projection request. */
    public enum Outcome {
        /** A complete current snapshot was produced. */
        PROJECTED,
        /** The requested identity does not name an open retained Scene. */
        SCENE_UNAVAILABLE,
        /** The requested revision no longer matches the current working copy. */
        STALE_REVISION,
        /** Safe composition planning returned structured diagnostics. */
        PLANNING_FAILED,
        /** Supported visual semantics could not be coherently projected. */
        PROJECTION_FAILED
    }
}
