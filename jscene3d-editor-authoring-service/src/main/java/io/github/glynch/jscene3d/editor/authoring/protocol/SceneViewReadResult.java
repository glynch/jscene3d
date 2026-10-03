/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Exact authoring response for one safe Scene View request.
 *
 * @param accepted whether the request addressed the active project generation
 * @param projectGeneration active generation for an accepted request
 * @param sceneAssetId requested authoritative Scene identity
 * @param requestedRevision definition revision supplied by the client
 * @param outcome projection outcome for an accepted request
 * @param currentRevision current Scene revision when retained
 * @param snapshot complete immutable visual snapshot when projected
 * @param launch fixed product-owned renderer launch context when projected
 * @param diagnostics projection diagnostics
 * @param failureCode stable ownership failure for a rejected request
 */
public record SceneViewReadResult(
        boolean accepted,
        @Nullable Long projectGeneration,
        String sceneAssetId,
        long requestedRevision,
        @Nullable String outcome,
        @Nullable Long currentRevision,
        @Nullable SceneViewSnapshotDto snapshot,
        @Nullable LaunchSpecification launch,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Copies and validates one exact result shape. */
    public SceneViewReadResult {
        if (Objects.requireNonNull(sceneAssetId, "sceneAssetId").isBlank()) {
            throw new IllegalArgumentException("sceneAssetId must not be blank");
        }
        if (requestedRevision < 0) {
            throw new IllegalArgumentException("requestedRevision must be non-negative");
        }
        diagnostics = List.copyOf(diagnostics);
        if (accepted) {
            if (projectGeneration == null || projectGeneration < 1 || outcome == null || failureCode != null) {
                throw new IllegalArgumentException("accepted Scene View result has an inconsistent shape");
            }
            boolean projected = "projected".equals(outcome);
            if (projected != (snapshot != null && launch != null)) {
                throw new IllegalArgumentException("projected Scene View result requires snapshot and launch");
            }
            if (snapshot != null
                    && (!sceneAssetId.equals(snapshot.sceneAssetId())
                            || !Objects.equals(currentRevision, snapshot.revision()))) {
                throw new IllegalArgumentException("snapshot identity must match the Scene View result");
            }
        } else if (projectGeneration != null
                || outcome != null
                || currentRevision != null
                || snapshot != null
                || launch != null
                || failureCode == null
                || !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("rejected Scene View result has an inconsistent shape");
        }
    }

    /**
     * Safe renderer launch inputs containing no title runtime artifacts.
     *
     * @param projectId stable project identity
     * @param projectName author-facing project name
     * @param projectRoot normalized project root
     * @param publishedContentRoot normalized published-content root
     * @param engineVersion product engine version
     * @param sceneName author-facing Scene name
     */
    public record LaunchSpecification(
            String projectId,
            String projectName,
            String projectRoot,
            String publishedContentRoot,
            String engineVersion,
            String sceneName) {
        /** Validates one complete safe renderer launch context. */
        public LaunchSpecification {
            requireText(projectId, "projectId");
            requireText(projectName, "projectName");
            requireText(projectRoot, "projectRoot");
            requireText(publishedContentRoot, "publishedContentRoot");
            requireText(engineVersion, "engineVersion");
            requireText(sceneName, "sceneName");
        }

        private static void requireText(String value, String name) {
            if (Objects.requireNonNull(value, name).isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
        }
    }
}
