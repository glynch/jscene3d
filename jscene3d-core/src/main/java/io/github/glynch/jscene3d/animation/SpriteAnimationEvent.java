/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.AnimatedBillboard;
import java.util.Objects;

/**
 * Immutable notification identifying an animated billboard's current playback state.
 *
 * @param source billboard producing the event
 * @param type event classification
 * @param animationName selected animation name
 * @param frameIndex selected frame index
 */
public record SpriteAnimationEvent(
        AnimatedBillboard source, SpriteAnimationEventType type, String animationName, int frameIndex) {
    /**
     * Creates a playback notification.
     *
     * @param source billboard producing the event
     * @param type event classification
     * @param animationName selected animation name
     * @param frameIndex selected frame index
     * @throws NullPointerException if a reference is {@code null}
     * @throws IllegalArgumentException if {@code frameIndex} is negative
     */
    public SpriteAnimationEvent {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(animationName, "animationName");
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must not be negative: " + frameIndex);
        }
    }
}
