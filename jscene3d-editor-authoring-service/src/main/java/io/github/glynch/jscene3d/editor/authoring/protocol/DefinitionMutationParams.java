/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** One exact SET or REMOVE request against a Java-issued Inspector mutation target.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param assetId authoritative definition asset identity
 * @param expectedDefinitionRevision definition revision observed by the client
 * @param operation stable {@code set} or {@code remove} operation
 * @param target Java-issued semantic mutation target
 * @param value typed candidate for {@code set}, otherwise {@code null}
 */
public record DefinitionMutationParams(
        long expectedProjectGeneration,
        String assetId,
        long expectedDefinitionRevision,
        String operation,
        MutationTarget target,
        @Nullable CandidateValue value) {
    /** Validates the complete mutation request shape. */
    public DefinitionMutationParams {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(target, "target");
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
        if (!("set".equals(operation) || "remove".equals(operation))) {
            throw new IllegalArgumentException("operation must be set or remove");
        }
        if (("set".equals(operation)) != (value != null)) {
            throw new IllegalArgumentException("value must be present exactly for set");
        }
    }

    /** Stable mutation identity copied from an Inspector snapshot.
     *
     * @param kind target kind
     * @param occurrence containing definition and entity path
     * @param entityId authored entity identity
     * @param componentId component identity for a property target
     * @param propertyId property identity for a property target
     */
    public record MutationTarget(
            String kind,
            DefinitionSnapshot.Occurrence occurrence,
            String entityId,
            @Nullable String componentId,
            @Nullable String propertyId) {
        /** Validates one closed target variant. */
        public MutationTarget {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(entityId, "entityId");
            if ("entity-enabled".equals(kind)) {
                if (componentId != null || propertyId != null) {
                    throw new IllegalArgumentException("entity-enabled target cannot contain property identity");
                }
            } else if ("component-property".equals(kind)) {
                if (componentId == null || propertyId == null) {
                    throw new IllegalArgumentException("component-property target requires property identity");
                }
            } else {
                throw new IllegalArgumentException("unsupported mutation target kind");
            }
        }
    }

    /** Exact candidate without binary floating-point conversion.
     *
     * @param kind boolean, integer, number, text, or number-array
     * @param value boolean candidate value
     * @param literal exact integer, decimal, or text literal
     * @param literals exact decimal components for a numeric array
     */
    public record CandidateValue(
            String kind,
            @Nullable Boolean value,
            @Nullable String literal,
            @Nullable List<String> literals) {
        /** Preserves the scalar construction form used by non-array clients.
         *
         * @param kind boolean, integer, number, or text
         * @param value boolean candidate value
         * @param literal exact integer, decimal, or text literal
         */
        public CandidateValue(String kind, @Nullable Boolean value, @Nullable String literal) {
            this(kind, value, literal, null);
        }

        /** Validates one supported candidate shape. */
        public CandidateValue {
            Objects.requireNonNull(kind, "kind");
            if ("boolean".equals(kind)) {
                if (value == null || literal != null || literals != null) {
                    throw new IllegalArgumentException("boolean candidate requires only value");
                }
            } else if (List.of("integer", "number", "text").contains(kind)) {
                if (value != null || literal == null || literals != null) {
                    throw new IllegalArgumentException("literal candidate requires only literal");
                }
            } else if ("number-array".equals(kind)) {
                if (value != null || literal != null || literals == null) {
                    throw new IllegalArgumentException("number-array candidate requires only literals");
                }
                literals = List.copyOf(literals);
            } else {
                throw new IllegalArgumentException("unsupported candidate kind");
            }
        }
    }
}
