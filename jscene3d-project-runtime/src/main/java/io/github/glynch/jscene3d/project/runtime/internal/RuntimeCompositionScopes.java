/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.composition.CompositionScope;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Live runtime indexes corresponding to immutable safe composition scopes. */
final class RuntimeCompositionScopes {
    private final Map<CompositionScope, EntityInstanceScope> scopes = new LinkedHashMap<>();

    /** Returns or creates the live index for one planned scope. */
    EntityInstanceScope require(CompositionScope scope) {
        return scopes.computeIfAbsent(Objects.requireNonNull(scope, "scope"), ignored -> new EntityInstanceScope());
    }
}
