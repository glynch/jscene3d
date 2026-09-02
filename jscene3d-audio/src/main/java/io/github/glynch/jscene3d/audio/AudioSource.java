/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import io.github.glynch.jscene3d.audio.internal.Preconditions;
import java.time.Duration;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

/**
 * Independently controlled playback of one {@link AudioClip}.
 *
 * <p>Sources are positional by default. Call {@link #setRelative(boolean)} with {@code true} for
 * music or interface effects that should remain fixed to the listener. All methods must be called
 * on the thread that created the owning {@link AudioEngine}.
 */
public final class AudioSource implements AutoCloseable {
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final AudioEngine engine;
    private final AudioClip clip;
    private final AudioCategory category;
    private final int sourceId;

    private float gain = 1.0F;
    private Duration requestedPosition = Duration.ZERO;
    private boolean requestedPositionPending;
    private boolean closed;

    /** Stores one engine-created OpenAL source and its immutable clip and volume group. */
    AudioSource(AudioEngine engine, AudioClip clip, AudioCategory category, int sourceId) {
        this.engine = engine;
        this.clip = clip;
        this.category = category;
        this.sourceId = sourceId;
    }

    /** Starts or resumes playback. A stopped source restarts from its beginning. */
    public void play() {
        engine.useSource(this, () -> AL10.alSourcePlay(sourceId), "start audio source");
        requestedPositionPending = false;
    }

    /** Pauses playback while retaining the current sample offset. */
    public void pause() {
        engine.useSource(this, () -> AL10.alSourcePause(sourceId), "pause audio source");
    }

    /** Stops playback and resets the next play operation to the beginning. */
    public void stop() {
        engine.useSource(this, () -> AL10.alSourceStop(sourceId), "stop audio source");
        requestedPositionPending = false;
    }

    /** Rewinds the source to its initial state without starting playback. */
    public void rewind() {
        engine.useSource(this, () -> AL10.alSourceRewind(sourceId), "rewind audio source");
        requestedPositionPending = false;
    }

    /**
     * Returns the duration of the immutable clip attached to this source.
     *
     * @return clip duration
     */
    public Duration duration() {
        requireOpen();
        return clip.duration();
    }

    /**
     * Returns the current playback position.
     *
     * @return current position between zero and {@link #duration()}
     */
    public Duration position() {
        requireOpen();
        if (requestedPositionPending) {
            return requestedPosition;
        }
        double seconds = engine.querySource(
                this, () -> AL10.alGetSourcef(sourceId, AL11.AL_SEC_OFFSET), "read source playback position");
        long positionNanos = Math.round(seconds * NANOS_PER_SECOND);
        return Duration.ofNanos(Math.clamp(positionNanos, 0L, duration().toNanos()));
    }

    /**
     * Moves playback to an exact position within the clip without changing playback state.
     *
     * @param position non-negative position no later than the clip duration
     */
    public void seek(Duration position) {
        Duration validPosition = Objects.requireNonNull(position, "position");
        if (validPosition.isNegative() || validPosition.compareTo(duration()) > 0) {
            throw new IllegalArgumentException("position must be within the clip duration");
        }
        if (validPosition.equals(duration())) {
            engine.useSource(this, () -> AL10.alSourceStop(sourceId), "seek source to its end");
            requestedPosition = validPosition;
            requestedPositionPending = true;
            return;
        }
        float seconds = (float) (validPosition.toNanos() / NANOS_PER_SECOND);
        engine.useSource(
                this, () -> AL10.alSourcef(sourceId, AL11.AL_SEC_OFFSET, seconds), "change source playback position");
        requestedPosition = validPosition;
        requestedPositionPending = true;
    }

    /**
     * Enables or disables continuous replay at the end of the clip.
     *
     * @param looping whether playback repeats
     */
    public void setLooping(boolean looping) {
        engine.useSource(
                this,
                () -> AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE),
                "change source looping");
    }

    /**
     * Returns whether continuous replay is enabled.
     *
     * @return whether playback repeats
     */
    public boolean isLooping() {
        return engine.querySource(this, () -> AL10.alGetSourcei(sourceId, AL10.AL_LOOPING), "read source looping")
                == AL10.AL_TRUE;
    }

    /**
     * Sets source gain before category and master volumes are applied.
     *
     * @param value finite gain in the inclusive unit interval, where one is the original clip
     *     amplitude
     */
    public void setGain(float value) {
        float validValue = Preconditions.requireUnitInterval(value, "value");
        engine.applyGain(this, validValue);
        gain = validValue;
    }

    /**
     * Returns source gain before category and master volumes are applied.
     *
     * @return source-local gain
     */
    public float gain() {
        return gain;
    }

    /**
     * Sets playback pitch, where one preserves the original speed and pitch.
     *
     * @param value positive finite pitch multiplier
     */
    public void setPitch(float value) {
        float validValue = Preconditions.requirePositive(value, "value");
        engine.useSource(this, () -> AL10.alSourcef(sourceId, AL10.AL_PITCH, validValue), "change source pitch");
    }

    /**
     * Sets the source position in listener-compatible world coordinates.
     *
     * @param value finite world position
     */
    public void setPosition(Vector3fc value) {
        Vector3f position = Preconditions.requireFinite(value, "value");
        engine.useSource(
                this,
                () -> AL10.alSource3f(sourceId, AL10.AL_POSITION, position.x, position.y, position.z),
                "change source position");
    }

    /**
     * Sets source velocity used by OpenAL Doppler calculations.
     *
     * @param value finite world-space velocity
     */
    public void setVelocity(Vector3fc value) {
        Vector3f velocity = Preconditions.requireFinite(value, "value");
        engine.useSource(
                this,
                () -> AL10.alSource3f(sourceId, AL10.AL_VELOCITY, velocity.x, velocity.y, velocity.z),
                "change source velocity");
    }

    /**
     * Selects listener-relative playback for music and interface sounds or world-relative playback
     * for positional effects.
     *
     * @param relative whether coordinates are relative to the listener
     */
    public void setRelative(boolean relative) {
        engine.useSource(
                this,
                () -> AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE),
                "change source relativity");
    }

    /**
     * Configures inverse-distance attenuation for a positional source.
     *
     * @param referenceDistance positive distance at which gain begins decreasing
     * @param maximumDistance positive distance at which attenuation is clamped
     * @param rolloffFactor non-negative attenuation rate
     */
    public void setAttenuation(float referenceDistance, float maximumDistance, float rolloffFactor) {
        float validReference = Preconditions.requirePositive(referenceDistance, "referenceDistance");
        float validMaximum = Preconditions.requirePositive(maximumDistance, "maximumDistance");
        float validRolloff = Preconditions.requireNonNegative(rolloffFactor, "rolloffFactor");
        if (validMaximum < validReference) {
            throw new IllegalArgumentException("maximumDistance must not be below referenceDistance");
        }
        engine.useSource(
                this, () -> applyAttenuation(validReference, validMaximum, validRolloff), "change source attenuation");
    }

    /**
     * Returns the source's current playback state.
     *
     * @return current playback state
     */
    public AudioPlaybackState state() {
        int state =
                engine.querySource(this, () -> AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE), "read source state");
        return switch (state) {
            case AL10.AL_INITIAL -> AudioPlaybackState.INITIAL;
            case AL10.AL_PLAYING -> AudioPlaybackState.PLAYING;
            case AL10.AL_PAUSED -> AudioPlaybackState.PAUSED;
            case AL10.AL_STOPPED -> AudioPlaybackState.STOPPED;
            default -> throw new IllegalStateException("Unknown OpenAL source state: " + state);
        };
    }

    /**
     * Returns the immutable volume group assigned at creation.
     *
     * @return assigned volume category
     */
    public AudioCategory category() {
        return category;
    }

    /**
     * Returns whether this source has released its native handle.
     *
     * @return whether the source is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Stops playback and releases the native source handle. */
    @Override
    public void close() {
        engine.destroySource(this);
    }

    /** Returns the associated clip for engine lifetime checks. */
    AudioClip clip() {
        return clip;
    }

    /** Returns the native source identifier after an ownership check. */
    int sourceId(AudioEngine expectedEngine) {
        requireOwnedBy(expectedEngine);
        requireOpen();
        return sourceId;
    }

    /** Rejects a source created by another engine. */
    void requireOwnedBy(AudioEngine expectedEngine) {
        if (engine != expectedEngine) {
            throw new IllegalArgumentException("source belongs to a different audio engine");
        }
    }

    /** Rejects operations after native source deletion. */
    void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Audio source is closed");
        }
    }

    /** Marks the handle closed after native source deletion. */
    void markClosed() {
        closed = true;
    }

    /** Applies source-local attenuation properties in the active OpenAL context. */
    private void applyAttenuation(float referenceDistance, float maximumDistance, float rolloffFactor) {
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, referenceDistance);
        AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, maximumDistance);
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, rolloffFactor);
    }
}
