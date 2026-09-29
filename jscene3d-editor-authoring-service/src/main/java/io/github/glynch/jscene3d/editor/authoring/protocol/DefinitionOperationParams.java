/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Generation- and revision-scoped authored-definition operation.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param assetId authoritative definition asset identity
 * @param expectedDefinitionRevision definition revision observed by the client
 */
public record DefinitionOperationParams(
        long expectedProjectGeneration, String assetId, long expectedDefinitionRevision) {
    /** Validates the optimistic concurrency context. */
    public DefinitionOperationParams {
        Objects.requireNonNull(assetId, "assetId");
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
    }
}
