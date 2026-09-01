/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caller-driven playback and weighted-pose owner for animation actions.
 *
 * <p>A mixer creates at most one action per clip identity. It does not create threads or read a
 * clock; the caller advances it explicitly with elapsed seconds on the same scene thread used for
 * the bound objects. Concurrent actions targeting the same property are accumulated and applied
 * once, independently of action registration order.
 */
public final class AnimationMixer {
    private final Map<AnimationClip, AnimationAction> actionsByClip = new IdentityHashMap<>();
    private final List<AnimationAction> actions = new ArrayList<>();
    private final Map<AnimationTrack, PropertyAccumulator> bindingsByTrack = new IdentityHashMap<>();
    private final Map<Object3D, EnumMap<TransformProperty, PropertyAccumulator>> bindingsByTarget =
            new IdentityHashMap<>();
    private final List<PropertyAccumulator> bindings = new ArrayList<>();
    private final float[] vectorSample = new float[3];
    private final float[] quaternionSample = new float[4];

    /** Creates an empty mixer with no actions or captured base poses. */
    public AnimationMixer() {
        // The empty action and binding collections are the complete initial mixer state.
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
        registerBindings(validClip);
        AnimationAction action = new AnimationAction(this, validClip);
        actionsByClip.put(validClip, action);
        actions.add(action);
        return action;
    }

    /**
     * Cross-fades from one owned action to another over a linear duration.
     *
     * <p>The destination is reset, started, and faded from zero. The source continues advancing
     * while fading out and deactivates after reaching zero influence.
     *
     * @param source currently contributing source action
     * @param destination destination action to reset and play
     * @param durationSeconds finite non-negative transition duration
     * @throws NullPointerException if an action is {@code null}
     * @throws IllegalArgumentException if the actions are identical, belong to another mixer, or
     *     the duration is negative or non-finite
     * @throws IllegalStateException if the source does not currently contribute a pose
     */
    public void crossFade(AnimationAction source, AnimationAction destination, float durationSeconds) {
        AnimationAction validSource = requireOwned(source, "source");
        AnimationAction validDestination = requireOwned(destination, "destination");
        if (validSource == validDestination) {
            throw new IllegalArgumentException("source and destination must be different actions");
        }
        if (!validSource.contributes()) {
            throw new IllegalStateException("source must currently contribute a pose");
        }
        float validDuration = Preconditions.requireNonNegative(durationSeconds, "durationSeconds");
        validSource.beginCrossFadeOut(validDuration);
        validDestination.beginCrossFadeIn(validDuration);
        evaluate();
    }

    /**
     * Advances every action followed by one blended property evaluation.
     *
     * @param elapsedSeconds finite non-negative elapsed time in seconds
     * @throws IllegalArgumentException if {@code elapsedSeconds} is negative or non-finite
     */
    public void update(float elapsedSeconds) {
        float validElapsed = Preconditions.requireNonNegative(elapsedSeconds, "elapsedSeconds");
        for (AnimationAction action : actions) {
            action.advance(validElapsed);
        }
        evaluate();
    }

    /** Stops every known action at its initial pose and resolves their combined result once. */
    public void stopAll() {
        for (AnimationAction action : actions) {
            action.stopAtInitialPose();
        }
        evaluate();
    }

    /** Samples every contributing action, then applies each controlled property exactly once. */
    void evaluate() {
        bindings.forEach(PropertyAccumulator::beginEvaluation);
        for (AnimationAction action : actions) {
            accumulate(action);
        }
        bindings.forEach(PropertyAccumulator::apply);
    }

    /** Registers stable shared property accumulators for every track in one new clip. */
    private void registerBindings(AnimationClip clip) {
        for (AnimationTrack track : clip.tracks()) {
            EnumMap<TransformProperty, PropertyAccumulator> targetBindings =
                    bindingsByTarget.computeIfAbsent(track.target(), ignored -> new EnumMap<>(TransformProperty.class));
            PropertyAccumulator binding = targetBindings.get(track.property());
            if (binding == null) {
                binding = new PropertyAccumulator(track.target(), track.property());
                targetBindings.put(track.property(), binding);
                bindings.add(binding);
            }
            bindingsByTrack.put(track, binding);
        }
    }

    /** Samples one active action into its shared property accumulators. */
    private void accumulate(AnimationAction action) {
        if (!action.contributes()) {
            return;
        }
        float actionWeight = action.effectiveWeight();
        for (AnimationTrack track : action.clip().tracks()) {
            float localTime = Math.clamp(action.time(), 0.0f, track.duration());
            float[] destination = track.components() == vectorSample.length ? vectorSample : quaternionSample;
            track.sample(localTime, destination);
            PropertyAccumulator binding =
                    Objects.requireNonNull(bindingsByTrack.get(track), "registered track binding");
            binding.accumulate(destination, actionWeight);
        }
    }

    /** Requires one non-null action owned by this mixer. */
    private AnimationAction requireOwned(AnimationAction action, String name) {
        AnimationAction validAction = Objects.requireNonNull(action, name);
        if (!validAction.belongsTo(this)) {
            throw new IllegalArgumentException(name + " must belong to this mixer");
        }
        return validAction;
    }
}
