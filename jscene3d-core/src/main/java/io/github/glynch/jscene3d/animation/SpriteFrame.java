/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.Objects;

/**
 * One immutable sprite-atlas region and its positive display duration.
 *
 * @param region immutable texture region
 * @param durationSeconds display duration in seconds
 */
public record SpriteFrame(TextureRegion region, float durationSeconds) {
    /**
     * Creates one timed atlas frame.
     *
     * @param region immutable texture region
     * @param durationSeconds finite positive display duration in seconds
     * @throws NullPointerException if {@code region} is {@code null}
     * @throws IllegalArgumentException if {@code durationSeconds} is not finite and positive
     */
    public SpriteFrame {
        Objects.requireNonNull(region, "region");
        durationSeconds = Preconditions.requirePositive(durationSeconds, "durationSeconds");
    }
}
