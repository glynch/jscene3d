/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import java.util.Objects;
import java.util.Optional;

/** One typed Inspector property with optional mutation identity.
 *
 * @param identity stable property identity within its section
 * @param presentation descriptor-backed presentation and constraints
 * @param state typed projected value and origin
 * @param mutationTarget stable mutation target when editable
 */
public record InspectorProperty(
        String identity, Presentation presentation, State state, Optional<InspectorMutationTarget> mutationTarget) {
    /** Copies and validates the property. */
    public InspectorProperty {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(presentation, "presentation");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mutationTarget, "mutationTarget");
    }

    /**
     * Descriptor-backed property presentation and structural metadata.
     *
     * @param label author-facing label
     * @param valueKind structural value kind
     * @param required whether authored data must contain the property
     * @param description optional author-facing description
     * @param constraints structured descriptor constraints
     */
    public record Presentation(
            AuthoringText label,
            ProjectValueKind valueKind,
            boolean required,
            Optional<AuthoringText> description,
            InspectorConstraints constraints) {
        /** Validates presentation metadata. */
        public Presentation {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(constraints, "constraints");
        }
    }

    /**
     * Typed projected value and provenance.
     *
     * @param authoredValue value explicitly present in authored data
     * @param defaultValue value declared by descriptor metadata
     * @param effectiveValue authored value or descriptor default, empty when unset
     * @param origin value provenance
     * @param validity current semantic validity
     * @param editable whether a future mutation may target this property
     * @param modified whether the authored target differs from its persisted baseline
     */
    public record State(
            Optional<InspectorValue> authoredValue,
            Optional<InspectorValue> defaultValue,
            Optional<InspectorValue> effectiveValue,
            Origin origin,
            Validity validity,
            boolean editable,
            boolean modified) {
        /** Validates typed state. */
        public State {
            Objects.requireNonNull(authoredValue, "authoredValue");
            Objects.requireNonNull(defaultValue, "defaultValue");
            Objects.requireNonNull(effectiveValue, "effectiveValue");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(validity, "validity");
        }
    }

    /** Projected value provenance. */
    public enum Origin {
        /** Value is present in authored data. */
        AUTHORED,
        /** Value comes from descriptor metadata. */
        DEFAULT,
        /** Neither authored nor default value is present. */
        UNSET
    }

    /** Semantic validity independent of value provenance and editability. */
    public enum Validity {
        /** The effective value satisfies currently available metadata. */
        VALID,
        /** A required property has neither an authored value nor a default. */
        REQUIRED_UNSET,
        /** A semantic reference or authored target cannot currently be resolved. */
        BROKEN_REFERENCE,
        /** Exact property descriptor metadata is unavailable. */
        METADATA_UNAVAILABLE
    }
}
