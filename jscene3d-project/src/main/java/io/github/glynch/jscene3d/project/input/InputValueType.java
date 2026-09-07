/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

/** Semantic value shapes supported by authored input actions. */
public enum InputValueType {
    /** A digital action with held, pressed, and released state. */
    BUTTON,
    /** One signed analog value in the inclusive range minus one to one. */
    AXIS_1D,
    /** Two signed analog values, each in the inclusive range minus one to one. */
    AXIS_2D
}
