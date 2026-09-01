/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

/** Identifies the renderer pass that is invoking an object render callback. */
public enum RenderPass {
    /** The visible scene-color pass requested by the application. */
    MAIN,

    /** A light-owned depth pass used to generate a shadow map. */
    SHADOW
}
