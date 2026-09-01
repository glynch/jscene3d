/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Local transform property controlled by a typed keyframe track. */
public enum TransformProperty {
    /** Local position with three scalar components per value. */
    POSITION,

    /** Local orientation with four normalized quaternion components per value. */
    ROTATION,

    /** Local scale with three scalar components per value. */
    SCALE
}
