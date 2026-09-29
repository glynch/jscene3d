/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Authored-definition recovery request carrying the exact Java backup bytes as Base64 text.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param assetId authoritative definition asset identity
 * @param expectedDefinitionRevision new-session definition revision observed by the client
 * @param backup Base64-encoded B3 recovery representation
 */
public record DefinitionRestoreParams(
        long expectedProjectGeneration, String assetId, long expectedDefinitionRevision, String backup) {
    /** Validates recovery identity and transport text. */
    public DefinitionRestoreParams {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(backup, "backup");
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
        if (backup.isBlank()) {
            throw new IllegalArgumentException("backup must not be blank");
        }
    }
}
