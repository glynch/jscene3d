/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Playback behavior when an animation action reaches either end of its clip. */
public enum LoopMode {
    /** Stops at the reached endpoint. */
    ONCE,

    /** Wraps to the opposite endpoint and continues in the same direction. */
    REPEAT,

    /** Alternates forward and backward traversal on successive passes. */
    PING_PONG
}
