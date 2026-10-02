/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** One effective portable value and the authored scope of any targets it contains.
 *
 * @param value effective portable value
 * @param authoredScope definition occurrence in which targets were authored
 */
public record CompositionValue(ProjectValue value, CompositionScope authoredScope) {
    /** Validates one scoped value. */
    public CompositionValue {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(authoredScope, "authoredScope");
    }
}
