/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** One phase of an interactive component-property edit.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param assetId authoritative definition asset identity
 * @param expectedDefinitionRevision definition revision observed by the client
 * @param target Java-issued component-property mutation target
 * @param phase {@code begin}, {@code update}, {@code commit}, or {@code cancel}
 * @param editId client-generated identity retained for the complete transaction
 * @param value exact preview candidate, present only for {@code update}
 */
public record DefinitionInteractiveEditParams(
        long expectedProjectGeneration,
        String assetId,
        long expectedDefinitionRevision,
        DefinitionMutationParams.MutationTarget target,
        String phase,
        String editId,
        DefinitionMutationParams.@Nullable CandidateValue value) {
    /** Validates one closed interactive-edit phase. */
    public DefinitionInteractiveEditParams {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(editId, "editId");
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
        if (!"component-property".equals(target.kind())) {
            throw new IllegalArgumentException("interactive edit requires a component-property target");
        }
        if (!List.of("begin", "update", "commit", "cancel").contains(phase)
                || editId.isBlank()
                || editId.length() > 128
                || ("update".equals(phase) != (value != null))) {
            throw new IllegalArgumentException("interactive edit shape is invalid");
        }
    }
}
