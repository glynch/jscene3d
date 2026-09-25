/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Generation-scoped structural-definition identity.
 *
 * @param expectedProjectGeneration project generation observed by the client
 * @param assetId authoritative definition asset identity
 */
public record DefinitionOpenParams(long expectedProjectGeneration, String assetId) {
    /** Validates the generation and identity text. */
    public DefinitionOpenParams {
        Objects.requireNonNull(assetId, "assetId");
        if (expectedProjectGeneration <= 0) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
    }
}
