/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable named collection of typed keyframe tracks sharing one playback timeline. */
public final class AnimationClip {
    private final String name;
    private final List<AnimationTrack> tracks;
    private final float duration;

    /**
     * Creates a clip whose duration is the greatest final track time.
     *
     * @param name non-blank display name
     * @param tracks one or more tracks to retain
     * @throws NullPointerException if an argument or track is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or {@code tracks} is empty
     */
    public AnimationClip(String name, List<? extends AnimationTrack> tracks) {
        this.name = Preconditions.requireNonBlank(name, "name");
        this.tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
        if (this.tracks.isEmpty()) {
            throw new IllegalArgumentException("tracks must not be empty");
        }
        validateUniqueBindings();
        float maximumDuration = 0.0f;
        for (AnimationTrack track : this.tracks) {
            maximumDuration = Math.max(maximumDuration, track.duration());
        }
        duration = maximumDuration;
    }

    /**
     * Returns the clip's display name.
     *
     * @return non-blank name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the immutable track list in evaluation order.
     *
     * @return immutable tracks
     */
    public List<AnimationTrack> tracks() {
        return tracks;
    }

    /**
     * Returns the greatest final track time in seconds.
     *
     * @return non-negative duration
     */
    public float duration() {
        return duration;
    }

    /** Rejects multiple tracks that would ambiguously control one property in this clip. */
    private void validateUniqueBindings() {
        Map<Object3D, HashSet<AnimatedProperty>> propertiesByTarget = new IdentityHashMap<>();
        for (AnimationTrack track : tracks) {
            HashSet<AnimatedProperty> properties =
                    propertiesByTarget.computeIfAbsent(track.target(), ignored -> new HashSet<>());
            if (!properties.add(track.property())) {
                throw new IllegalArgumentException(
                        "tracks must not contain duplicate target property bindings: " + track.property());
            }
        }
    }
}
