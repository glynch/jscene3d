/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies immutable PCM value behavior without initializing native audio. */
final class PcmAudioTest {
    /** Preserves mono format metadata and isolates both sides of its sample storage. */
    @Test
    void createsImmutableMonoAudio() {
        short[] source = {Short.MIN_VALUE, 0, Short.MAX_VALUE};
        PcmAudio audio = PcmAudio.mono16(11_025, source);
        source[0] = 7;

        short[] returned = audio.samples();
        returned[1] = 7;

        assertThat(audio.channels()).isOne();
        assertThat(audio.sampleRate()).isEqualTo(11_025);
        assertThat(audio.frameCount()).isEqualTo(3);
        assertThat(audio.samples()).containsExactly(Short.MIN_VALUE, 0, Short.MAX_VALUE);
    }

    /** Uses sample content in equality, hashing, and diagnostics. */
    @Test
    void comparesPcmByValue() {
        PcmAudio first = PcmAudio.stereo16(8_000, new short[] {1, 2, 3, 4});
        PcmAudio same = PcmAudio.stereo16(8_000, new short[] {1, 2, 3, 4});
        PcmAudio different = PcmAudio.stereo16(8_000, new short[] {1, 2, 3, 5});

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(different);
        assertThat(first.toString()).contains("samples=[1, 2, 3, 4]");
    }

    /** Rejects malformed or unusable PCM before native upload. */
    @Test
    void rejectsInvalidPcm() {
        short[] noSamples = {};
        short[] incompleteStereoFrame = {1};
        short[] oneSample = {1};

        assertThatThrownBy(() -> PcmAudio.mono16(0, oneSample)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PcmAudio.mono16(8_000, noSamples)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PcmAudio.stereo16(8_000, incompleteStereoFrame))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
