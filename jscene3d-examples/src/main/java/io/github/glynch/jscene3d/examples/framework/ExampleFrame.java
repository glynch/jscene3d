/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

/**
 * Immutable host state supplied to one example update.
 *
 * @param elapsedSeconds finite non-negative elapsed duration
 * @param inputCaptured whether host UI owns pointer and keyboard input for this frame
 */
public record ExampleFrame(float elapsedSeconds, boolean inputCaptured) {
    /**
     * Validates the elapsed duration.
     *
     * @throws IllegalArgumentException if {@code elapsedSeconds} is negative or non-finite
     */
    public ExampleFrame {
        if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0.0f) {
            throw new IllegalArgumentException("elapsedSeconds must be finite and non-negative: " + elapsedSeconds);
        }
    }
}
