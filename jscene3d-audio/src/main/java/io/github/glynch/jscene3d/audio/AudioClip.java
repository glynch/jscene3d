/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import java.time.Duration;

/**
 * Reusable decoded audio held in an OpenAL buffer owned by an {@link AudioEngine}.
 *
 * <p>Mono clips can be spatialized by an {@link AudioSource}; stereo clips are intended for
 * relative music and interface playback. Close every source that uses a clip before closing the
 * clip. Closing the engine releases all remaining clips automatically.
 */
public final class AudioClip implements AutoCloseable {
    private final AudioEngine engine;
    private final int bufferId;
    private final int channels;
    private final int sampleRate;
    private final Duration duration;

    private boolean closed;

    /** Stores one engine-owned native buffer and immutable format metadata. */
    AudioClip(AudioEngine engine, int bufferId, int channels, int sampleRate, Duration duration) {
        this.engine = engine;
        this.bufferId = bufferId;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.duration = duration;
    }

    /**
     * Returns one for mono or two for stereo.
     *
     * @return decoded channel count
     */
    public int channels() {
        return channels;
    }

    /**
     * Returns sample frames per channel per second.
     *
     * @return decoded sample rate
     */
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * Returns the decoded clip duration.
     *
     * @return complete buffered duration
     */
    public Duration duration() {
        return duration;
    }

    /**
     * Returns whether this clip has released its native buffer.
     *
     * @return whether the clip is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases the native buffer after all sources using it have been closed. */
    @Override
    public void close() {
        engine.destroyClip(this);
    }

    /** Returns the native buffer after validating engine ownership and lifetime. */
    int bufferId(AudioEngine expectedEngine) {
        requireOwnedBy(expectedEngine);
        requireOpen();
        return bufferId;
    }

    /** Returns whether this clip belongs to the supplied engine. */
    boolean belongsTo(AudioEngine expectedEngine) {
        return engine == expectedEngine;
    }

    /** Rejects a clip created by another OpenAL context. */
    void requireOwnedBy(AudioEngine expectedEngine) {
        if (!belongsTo(expectedEngine)) {
            throw new IllegalArgumentException("clip belongs to a different audio engine");
        }
    }

    /** Rejects operations after the native buffer has been released. */
    void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Audio clip is closed");
        }
    }

    /** Marks the handle closed after its native buffer has been deleted. */
    void markClosed() {
        closed = true;
    }
}
