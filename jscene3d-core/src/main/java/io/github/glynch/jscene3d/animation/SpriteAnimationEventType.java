/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Observable change produced by one animated billboard. */
public enum SpriteAnimationEventType {
    /** The selected named animation changed. */
    ANIMATION_CHANGED,
    /** The displayed frame index changed. */
    FRAME_CHANGED,
    /** Repeating or ping-pong playback crossed an endpoint. */
    LOOPED,
    /** One-shot playback reached its terminal endpoint. */
    FINISHED
}
