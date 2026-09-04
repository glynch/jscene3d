/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.importing.internal.Preconditions;

/** Directed relationship between two inspected source items.
 *
 * @param kind adapter-defined relationship kind
 * @param targetIdentity target source-item identity
 */
public record SourceItemRelation(String kind, String targetIdentity) {
    /** Validates the relationship vocabulary and target. */
    public SourceItemRelation {
        kind = Preconditions.requirePortableIdentity(kind, "kind");
        targetIdentity = Preconditions.requirePortableIdentity(targetIdentity, "targetIdentity");
    }
}
