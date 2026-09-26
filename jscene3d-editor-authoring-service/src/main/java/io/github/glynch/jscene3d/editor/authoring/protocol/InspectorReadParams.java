/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/** Generation- and revision-scoped request for one complete Inspector snapshot.
 *
 * @param expectedProjectGeneration active project generation observed by the client
 * @param expectedDefinitionRevision retained definition revision observed by the client
 * @param target Java-issued semantic target selected in the Hierarchy
 */
public record InspectorReadParams(
        long expectedProjectGeneration, long expectedDefinitionRevision, DefinitionSnapshot.SemanticTarget target) {
    /** Validates the optimistic concurrency context. */
    public InspectorReadParams {
        Objects.requireNonNull(target, "target");
        if (expectedProjectGeneration < 1) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        if (expectedDefinitionRevision < 0) {
            throw new IllegalArgumentException("expectedDefinitionRevision must be non-negative");
        }
    }
}
