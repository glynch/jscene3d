/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import io.github.glynch.jscene3d.project.component.ComponentId;
import java.util.Objects;

/**
 * Stable target of one locally authored component within an authored asset.
 *
 * @param entity stable identity of the component's owning entity
 * @param component stable identity of the component on that entity
 */
public record ComponentTarget(EntityId entity, ComponentId component) {
    /** Validates the two identity dimensions of one component target. */
    public ComponentTarget {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(component, "component");
    }
}
