/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.Objects;

/** One authored identity through which an expanded occurrence is addressable.
 *
 * @param scope containing definition occurrence
 * @param entity local-entity or placement identity in that scope
 */
public record CompositionEntityBinding(CompositionScope scope, EntityId entity) {
    /** Validates one binding. */
    public CompositionEntityBinding {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(entity, "entity");
    }
}
