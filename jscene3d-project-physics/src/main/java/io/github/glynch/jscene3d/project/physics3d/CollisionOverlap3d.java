/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.util.Objects;

/**
 * Precise overlap relationship delivered by a sensor signal.
 *
 * @param sensor sensor producing the signal
 * @param sensorShape exact sensor shape involved
 * @param other other collision object
 * @param otherShape exact other shape involved
 */
public record CollisionOverlap3d(
        CollisionSensor3d sensor, CollisionShape3d sensorShape, CollisionObject3d other, CollisionShape3d otherShape) {
    /** Validates every addressed runtime object. */
    public CollisionOverlap3d {
        Objects.requireNonNull(sensor, "sensor");
        Objects.requireNonNull(sensorShape, "sensorShape");
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(otherShape, "otherShape");
    }
}
