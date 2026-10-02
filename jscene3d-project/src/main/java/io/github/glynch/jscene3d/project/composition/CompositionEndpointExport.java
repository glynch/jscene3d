/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import java.util.Objects;

/** Scoped authored mapping from a placement contract endpoint to its private target.
 *
 * @param containingScope scope containing the placement
 * @param exportedTarget placement-contract endpoint visible in the containing scope
 * @param targetScope private instantiated-definition scope
 * @param target private component or nested-placement endpoint
 * @param kind endpoint direction
 * @param location diagnostic location within the root composition
 */
public record CompositionEndpointExport(
        CompositionScope containingScope,
        EndpointTarget exportedTarget,
        CompositionScope targetScope,
        EndpointTarget target,
        CompositionEndpointKind kind,
        String location) {
    /** Validates one scoped endpoint export. */
    public CompositionEndpointExport {
        Objects.requireNonNull(containingScope, "containingScope");
        Objects.requireNonNull(exportedTarget, "exportedTarget");
        Objects.requireNonNull(targetScope, "targetScope");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(location, "location");
    }
}
