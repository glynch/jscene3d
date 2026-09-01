/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/**
 * Mutable playback and influence state for one clip owned by an {@link AnimationMixer}.
 *
 * <p>Actions are not thread-safe. Configuration and mixer updates must remain on the caller's
 * scene thread. Playback, seeking, weight, and fade changes are resolved immediately through the
 * owning mixer so concurrent actions always produce one coherent pose.
 */
public final class AnimationAction {
    private final AnimationMixer mixer;
    private final AnimationClip clip;

    private LoopMode loopMode = LoopMode.REPEAT;
    private float time;
    private float phase;
    private float timeScale = 1.0f;
    private float weight = 1.0f;
    private float fadeWeight = 1.0f;
    private float fadeStart;
    private float fadeEnd;
    private float fadeDuration;
    private float fadeElapsed;
    private boolean running;
    private boolean paused;
    private boolean holdingPose;
    private boolean fading;
    private boolean deactivateAfterFade;

    /** Retains one immutable clip and its playback owner. */
    AnimationAction(AnimationMixer mixer, AnimationClip clip) {
        this.mixer = Objects.requireNonNull(mixer, "mixer");
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
     * Starts or resumes playback and resolves the current blended pose immediately.
     *
     * @return this action
     */
    public AnimationAction play() {
        running = true;
        paused = false;
        holdingPose = true;
        mixer.evaluate();
        return this;
    }

    /**
     * Pauses time advancement without removing this action's current pose contribution.
     *
     * @return this action
     */
    public AnimationAction pause() {
        paused = true;
        return this;
    }

    /**
     * Stops playback, returns to time zero, and retains the clip's initial weighted pose.
     *
     * @return this action
     */
    public AnimationAction stop() {
        stopAtInitialPose();
        mixer.evaluate();
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
        holdingPose = true;
        mixer.evaluate();
        return this;
    }

    /**
     * Sets and resolves an exact local clip time.
     *
     * @param time time in the inclusive range from zero through the clip duration
     * @return this action
     * @throws IllegalArgumentException if {@code time} is non-finite or outside the clip
     */
    public AnimationAction setTime(float time) {
        this.time = Preconditions.requireInRange(time, 0.0f, clip.duration(), "time");
        phase = this.time;
        holdingPose = true;
        mixer.evaluate();
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
     * Sets this action's contribution weight and immediately resolves the blended pose.
     *
     * <p>Weights below one leave the remaining influence to the captured base pose. When action
     * weights targeting one property total more than one, the mixer normalizes their contributions.
     *
     * @param weight finite inclusive value from zero through one
     * @return this action
     * @throws IllegalArgumentException if {@code weight} is non-finite or outside the range
     */
    public AnimationAction setWeight(float weight) {
        this.weight = Preconditions.requireInRange(weight, 0.0f, 1.0f, "weight");
        mixer.evaluate();
        return this;
    }

    /**
     * Schedules a linear influence fade from zero to the configured weight.
     *
     * <p>The fade advances only while this action retains a pose, normally after {@link #play()}.
     *
     * @param durationSeconds finite non-negative fade duration
     * @return this action
     * @throws IllegalArgumentException if the duration is negative or non-finite
     */
    public AnimationAction fadeIn(float durationSeconds) {
        startFade(0.0f, 1.0f, durationSeconds, false);
        mixer.evaluate();
        return this;
    }

    /**
     * Schedules a linear fade from the current influence to zero, then deactivates this action.
     *
     * @param durationSeconds finite non-negative fade duration
     * @return this action
     * @throws IllegalArgumentException if the duration is negative or non-finite
     */
    public AnimationAction fadeOut(float durationSeconds) {
        startFade(fadeWeight, 0.0f, durationSeconds, true);
        mixer.evaluate();
        return this;
    }

    /**
     * Cancels a scheduled fade at its current influence.
     *
     * @return this action
     */
    public AnimationAction stopFading() {
        fading = false;
        deactivateAfterFade = false;
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
     * Returns the configured contribution weight before fade influence is applied.
     *
     * @return value from zero through one
     */
    public float weight() {
        return weight;
    }

    /**
     * Returns this action's current effective contribution.
     *
     * @return configured weight multiplied by fade influence, or zero when inactive
     */
    public float effectiveWeight() {
        return holdingPose ? weight * fadeWeight : 0.0f;
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
     * Returns whether clip time participates in mixer updates.
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

    /** Returns whether this action belongs to the supplied mixer identity. */
    boolean belongsTo(AnimationMixer candidate) {
        return mixer == candidate;
    }

    /** Returns whether this action currently contributes a sampled pose. */
    boolean contributes() {
        return holdingPose && effectiveWeight() > 0.0f;
    }

    /** Advances fade influence and local clip time for one validated mixer interval. */
    void advance(float elapsedSeconds) {
        if (holdingPose) {
            advanceFade(elapsedSeconds);
        }
        if (!running || paused || timeScale == 0.0f) {
            return;
        }
        float change = elapsedSeconds * timeScale;
        if (!Float.isFinite(change)) {
            throw new IllegalArgumentException("elapsedSeconds * timeScale must be finite: " + change);
        }
        float duration = clip.duration();
        if (duration == 0.0f) {
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
    }

    /** Prepares this action as the reset, playing destination of a cross-fade. */
    void beginCrossFadeIn(float durationSeconds) {
        time = 0.0f;
        phase = 0.0f;
        running = true;
        paused = false;
        holdingPose = true;
        startFade(0.0f, 1.0f, durationSeconds, false);
    }

    /** Prepares this action as the source that deactivates when a cross-fade completes. */
    void beginCrossFadeOut(float durationSeconds) {
        startFade(fadeWeight, 0.0f, durationSeconds, true);
    }

    /** Stops without recursively requesting mixer evaluation. */
    void stopAtInitialPose() {
        running = false;
        paused = false;
        holdingPose = true;
        time = 0.0f;
        phase = 0.0f;
        fadeWeight = 1.0f;
        fading = false;
        deactivateAfterFade = false;
    }

    /** Schedules or immediately completes one validated fade. */
    private void startFade(float start, float end, float durationSeconds, boolean deactivate) {
        float validDuration = Preconditions.requireNonNegative(durationSeconds, "durationSeconds");
        fadeStart = start;
        fadeEnd = end;
        fadeDuration = validDuration;
        fadeElapsed = 0.0f;
        deactivateAfterFade = deactivate;
        if (validDuration == 0.0f) {
            fadeWeight = end;
            fading = false;
            completeFade();
        } else {
            fadeWeight = start;
            fading = true;
        }
    }

    /** Advances one scheduled fade independently from local clip time. */
    private void advanceFade(float elapsedSeconds) {
        if (!fading) {
            return;
        }
        fadeElapsed = Math.min(fadeElapsed + elapsedSeconds, fadeDuration);
        float progress = fadeElapsed / fadeDuration;
        fadeWeight = Math.fma(fadeEnd - fadeStart, progress, fadeStart);
        if (fadeElapsed == fadeDuration) {
            fading = false;
            completeFade();
        }
    }

    /** Deactivates a fully faded-out action when requested. */
    private void completeFade() {
        if (!deactivateAfterFade || fadeWeight != 0.0f) {
            return;
        }
        running = false;
        paused = false;
        holdingPose = false;
        deactivateAfterFade = false;
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
