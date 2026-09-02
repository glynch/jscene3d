/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples;

import io.github.glynch.jscene3d.audio.AudioPlaybackState;
import io.github.glynch.jscene3d.audio.AudioSource;
import io.github.glynch.jscene3d.gui.AudioPlayerBinding;
import java.time.Duration;
import java.util.Objects;

/** Adapts one OpenAL-backed source to the backend-independent GUI audio-player contract. */
final class AudioSourcePlayerBinding implements AudioPlayerBinding {
    private final AudioSource source;

    private float volume;
    private boolean muted;

    /** Retains the source and its initial source-local gain. */
    AudioSourcePlayerBinding(AudioSource source) {
        this.source = Objects.requireNonNull(source, "source");
        volume = source.gain();
    }

    @Override
    public boolean isPlaying() {
        return source.state() == AudioPlaybackState.PLAYING;
    }

    @Override
    public Duration position() {
        return source.position();
    }

    @Override
    public Duration duration() {
        return source.duration();
    }

    @Override
    public void play() {
        source.play();
    }

    @Override
    public void pause() {
        source.pause();
    }

    @Override
    public void seek(Duration position) {
        source.seek(position);
    }

    @Override
    public float volume() {
        return volume;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        applyGain();
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        applyGain();
    }

    /** Applies mute without discarding the user-selected volume. */
    private void applyGain() {
        source.setGain(muted ? 0.0F : volume);
    }
}
