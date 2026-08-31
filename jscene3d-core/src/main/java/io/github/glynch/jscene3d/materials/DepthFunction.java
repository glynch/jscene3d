/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

/** Comparison applied between an incoming fragment depth and the stored depth. */
public enum DepthFunction {
    /** Never accepts an incoming fragment. */
    NEVER,

    /** Accepts an incoming fragment whose depth is less than the stored depth. */
    LESS,

    /** Accepts an incoming fragment whose depth equals the stored depth. */
    EQUAL,

    /** Accepts an incoming fragment whose depth is less than or equal to the stored depth. */
    LESS_OR_EQUAL,

    /** Accepts an incoming fragment whose depth is greater than the stored depth. */
    GREATER,

    /** Accepts an incoming fragment whose depth differs from the stored depth. */
    NOT_EQUAL,

    /** Accepts an incoming fragment whose depth is greater than or equal to the stored depth. */
    GREATER_OR_EQUAL,

    /** Always accepts an incoming fragment. */
    ALWAYS
}
