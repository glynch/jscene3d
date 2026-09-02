/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.internal;

import java.nio.ShortBuffer;
import org.lwjgl.system.MemoryUtil;

/** Owns one native decoded PCM allocation until it has been uploaded to OpenAL. */
public final class DecodedAudio implements AutoCloseable {
    private final ShortBuffer samples;
    private final int channels;
    private final int sampleRate;

    /** Stores decoded interleaved 16-bit PCM and its format metadata. */
    DecodedAudio(ShortBuffer samples, int channels, int sampleRate) {
        this.samples = samples;
        this.channels = channels;
        this.sampleRate = sampleRate;
    }

    /**
     * Returns the native interleaved 16-bit PCM view.
     *
     * @return native PCM sample buffer
     */
    public ShortBuffer samples() {
        return samples;
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
     * Returns samples per channel per second.
     *
     * @return decoded sample rate
     */
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of sample frames in the decoded audio.
     *
     * @return sample frames across all channels
     */
    public int frameCount() {
        return samples.remaining() / channels;
    }

    /** Frees the native PCM allocation returned by STB Vorbis. */
    @Override
    public void close() {
        MemoryUtil.memFree(samples);
    }
}
