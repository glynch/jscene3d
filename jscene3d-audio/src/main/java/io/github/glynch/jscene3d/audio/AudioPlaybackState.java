/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

/** Observable OpenAL playback state of one audio source. */
public enum AudioPlaybackState {
    /** Source has not started since creation or rewind. */
    INITIAL,

    /** Source is currently producing audio. */
    PLAYING,

    /** Source is paused and can resume from its current position. */
    PAUSED,

    /** Source reached its end or was explicitly stopped. */
    STOPPED
}
