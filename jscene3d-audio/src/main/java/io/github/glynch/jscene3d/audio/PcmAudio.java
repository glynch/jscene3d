/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import java.util.Arrays;
import java.util.Objects;

/** Immutable interleaved signed 16-bit PCM that can be uploaded to an {@link AudioEngine}. */
public final class PcmAudio {
    private final int channels;
    private final int sampleRate;
    private final short[] samples;

    /** Stores validated format metadata and an owned copy of the interleaved samples. */
    private PcmAudio(int channels, int sampleRate, short[] samples) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive");
        }
        short[] validSamples = Objects.requireNonNull(samples, "samples");
        if (validSamples.length == 0) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        if (validSamples.length % channels != 0) {
            throw new IllegalArgumentException("samples must contain complete interleaved frames");
        }
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.samples = validSamples.clone();
    }

    /**
     * Creates mono PCM from signed 16-bit samples.
     *
     * @param sampleRate sample frames per second
     * @param samples mono signed 16-bit samples
     * @return immutable mono PCM
     */
    public static PcmAudio mono16(int sampleRate, short[] samples) {
        return new PcmAudio(1, sampleRate, samples);
    }

    /**
     * Creates stereo PCM from interleaved left and right signed 16-bit samples.
     *
     * @param sampleRate sample frames per channel per second
     * @param samples interleaved left and right signed 16-bit samples
     * @return immutable stereo PCM
     */
    public static PcmAudio stereo16(int sampleRate, short[] samples) {
        return new PcmAudio(2, sampleRate, samples);
    }

    /**
     * Returns one for mono or two for stereo.
     *
     * @return channel count
     */
    public int channels() {
        return channels;
    }

    /**
     * Returns sample frames per channel per second.
     *
     * @return sample rate
     */
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of sample frames across all channels.
     *
     * @return frame count
     */
    public int frameCount() {
        return samples.length / channels;
    }

    /**
     * Returns a defensive copy of the interleaved signed 16-bit samples.
     *
     * @return copied samples
     */
    public short[] samples() {
        return samples.clone();
    }

    /** Compares format metadata and sample content. */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof PcmAudio other
                && channels == other.channels
                && sampleRate == other.sampleRate
                && Arrays.equals(samples, other.samples);
    }

    /** Hashes format metadata and sample content. */
    @Override
    public int hashCode() {
        int result = Objects.hash(channels, sampleRate);
        return 31 * result + Arrays.hashCode(samples);
    }

    /** Describes format metadata and sample content. */
    @Override
    public String toString() {
        return "PcmAudio[channels=" + channels + ", sampleRate=" + sampleRate + ", samples=" + Arrays.toString(samples)
                + ']';
    }
}
