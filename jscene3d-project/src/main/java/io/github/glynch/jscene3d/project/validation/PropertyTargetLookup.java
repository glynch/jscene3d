/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation;

import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;

/**
 * Read-only definition-local target lookup used by authoritative property validation.
 *
 * <p>The lookup must be deterministic and side-effect free. It represents the current authored definition scope;
 * component lookup must not cross a placement seam.
 */
public interface PropertyTargetLookup {
    /**
     * Returns whether an entity or placement is addressable in the current definition.
     *
     * @param entity target entity identity
     * @return whether the target is addressable
     */
    boolean containsEntity(EntityId entity);

    /**
     * Returns whether a locally authored component is addressable without crossing a placement seam.
     *
     * @param target component target
     * @return whether the local component exists
     */
    boolean containsComponent(ComponentTarget target);
}
