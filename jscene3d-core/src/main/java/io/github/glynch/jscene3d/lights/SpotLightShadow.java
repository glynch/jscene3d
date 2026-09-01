/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

/** Perspective shadow-camera configuration owned by a {@link SpotLight}. */
public final class SpotLightShadow extends LightShadow {
    /** Creates the common shadow defaults. */
    SpotLightShadow() {
        super(false);
    }
}
