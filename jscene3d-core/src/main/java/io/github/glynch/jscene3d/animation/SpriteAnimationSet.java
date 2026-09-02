/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, ordered collection of uniquely named sprite animations. */
public final class SpriteAnimationSet {
    private final List<SpriteAnimation> animations;
    private final Map<String, SpriteAnimation> animationsByName;

    /**
     * Creates a non-empty set while preserving declaration order.
     *
     * @param animations one or more uniquely named animations
     * @throws NullPointerException if the list or an animation is {@code null}
     * @throws IllegalArgumentException if the list is empty or contains duplicate names
     */
    public SpriteAnimationSet(List<SpriteAnimation> animations) {
        this.animations = List.copyOf(Objects.requireNonNull(animations, "animations"));
        if (this.animations.isEmpty()) {
            throw new IllegalArgumentException("animations must not be empty");
        }
        animationsByName = indexAnimations(this.animations);
    }

    /**
     * Returns all animations in stable declaration order.
     *
     * @return immutable ordered animations
     */
    public List<SpriteAnimation> animations() {
        return animations;
    }

    /**
     * Returns one required animation by name.
     *
     * @param name animation name
     * @return matching animation
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if no animation has that name
     */
    public SpriteAnimation animation(String name) {
        String validName = Objects.requireNonNull(name, "name");
        SpriteAnimation animation = animationsByName.get(validName);
        if (animation == null) {
            throw new IllegalArgumentException("Unknown sprite animation: " + validName);
        }
        return animation;
    }

    /** Creates the immutable name index while rejecting ambiguous definitions. */
    private static Map<String, SpriteAnimation> indexAnimations(List<SpriteAnimation> animations) {
        Map<String, SpriteAnimation> result = new LinkedHashMap<>();
        for (SpriteAnimation animation : animations) {
            SpriteAnimation previous = result.put(animation.name(), animation);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate sprite animation name: " + animation.name());
            }
        }
        return Map.copyOf(result);
    }
}
