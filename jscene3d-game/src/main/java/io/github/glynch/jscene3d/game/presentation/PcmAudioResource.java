/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.audio.PcmAudio;
import java.util.Objects;

/** Shared immutable-use signed 16-bit PCM runtime resource. */
public final class PcmAudioResource implements AutoCloseable {
    private final PcmAudio audio;
    private boolean closed;

    /** Takes ownership of one validated immutable PCM value. */
    private PcmAudioResource(PcmAudio audio) {
        this.audio = Objects.requireNonNull(audio, "audio");
    }

    /**
     * Creates a resource owning the supplied immutable PCM value.
     *
     * @param audio immutable PCM value
     * @return open resource
     */
    public static PcmAudioResource owning(PcmAudio audio) {
        return new PcmAudioResource(audio);
    }

    /**
     * Returns the immutable PCM value while this resource is open.
     *
     * @return PCM audio
     * @throws IllegalStateException after closure
     */
    public PcmAudio audio() {
        if (closed) {
            throw new IllegalStateException("PCM audio resource is closed");
        }
        return audio;
    }

    /**
     * Returns whether terminal resource closure has completed.
     *
     * @return {@code true} after closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Marks this immutable heap resource closed. */
    @Override
    public void close() {
        closed = true;
    }
}
