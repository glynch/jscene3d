/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

/** Six-face perspective shadow-camera configuration owned by a {@link PointLight}. */
public final class PointLightShadow extends LightShadow {
    /** Creates common shadow defaults with square cube-map faces. */
    PointLightShadow() {
        super(true);
    }
}
