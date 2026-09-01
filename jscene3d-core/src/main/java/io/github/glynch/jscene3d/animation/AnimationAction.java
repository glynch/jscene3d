/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/**
 * Mutable playback state for one clip owned by an {@link AnimationMixer}.
 *
 * <p>Actions are not thread-safe. Configuration and mixer updates must remain on the caller's
 * scene thread. Calling {@link #play()}, {@link #setTime(float)}, {@link #reset()}, or {@link
 * #stop()} immediately applies the resulting local time to the clip's targets.
 */
public final class AnimationAction {
    private final AnimationClip clip;

    private LoopMode loopMode = LoopMode.REPEAT;
    private float time;
    private float phase;
    private float timeScale = 1.0f;
    private boolean running;
    private boolean paused;

    /** Retains one immutable clip for mixer-owned playback. */
    AnimationAction(AnimationClip clip) {
        this.clip = Objects.requireNonNull(clip, "clip");
    }

    /**
     * Returns the retained clip.
     *
     * @return retained clip
     */
    public AnimationClip clip() {
        return clip;
    }

    /**
     * Starts or resumes playback and applies the current time immediately.
     *
     * @return this action
     */
    public AnimationAction play() {
        running = true;
        paused = false;
        clip.apply(time);
        return this;
    }

    /**
     * Pauses an action without changing its current time.
     *
     * @return this action
     */
    public AnimationAction pause() {
        paused = true;
        return this;
    }

    /**
     * Stops playback, returns to time zero, and applies the initial pose.
     *
     * @return this action
     */
    public AnimationAction stop() {
        running = false;
        paused = false;
        time = 0.0f;
        phase = 0.0f;
        clip.apply(time);
        return this;
    }

    /**
     * Returns to time zero without changing the running or paused state.
     *
     * @return this action
     */
    public AnimationAction reset() {
        time = 0.0f;
        phase = 0.0f;
        clip.apply(time);
        return this;
    }

    /**
     * Sets and applies an exact local clip time.
     *
     * @param time time in the inclusive range from zero through the clip duration
     * @return this action
     * @throws IllegalArgumentException if {@code time} is non-finite or outside the clip
     */
    public AnimationAction setTime(float time) {
        this.time = Preconditions.requireInRange(time, 0.0f, clip.duration(), "time");
        phase = this.time;
        clip.apply(this.time);
        return this;
    }

    /**
     * Sets the playback-rate multiplier; negative values play backward and zero freezes time.
     *
     * @param timeScale finite playback-rate multiplier
     * @return this action
     * @throws IllegalArgumentException if {@code timeScale} is not finite
     */
    public AnimationAction setTimeScale(float timeScale) {
        this.timeScale = Preconditions.requireFinite(timeScale, "timeScale");
        return this;
    }

    /**
     * Selects behavior at the clip endpoints.
     *
     * @param loopMode loop behavior
     * @return this action
     * @throws NullPointerException if {@code loopMode} is {@code null}
     */
    public AnimationAction setLoopMode(LoopMode loopMode) {
        this.loopMode = Objects.requireNonNull(loopMode, "loopMode");
        phase = time;
        return this;
    }

    /**
     * Returns the current local clip time in seconds.
     *
     * @return time in the inclusive clip interval
     */
    public float time() {
        return time;
    }

    /**
     * Returns the playback-rate multiplier.
     *
     * @return finite time scale
     */
    public float timeScale() {
        return timeScale;
    }

    /**
     * Returns the configured endpoint behavior.
     *
     * @return loop mode
     */
    public LoopMode loopMode() {
        return loopMode;
    }

    /**
     * Returns whether this action participates in mixer updates.
     *
     * @return {@code true} after play and before stop or one-shot completion
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns whether playback is explicitly paused.
     *
     * @return pause state
     */
    public boolean isPaused() {
        return paused;
    }

    /** Advances and applies this action for one non-negative mixer interval. */
    void advance(float elapsedSeconds) {
        if (!running || paused || timeScale == 0.0f) {
            return;
        }
        float change = elapsedSeconds * timeScale;
        if (!Float.isFinite(change)) {
            throw new IllegalArgumentException("elapsedSeconds * timeScale must be finite: " + change);
        }
        float duration = clip.duration();
        if (duration == 0.0f) {
            clip.apply(0.0f);
            if (loopMode == LoopMode.ONCE) {
                running = false;
            }
            return;
        }
        phase += change;
        time = switch (loopMode) {
            case ONCE -> advanceOnce(duration);
            case REPEAT -> wrap(phase, duration);
            case PING_PONG -> pingPong(phase, duration);
        };
        clip.apply(time);
    }

    /** Clamps one-shot playback and marks the reached endpoint as complete. */
    private float advanceOnce(float duration) {
        if (phase >= duration) {
            running = false;
            phase = duration;
            return duration;
        }
        if (phase <= 0.0f) {
            running = false;
            phase = 0.0f;
            return 0.0f;
        }
        return phase;
    }

    /** Wraps a possibly negative value into a half-open positive interval. */
    private static float wrap(float value, float period) {
        return value - (float) Math.floor(value / period) * period;
    }

    /** Reflects alternate repeat cycles into the clip's forward interval. */
    private static float pingPong(float value, float duration) {
        float cycle = wrap(value, duration * 2.0f);
        return cycle <= duration ? cycle : duration * 2.0f - cycle;
    }
}
