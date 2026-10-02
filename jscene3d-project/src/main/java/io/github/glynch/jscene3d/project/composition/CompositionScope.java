/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.asset.AssetId;
import java.util.Objects;

/** Runtime-free identity of one distinct expansion of an authored definition.
 *
 * @param definition expanded world or reusable-definition asset
 * @param anchor occurrence anchoring this distinct definition instance
 */
public record CompositionScope(AssetId definition, CompositionOccurrenceId anchor) {
    /** Validates one definition-instance scope. */
    public CompositionScope {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(anchor, "anchor");
    }
}
