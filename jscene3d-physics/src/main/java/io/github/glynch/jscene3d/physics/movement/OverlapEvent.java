/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import io.github.glynch.jscene3d.physics.CollisionSensor;
import java.util.Objects;

/**
 * A deterministic collision-sensor transition produced by one kinematic move.
 *
 * @param sensor sensor involved in the transition
 * @param phase overlap lifecycle phase
 */
public record OverlapEvent(CollisionSensor sensor, OverlapPhase phase) {
    /** Validates both event components. */
    public OverlapEvent {
        Objects.requireNonNull(sensor, "sensor");
        Objects.requireNonNull(phase, "phase");
    }
}
