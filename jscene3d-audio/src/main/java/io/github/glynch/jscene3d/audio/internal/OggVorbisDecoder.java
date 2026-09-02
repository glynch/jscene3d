/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Decodes complete in-memory Ogg Vorbis files to interleaved 16-bit PCM. */
public final class OggVorbisDecoder {
    /** Prevents instantiation of this native decoding container. */
    private OggVorbisDecoder() {
        throw new AssertionError("OggVorbisDecoder cannot be instantiated");
    }

    /**
     * Decodes a direct encoded buffer and transfers ownership of returned PCM to the caller.
     *
     * @param encodedAudio direct buffer containing one complete Ogg Vorbis stream
     * @return owned native decoded PCM and format metadata
     */
    public static DecodedAudio decode(ByteBuffer encodedAudio) {
        ByteBuffer validAudio = Objects.requireNonNull(encodedAudio, "encodedAudio");
        if (!validAudio.isDirect()) {
            throw new IllegalArgumentException("encodedAudio must be a direct buffer");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer samples = STBVorbis.stb_vorbis_decode_memory(validAudio, channels, sampleRate);
            if (samples == null) {
                throw new IllegalArgumentException("Encoded audio is not a supported Ogg Vorbis stream");
            }
            int channelCount = channels.get(0);
            if (channelCount < 1 || channelCount > 2) {
                MemoryUtil.memFree(samples);
                throw new IllegalArgumentException("Only mono and stereo audio are supported: " + channelCount);
            }
            int decodedSampleRate = sampleRate.get(0);
            if (decodedSampleRate <= 0) {
                MemoryUtil.memFree(samples);
                throw new IllegalArgumentException("Decoded audio has an invalid sample rate: " + decodedSampleRate);
            }
            return new DecodedAudio(samples, channelCount, decodedSampleRate);
        }
    }
}
