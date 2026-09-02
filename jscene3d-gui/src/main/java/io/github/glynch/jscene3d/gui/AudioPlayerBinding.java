/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import java.time.Duration;

/**
 * Application-owned playback state and actions displayed by a control-panel audio player.
 *
 * <p>This interface deliberately describes media controls rather than a particular audio backend.
 * Implementations may adapt OpenAL, another audio library, or application-specific playback.
 */
public interface AudioPlayerBinding {
    /**
     * Returns whether playback is currently advancing.
     *
     * @return {@code true} when playback is advancing
     */
    boolean isPlaying();

    /**
     * Returns the current playback position.
     *
     * @return the current playback position
     */
    Duration position();

    /**
     * Returns the total media duration.
     *
     * @return the total media duration
     */
    Duration duration();

    /** Starts or resumes playback. */
    void play();

    /** Pauses playback at the current position. */
    void pause();

    /**
     * Moves playback to a position within the media duration.
     *
     * @param position the requested playback position
     */
    void seek(Duration position);

    /**
     * Returns the user-selected volume in the inclusive unit interval.
     *
     * @return the user-selected volume
     */
    float volume();

    /**
     * Replaces the user-selected volume with a value in the inclusive unit interval.
     *
     * @param volume the new volume
     */
    void setVolume(float volume);

    /**
     * Returns whether output is muted independently of the selected volume.
     *
     * @return {@code true} when output is muted
     */
    boolean isMuted();

    /**
     * Replaces the muted state without discarding the selected volume.
     *
     * @param muted whether output should be muted
     */
    void setMuted(boolean muted);
}
