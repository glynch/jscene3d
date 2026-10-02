/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Generation-scoped request for an isolated viewport renderer launch.
 *
 * @param expectedProjectGeneration project generation observed by the client
 * @param worldAssetId authoritative world-definition identity
 */
public record ViewportLaunchParams(long expectedProjectGeneration, String worldAssetId) {
    /** Validates the generation and world identity text. */
    public ViewportLaunchParams {
        Objects.requireNonNull(worldAssetId, "worldAssetId");
        if (expectedProjectGeneration <= 0) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (worldAssetId.isBlank()) {
            throw new IllegalArgumentException("worldAssetId must not be blank");
        }
    }
}
