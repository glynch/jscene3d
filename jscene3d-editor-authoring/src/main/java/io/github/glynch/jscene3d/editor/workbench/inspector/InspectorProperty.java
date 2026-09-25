/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.value.ProjectValue;
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
     * @param value authored or default value, empty when unset
     * @param origin value provenance
     */
    public record State(Optional<ProjectValue> value, Origin origin) {
        /** Validates typed state. */
        public State {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(origin, "origin");
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
}
