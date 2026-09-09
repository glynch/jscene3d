/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

/**
 * Immutable inverse-distance attenuation authored for one positional sound.
 *
 * @param referenceDistance positive distance within which gain remains unchanged
 * @param maximumDistance positive distance at which attenuation is clamped
 * @param rolloffFactor non-negative attenuation rate beyond the reference distance
 */
public record PositionalSoundAttenuation(float referenceDistance, float maximumDistance, float rolloffFactor) {
    /** Validates one complete backend-independent attenuation definition. */
    public PositionalSoundAttenuation {
        requirePositive(referenceDistance, "referenceDistance");
        requirePositive(maximumDistance, "maximumDistance");
        if (maximumDistance < referenceDistance) {
            throw new IllegalArgumentException("maximumDistance must not be below referenceDistance");
        }
        if (!Float.isFinite(rolloffFactor) || rolloffFactor < 0.0F) {
            throw new IllegalArgumentException("rolloffFactor must be finite and non-negative");
        }
    }

    /** Requires one finite positive distance. */
    private static void requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
