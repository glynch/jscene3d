/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Generation-scoped request for an isolated viewport renderer launch.
 *
 * @param expectedProjectGeneration project generation observed by the client
 * @param sceneAssetId authoritative scene-definition identity
 */
public record ViewportLaunchParams(long expectedProjectGeneration, String sceneAssetId) {
    /** Validates the generation and Scene identity text. */
    public ViewportLaunchParams {
        Objects.requireNonNull(sceneAssetId, "sceneAssetId");
        if (expectedProjectGeneration <= 0) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (sceneAssetId.isBlank()) {
            throw new IllegalArgumentException("sceneAssetId must not be blank");
        }
    }
}
