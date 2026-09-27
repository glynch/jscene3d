/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** Closed authored mutation vocabulary shared by built-in and future authoring clients. */
public sealed interface AuthoringMutation permits AuthoringMutation.Set, AuthoringMutation.Remove {
    /** Adds or replaces one authored value.
     *
     * @param value complete typed authored value
     */
    record Set(ProjectValue value) implements AuthoringMutation {
        /** Validates the replacement value. */
        public Set {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Removes one authored property so descriptor default or unset semantics apply. */
    record Remove() implements AuthoringMutation {}
}
