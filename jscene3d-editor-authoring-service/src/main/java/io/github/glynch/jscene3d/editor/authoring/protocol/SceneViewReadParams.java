/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/**
 * Generation- and revision-scoped request for one editor-safe Scene View snapshot.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param sceneAssetId authoritative Scene asset identity
 * @param expectedDefinitionRevision Scene working-copy revision observed by the client
 */
public record SceneViewReadParams(
        long expectedProjectGeneration, String sceneAssetId, long expectedDefinitionRevision) {
    /** Validates one exact Scene View request identity. */
    public SceneViewReadParams {
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (Objects.requireNonNull(sceneAssetId, "sceneAssetId").isBlank()) {
            throw new IllegalArgumentException("sceneAssetId must not be blank");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
    }
}
