/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable named sequence of timed sprite-atlas frames and endpoint behavior. */
public final class SpriteAnimation {
    private final String name;
    private final List<SpriteFrame> frames;
    private final float[] frameEndTimes;
    private final LoopMode loopMode;
    private final float duration;

    /**
     * Creates an animation with explicit per-frame durations.
     *
     * @param name non-blank animation name
     * @param frames one or more timed frames
     * @param loopMode endpoint behavior
     * @throws NullPointerException if an argument or frame is {@code null}
     * @throws IllegalArgumentException if the name is blank, the frame list is empty, or its total
     *     duration is not representable as a finite float
     */
    public SpriteAnimation(String name, List<SpriteFrame> frames, LoopMode loopMode) {
        this.name = Preconditions.requireNonBlank(name, "name");
        this.frames = List.copyOf(Objects.requireNonNull(frames, "frames"));
        if (this.frames.isEmpty()) {
            throw new IllegalArgumentException("frames must not be empty");
        }
        this.loopMode = Objects.requireNonNull(loopMode, "loopMode");
        frameEndTimes = new float[this.frames.size()];
        duration = resolveEndTimes();
    }

    /**
     * Creates uniformly timed frames from atlas regions and a frame rate.
     *
     * @param name non-blank animation name
     * @param regions one or more frame regions
     * @param framesPerSecond finite positive frame rate
     * @param loopMode endpoint behavior
     * @return immutable uniformly timed animation
     * @throws NullPointerException if an argument or region is {@code null}
     * @throws IllegalArgumentException if the name, regions, or frame rate is invalid
     */
    public static SpriteAnimation uniform(
            String name, List<TextureRegion> regions, float framesPerSecond, LoopMode loopMode) {
        float frameDuration = 1.0f / Preconditions.requirePositive(framesPerSecond, "framesPerSecond");
        List<TextureRegion> validRegions = List.copyOf(Objects.requireNonNull(regions, "regions"));
        List<SpriteFrame> frames = new ArrayList<>(validRegions.size());
        for (TextureRegion region : validRegions) {
            frames.add(new SpriteFrame(region, frameDuration));
        }
        return new SpriteAnimation(name, frames, loopMode);
    }

    /**
     * Returns the stable animation name.
     *
     * @return animation name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the immutable frames in playback order.
     *
     * @return ordered frames
     */
    public List<SpriteFrame> frames() {
        return frames;
    }

    /**
     * Returns endpoint behavior owned by this animation.
     *
     * @return endpoint behavior
     */
    public LoopMode loopMode() {
        return loopMode;
    }

    /**
     * Returns the finite positive total duration in seconds.
     *
     * @return duration in seconds
     */
    public float duration() {
        return duration;
    }

    /**
     * Samples the frame covering an exact local time.
     *
     * @param timeSeconds local time from zero through {@link #duration()}
     * @return immutable frame, index, and within-frame progress
     * @throws IllegalArgumentException if the time is non-finite or outside the animation
     */
    public SpriteAnimationSample sample(float timeSeconds) {
        float validTime = Preconditions.requireInRange(timeSeconds, 0.0f, duration, "timeSeconds");
        int frameIndex = frameIndex(validTime);
        float frameStart = frameIndex == 0 ? 0.0f : frameEndTimes[frameIndex - 1];
        float frameDuration = frames.get(frameIndex).durationSeconds();
        float progress = Math.clamp((validTime - frameStart) / frameDuration, 0.0f, 1.0f);
        return new SpriteAnimationSample(frames.get(frameIndex), frameIndex, progress);
    }

    /** Accumulates validated frame end times and returns the total duration. */
    private float resolveEndTimes() {
        float endTime = 0.0f;
        for (int index = 0; index < frames.size(); index++) {
            endTime += frames.get(index).durationSeconds();
            if (!Float.isFinite(endTime)) {
                throw new IllegalArgumentException("total frame duration must be finite");
            }
            frameEndTimes[index] = endTime;
        }
        return endTime;
    }

    /** Resolves the frame containing one already validated local time. */
    private int frameIndex(float timeSeconds) {
        for (int index = 0; index < frameEndTimes.length - 1; index++) {
            if (timeSeconds < frameEndTimes[index]) {
                return index;
            }
        }
        return frameEndTimes.length - 1;
    }
}
