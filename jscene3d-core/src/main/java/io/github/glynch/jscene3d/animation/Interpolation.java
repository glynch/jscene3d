/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Interpolation applied between adjacent animation keyframes. */
public enum Interpolation {
    /** Holds the earlier keyframe value until the next keyframe is reached. */
    STEP,

    /** Interpolates directly between adjacent keyframe values. */
    LINEAR,

    /** Applies cubic Hermite interpolation using per-key incoming and outgoing tangents. */
    CUBIC_SPLINE
}
