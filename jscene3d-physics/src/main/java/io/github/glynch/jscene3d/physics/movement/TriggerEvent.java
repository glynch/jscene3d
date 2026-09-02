/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;

/**
 * A deterministic trigger transition produced by one kinematic move.
 *
 * @param trigger trigger collider involved in the transition
 * @param type lifecycle transition type
 */
public record TriggerEvent(Collider trigger, TriggerEventType type) {
    /** Validates both event components. */
    public TriggerEvent {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(type, "type");
    }
}
