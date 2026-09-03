/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Exercises the public audio interface against OpenAL Soft's null playback driver. */
final class AudioEngineTest {
    private static final String FIXTURE = "/io/github/glynch/jscene3d/audio/confirmation_001.ogg";

    /** Uploads in-memory signed 16-bit PCM without an encoded resource intermediary. */
    @Test
    void uploadsInMemoryPcm() {
        short[] samples = new short[800];
        try (AudioEngine engine = AudioEngine.create()) {
            AudioClip clip = engine.createClip(PcmAudio.mono16(8_000, samples));

            assertThat(clip.channels()).isOne();
            assertThat(clip.sampleRate()).isEqualTo(8_000);
            assertThat(clip.duration()).isEqualTo(Duration.ofMillis(100));

            clip.close();
        }
    }

    /** Loads a clip and drives source, category, listener, and cleanup behavior. */
    @Test
    void controlsBufferedPlaybackAndSpatialState() {
        try (AudioEngine engine = AudioEngine.create()) {
            AudioClip clip = engine.loadClip(AudioEngineTest.class, FIXTURE);
            assertThat(clip.channels()).isOne();
            assertThat(clip.sampleRate()).isEqualTo(44_100);
            assertThat(clip.duration()).isBetween(Duration.ofMillis(280), Duration.ofMillis(300));
            assertThat(clip.isClosed()).isFalse();

            AudioSource source = engine.createSource(clip, AudioCategory.EFFECTS);
            assertThat(source.category()).isEqualTo(AudioCategory.EFFECTS);
            assertThat(source.gain()).isOne();
            assertThat(source.duration()).isEqualTo(clip.duration());
            assertThat(source.state()).isEqualTo(AudioPlaybackState.INITIAL);

            source.seek(Duration.ofMillis(100));
            assertThat(source.position()).isBetween(Duration.ofMillis(95), Duration.ofMillis(105));
            source.seek(source.duration());
            assertThat(source.position()).isEqualTo(source.duration());
            source.rewind();

            source.setRelative(false);
            source.setPosition(new Vector3f(2.0F, 1.0F, -3.0F));
            source.setVelocity(new Vector3f(0.5F, 0.0F, 0.0F));
            source.setAttenuation(1.0F, 20.0F, 1.0F);
            source.setPitch(1.1F);
            source.setGain(0.8F);
            source.setLooping(true);
            assertThat(source.isLooping()).isTrue();

            engine.listener()
                    .setTransform(
                            new Vector3f(0.0F, 1.0F, 5.0F),
                            new Vector3f(0.0F, 0.0F, -1.0F),
                            new Vector3f(0.0F, 1.0F, 0.0F));
            engine.listener().setVelocity(new Vector3f());
            engine.setMasterGain(0.7F);
            engine.setCategoryGain(AudioCategory.EFFECTS, 0.6F);
            assertThat(engine.masterGain()).isEqualTo(0.7F);
            assertThat(engine.categoryGain(AudioCategory.EFFECTS)).isEqualTo(0.6F);

            source.play();
            assertThat(source.state()).isEqualTo(AudioPlaybackState.PLAYING);
            source.pause();
            assertThat(source.state()).isEqualTo(AudioPlaybackState.PAUSED);
            source.play();
            source.stop();
            assertThat(source.state()).isEqualTo(AudioPlaybackState.STOPPED);
            source.rewind();
            assertThat(source.state()).isEqualTo(AudioPlaybackState.INITIAL);
            assertThat(source.position()).isLessThan(Duration.ofMillis(5));

            assertThatThrownBy(clip::close)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("source");
            source.close();
            source.close();
            assertThat(source.isClosed()).isTrue();
            clip.close();
            clip.close();
            assertThat(clip.isClosed()).isTrue();
        }
    }

    /** Releases child handles when only the engine itself is explicitly closed. */
    @Test
    void closesOwnedResourcesAndRejectsFurtherUse() {
        AudioEngine engine = AudioEngine.create();
        AudioClip clip = engine.loadClip(AudioEngineTest.class, FIXTURE);
        AudioSource source = engine.createSource(clip, AudioCategory.MUSIC);

        engine.close();
        engine.close();

        assertThat(engine.isClosed()).isTrue();
        assertThat(source.isClosed()).isTrue();
        assertThat(clip.isClosed()).isTrue();
        assertThatThrownBy(engine::masterGain).isInstanceOf(IllegalStateException.class);
    }

    /** Reports missing classpath audio using its requested resource name. */
    @Test
    void rejectsMissingResources() {
        try (AudioEngine engine = AudioEngine.create()) {
            Class<?> anchor = AudioEngineTest.class;
            String missingResource = "/missing.ogg";

            assertThatThrownBy(() -> engine.loadClip(anchor, missingResource))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(missingResource);
        }
    }

    /** Rejects invalid source and listener values before changing native state. */
    @Test
    void rejectsInvalidPlaybackValues() {
        try (AudioEngine engine = AudioEngine.create()) {
            AudioClip clip = engine.loadClip(AudioEngineTest.class, FIXTURE);
            AudioSource source = engine.createSource(clip, AudioCategory.EFFECTS);
            Vector3f forward = new Vector3f(0.0F, 0.0F, -1.0F);
            Vector3f position = new Vector3f();
            AudioListener listener = engine.listener();
            Duration negativePosition = Duration.ofMillis(-1);
            Duration positionAfterEnd = clip.duration().plusNanos(1L);

            assertThatThrownBy(() -> source.setAttenuation(2.0F, 1.0F, 1.0F))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> source.setGain(1.1F)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> source.seek(negativePosition)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> source.seek(positionAfterEnd)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> listener.setTransform(position, forward, forward))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parallel");

            source.close();
            clip.close();
        }
    }
}
