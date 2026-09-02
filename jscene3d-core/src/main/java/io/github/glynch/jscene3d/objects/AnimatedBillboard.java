/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.animation.SpriteAnimation;
import io.github.glynch.jscene3d.animation.SpriteAnimationEvent;
import io.github.glynch.jscene3d.animation.SpriteAnimationEventType;
import io.github.glynch.jscene3d.animation.SpriteAnimationSample;
import io.github.glynch.jscene3d.animation.SpriteAnimationSet;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A billboard with explicit, caller-driven playback of named sprite-atlas animations.
 *
 * <p>The immutable animation set and material remain caller-owned and may be shared. Playback state
 * belongs to this billboard, allowing many billboards to use one atlas while advancing
 * independently. No thread or hidden clock is created; applications call {@link #update(float)}
 * from their chosen update loop.
 */
public final class AnimatedBillboard extends Billboard {
    private final List<Consumer<? super SpriteAnimationEvent>> listeners = new ArrayList<>();

    private SpriteAnimationSet animationSet;
    private SpriteAnimation animation;
    private float time;
    private float phase;
    private float playbackSpeed = 1.0f;
    private int frameIndex;
    private float frameProgress;
    private boolean running;
    private boolean paused;

    /**
     * Creates a stopped billboard showing the first frame of the first declared animation.
     *
     * @param material open shared material, normally using the animation atlas as its color map
     * @param animationSet non-empty immutable animation set retained without taking ownership
     * @throws NullPointerException if {@code animationSet} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     */
    public AnimatedBillboard(BasicMaterial material, SpriteAnimationSet animationSet) {
        super(material);
        this.animationSet = Objects.requireNonNull(animationSet, "animationSet");
        animation = animationSet.animations().getFirst();
        applyTime(0.0f, false);
    }

    /**
     * Returns the shared immutable animation set.
     *
     * @return retained animation set
     */
    public SpriteAnimationSet animationSet() {
        geometry();
        return animationSet;
    }

    /**
     * Replaces the shared animation set, selects its first animation, and stops playback.
     *
     * @param animationSet replacement non-empty set
     * @throws NullPointerException if {@code animationSet} is {@code null}
     * @throws IllegalStateException if this billboard is closed
     */
    public void setAnimationSet(SpriteAnimationSet animationSet) {
        geometry();
        this.animationSet = Objects.requireNonNull(animationSet, "animationSet");
        animation = animationSet.animations().getFirst();
        resetPlayback();
        emit(SpriteAnimationEventType.ANIMATION_CHANGED);
    }

    /**
     * Returns the selected animation's stable name.
     *
     * @return selected animation name
     */
    public String animationName() {
        geometry();
        return animation.name();
    }

    /**
     * Selects an animation, resets it to its first frame, and stops playback.
     *
     * @param animationName required name from the retained set
     * @return this billboard
     * @throws NullPointerException if {@code animationName} is {@code null}
     * @throws IllegalArgumentException if the name is unknown
     * @throws IllegalStateException if this billboard is closed
     */
    public AnimatedBillboard setAnimation(String animationName) {
        geometry();
        SpriteAnimation replacement = animationSet.animation(animationName);
        if (animation != replacement) {
            animation = replacement;
            resetPlayback();
            emit(SpriteAnimationEventType.ANIMATION_CHANGED);
        }
        return this;
    }

    /**
     * Selects and starts or restarts one named animation.
     *
     * <p>Calling this method with the already selected paused animation resumes it without losing
     * frame progress.
     *
     * @param animationName required name from the retained set
     * @return this billboard
     */
    public AnimatedBillboard play(String animationName) {
        SpriteAnimation replacement = animationSet.animation(animationName);
        if (animation != replacement) {
            setAnimation(animationName);
        }
        return play();
    }

    /**
     * Starts or resumes the selected animation.
     *
     * @return this billboard
     * @throws IllegalStateException if this billboard is closed
     */
    public AnimatedBillboard play() {
        geometry();
        if (!running && terminalInPlaybackDirection()) {
            applyTime(playbackSpeed < 0.0f ? animation.duration() : 0.0f, true);
            phase = time;
        }
        running = true;
        paused = false;
        return this;
    }

    /**
     * Pauses time advancement while retaining the current frame and progress.
     *
     * @return this billboard
     */
    public AnimatedBillboard pause() {
        geometry();
        paused = true;
        return this;
    }

    /**
     * Stops playback and restores the selected animation's first frame.
     *
     * @return this billboard
     */
    public AnimatedBillboard stop() {
        geometry();
        resetPlayback();
        return this;
    }

    /**
     * Sets an exact frame and progress for editor scrubbing or deterministic seeking.
     *
     * @param frameIndex frame index in the selected animation
     * @param frameProgress finite progress from zero through one within that frame
     * @return this billboard
     * @throws IllegalArgumentException if an argument is outside its valid range
     */
    public AnimatedBillboard setFrameAndProgress(int frameIndex, float frameProgress) {
        geometry();
        int maximumIndex = animation.frames().size() - 1;
        int validIndex = Preconditions.requireInRange(frameIndex, 0, maximumIndex, "frameIndex");
        float validProgress = Preconditions.requireInRange(frameProgress, 0.0f, 1.0f, "frameProgress");
        float frameStart = frameStart(validIndex);
        float selectedTime = frameStart + animation.frames().get(validIndex).durationSeconds() * validProgress;
        applyTime(Math.clamp(selectedTime, 0.0f, animation.duration()), true);
        phase = time;
        return this;
    }

    /**
     * Changes the finite playback-rate multiplier; negative values play backward and zero freezes.
     *
     * @param playbackSpeed finite multiplier
     * @return this billboard
     * @throws IllegalArgumentException if the multiplier is not finite
     */
    public AnimatedBillboard setPlaybackSpeed(float playbackSpeed) {
        geometry();
        this.playbackSpeed = Preconditions.requireFinite(playbackSpeed, "playbackSpeed");
        return this;
    }

    /**
     * Returns the selected frame index.
     *
     * @return selected frame index
     */
    public int frameIndex() {
        geometry();
        return frameIndex;
    }

    /**
     * Returns progress from zero through one within the selected frame.
     *
     * @return within-frame progress
     */
    public float frameProgress() {
        geometry();
        return frameProgress;
    }

    /**
     * Returns the local animation time in seconds.
     *
     * @return local animation time
     */
    public float time() {
        geometry();
        return time;
    }

    /**
     * Returns the finite playback-rate multiplier.
     *
     * @return playback-rate multiplier
     */
    public float playbackSpeed() {
        geometry();
        return playbackSpeed;
    }

    /**
     * Returns whether the selected animation is scheduled to advance, including while paused.
     *
     * @return whether playback has been started
     */
    public boolean isRunning() {
        geometry();
        return running;
    }

    /**
     * Returns whether scheduled playback is explicitly paused.
     *
     * @return whether playback is paused
     */
    public boolean isPaused() {
        geometry();
        return paused;
    }

    /**
     * Adds one scene-thread listener for animation, frame, loop, and completion events.
     *
     * @param listener listener retained by identity
     * @return this billboard
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public AnimatedBillboard addAnimationListener(Consumer<? super SpriteAnimationEvent> listener) {
        geometry();
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Removes the first matching listener identity when present.
     *
     * @param listener listener identity to remove
     * @return whether a listener was removed
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public boolean removeAnimationListener(Consumer<? super SpriteAnimationEvent> listener) {
        geometry();
        Consumer<? super SpriteAnimationEvent> validListener = Objects.requireNonNull(listener, "listener");
        for (int index = 0; index < listeners.size(); index++) {
            if (listeners.get(index) == validListener) {
                listeners.remove(index);
                return true;
            }
        }
        return false;
    }

    /**
     * Advances playback by one caller-owned interval and applies the resulting atlas region.
     *
     * @param elapsedSeconds finite non-negative interval in seconds
     * @throws IllegalArgumentException if the interval or scaled interval is invalid
     * @throws IllegalStateException if this billboard is closed
     */
    public void update(float elapsedSeconds) {
        geometry();
        float validElapsed = Preconditions.requireNonNegative(elapsedSeconds, "elapsedSeconds");
        if (!running || paused || validElapsed == 0.0f || playbackSpeed == 0.0f) {
            return;
        }
        float change = validElapsed * playbackSpeed;
        if (!Float.isFinite(change)) {
            throw new IllegalArgumentException("elapsedSeconds * playbackSpeed must be finite: " + change);
        }
        AdvanceResult result = advance(change);
        applyTime(result.time(), true);
        emitEndpoint(result.endpoint());
    }

    /** Resets playback state and applies the first frame without changing the selected animation. */
    private void resetPlayback() {
        running = false;
        paused = false;
        phase = 0.0f;
        applyTime(0.0f, true);
    }

    /** Resolves one scaled interval according to the selected animation's endpoint behavior. */
    private AdvanceResult advance(float change) {
        return switch (animation.loopMode()) {
            case ONCE -> advanceOnce(change);
            case REPEAT -> advanceRepeat(change);
            case PING_PONG -> advancePingPong(change);
        };
    }

    /** Clamps one-shot playback and reports terminal completion. */
    private AdvanceResult advanceOnce(float change) {
        float rawTime = time + change;
        float resolvedTime = Math.clamp(rawTime, 0.0f, animation.duration());
        boolean finished = change > 0.0f ? rawTime >= animation.duration() : rawTime <= 0.0f;
        phase = resolvedTime;
        return new AdvanceResult(resolvedTime, finished ? Endpoint.FINISHED : Endpoint.NONE);
    }

    /** Wraps repeated playback while retaining a bounded phase. */
    private AdvanceResult advanceRepeat(float change) {
        float duration = animation.duration();
        float rawPhase = phase + change;
        boolean looped = rawPhase < 0.0f || rawPhase >= duration;
        phase = wrap(rawPhase, duration);
        return new AdvanceResult(phase, looped ? Endpoint.LOOPED : Endpoint.NONE);
    }

    /** Reflects ping-pong playback across each endpoint while retaining a bounded phase. */
    private AdvanceResult advancePingPong(float change) {
        float duration = animation.duration();
        float rawPhase = phase + change;
        boolean looped = Math.floor(phase / duration) != Math.floor(rawPhase / duration);
        phase = wrap(rawPhase, duration * 2.0f);
        float resolvedTime = phase <= duration ? phase : duration * 2.0f - phase;
        return new AdvanceResult(resolvedTime, looped ? Endpoint.LOOPED : Endpoint.NONE);
    }

    /** Applies one resolved time and notifies listeners only when its frame changes. */
    private void applyTime(float resolvedTime, boolean notify) {
        int previousFrame = frameIndex;
        SpriteAnimationSample sample = animation.sample(resolvedTime);
        time = resolvedTime;
        frameIndex = sample.frameIndex();
        frameProgress = sample.frameProgress();
        setTextureRegion(sample.frame().region());
        if (notify && frameIndex != previousFrame) {
            emit(SpriteAnimationEventType.FRAME_CHANGED);
        }
    }

    /** Returns the selected frame's local start time. */
    private float frameStart(int selectedFrame) {
        float result = 0.0f;
        for (int index = 0; index < selectedFrame; index++) {
            result += animation.frames().get(index).durationSeconds();
        }
        return result;
    }

    /** Returns whether stopped playback currently rests at its directional terminal endpoint. */
    private boolean terminalInPlaybackDirection() {
        return playbackSpeed < 0.0f ? time <= 0.0f : time >= animation.duration();
    }

    /** Emits a loop or finish event and stops completed one-shot playback. */
    private void emitEndpoint(Endpoint endpoint) {
        if (endpoint == Endpoint.LOOPED) {
            emit(SpriteAnimationEventType.LOOPED);
        } else if (endpoint == Endpoint.FINISHED) {
            running = false;
            paused = false;
            emit(SpriteAnimationEventType.FINISHED);
        }
    }

    /** Notifies a stable listener snapshot so callbacks may safely add or remove listeners. */
    private void emit(SpriteAnimationEventType type) {
        SpriteAnimationEvent event = new SpriteAnimationEvent(this, type, animation.name(), frameIndex);
        for (Consumer<? super SpriteAnimationEvent> listener : List.copyOf(listeners)) {
            listener.accept(event);
        }
    }

    /** Wraps a finite value into the half-open interval from zero through {@code length}. */
    private static float wrap(float value, float length) {
        float wrapped = value % length;
        return wrapped < 0.0f ? wrapped + length : wrapped;
    }

    /** Internal endpoint transition produced by one update. */
    private enum Endpoint {
        NONE,
        LOOPED,
        FINISHED
    }

    /** One resolved local time and its optional endpoint transition. */
    private record AdvanceResult(float time, Endpoint endpoint) {}
}
