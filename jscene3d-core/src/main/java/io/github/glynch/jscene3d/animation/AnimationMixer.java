/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caller-driven playback owner for animation actions.
 *
 * <p>A mixer creates at most one action per clip identity. It does not create threads or read a
 * clock; the caller advances it explicitly with elapsed seconds on the same scene thread used for
 * the bound objects. Concurrent actions must not target the same property until weighted blending
 * support is introduced.
 */
public final class AnimationMixer {
    private final Map<AnimationClip, AnimationAction> actionsByClip = new IdentityHashMap<>();
    private final List<AnimationAction> actions = new ArrayList<>();

    /** Creates an empty mixer with no actions. */
    public AnimationMixer() {
        // The empty action collection is the complete initial mixer state.
    }

    /**
     * Returns the stable action owned for one clip, creating it when first requested.
     *
     * @param clip clip to control
     * @return stable mixer-owned action
     * @throws NullPointerException if {@code clip} is {@code null}
     */
    public AnimationAction action(AnimationClip clip) {
        AnimationClip validClip = Objects.requireNonNull(clip, "clip");
        AnimationAction existing = actionsByClip.get(validClip);
        if (existing != null) {
            return existing;
        }
        AnimationAction action = new AnimationAction(validClip);
        actionsByClip.put(validClip, action);
        actions.add(action);
        return action;
    }

    /**
     * Advances every running action by one elapsed interval.
     *
     * @param elapsedSeconds finite non-negative elapsed time in seconds
     * @throws IllegalArgumentException if {@code elapsedSeconds} is negative or non-finite
     */
    public void update(float elapsedSeconds) {
        float validElapsed = Preconditions.requireNonNegative(elapsedSeconds, "elapsedSeconds");
        for (AnimationAction action : actions) {
            action.advance(validElapsed);
        }
    }

    /** Stops every known action and reapplies each clip's initial pose. */
    public void stopAll() {
        for (AnimationAction action : actions) {
            action.stop();
        }
    }
}
