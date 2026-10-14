/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import java.util.Objects;

/** Immutable logical-pixel presentation and picking state for one authored-camera occurrence. */
record SceneCameraIconState(
        CompositionOccurrenceId occurrence,
        Point center,
        float depth,
        Bounds bounds,
        int cameraCount,
        boolean hasPerspective,
        boolean hasOrthographic,
        boolean selected) {
    /** Validates one projected icon. */
    SceneCameraIconState {
        Objects.requireNonNull(occurrence, "occurrence");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(bounds, "bounds");
        if (!Float.isFinite(depth)) {
            throw new IllegalArgumentException("depth must be finite");
        }
        if (cameraCount <= 0) {
            throw new IllegalArgumentException("cameraCount must be positive");
        }
        if (!hasPerspective && !hasOrthographic) {
            throw new IllegalArgumentException("an icon must represent at least one camera type");
        }
    }

    /** Finite logical-pixel point. */
    record Point(float x, float y) {
        /** Validates both coordinates. */
        Point {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("point coordinates must be finite");
            }
        }
    }

    /** Finite non-negative logical-pixel rectangle. */
    record Bounds(float x, float y, float width, float height) {
        /** Validates rectangle coordinates and extents. */
        Bounds {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)) {
                throw new IllegalArgumentException("bounds must be finite");
            }
            if (width < 0.0f || height < 0.0f) {
                throw new IllegalArgumentException("bounds extents must be non-negative");
            }
        }

        /** Returns whether this rectangle contains the supplied logical-pixel point. */
        boolean contains(float horizontal, float vertical) {
            return horizontal >= x && horizontal <= x + width && vertical >= y && vertical <= y + height;
        }
    }
}
