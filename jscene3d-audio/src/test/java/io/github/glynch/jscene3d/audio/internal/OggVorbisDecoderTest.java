/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

/** Verifies complete in-memory Ogg Vorbis decoding and validation. */
final class OggVorbisDecoderTest {
    /** Decodes the bundled CC0 mono fixture to its original format. */
    @Test
    void decodesMonoVorbisAudio() throws IOException {
        byte[] encoded = resourceBytes();
        ByteBuffer buffer = MemoryUtil.memAlloc(encoded.length);
        try {
            buffer.put(encoded).flip();
            try (DecodedAudio decoded = OggVorbisDecoder.decode(buffer)) {
                assertThat(decoded.channels()).isOne();
                assertThat(decoded.sampleRate()).isEqualTo(44_100);
                assertThat(decoded.frameCount()).isGreaterThan(12_000);
                assertThat(decoded.samples().hasRemaining()).isTrue();
            }
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    /** Rejects heap buffers before invoking native decoding. */
    @Test
    void rejectsHeapBuffers() {
        ByteBuffer heapBuffer = ByteBuffer.wrap(new byte[] {1, 2, 3});

        assertThatThrownBy(() -> OggVorbisDecoder.decode(heapBuffer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direct");
    }

    /** Rejects direct bytes that are not an Ogg Vorbis stream. */
    @Test
    void rejectsInvalidEncodedAudio() {
        ByteBuffer invalid = MemoryUtil.memAlloc(4);
        try {
            invalid.putInt(0x12345678).flip();

            assertThatThrownBy(() -> OggVorbisDecoder.decode(invalid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ogg Vorbis");
        } finally {
            MemoryUtil.memFree(invalid);
        }
    }

    /** Reads the required published fixture bytes. */
    private static byte[] resourceBytes() throws IOException {
        try (InputStream input = OggVorbisDecoderTest.class.getResourceAsStream(
                "/io/github/glynch/jscene3d/audio/confirmation_001.ogg")) {
            if (input == null) {
                throw new IllegalStateException("Missing Ogg Vorbis test fixture");
            }
            return input.readAllBytes();
        }
    }
}
