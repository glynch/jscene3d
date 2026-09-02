/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/**
 * Immutable result of sampling one sprite animation at an exact local time.
 *
 * @param frame selected frame
 * @param frameIndex selected frame index
 * @param frameProgress within-frame progress
 */
public record SpriteAnimationSample(SpriteFrame frame, int frameIndex, float frameProgress) {
    /**
     * Creates one validated sample.
     *
     * @param frame selected frame
     * @param frameIndex non-negative selected frame index
     * @param frameProgress finite progress from zero through one within the selected frame
     * @throws NullPointerException if {@code frame} is {@code null}
     * @throws IllegalArgumentException if an index or progress value is invalid
     */
    public SpriteAnimationSample {
        Objects.requireNonNull(frame, "frame");
        frameIndex = Preconditions.requireNonNegative(frameIndex, "frameIndex");
        frameProgress = Preconditions.requireInRange(frameProgress, 0.0f, 1.0f, "frameProgress");
    }
}
