/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.math.Color;

/** Base scene light capable of requesting renderer-owned shadow-map generation. */
public abstract sealed class ShadowCastingLight extends Light permits DirectionalLight, PointLight, SpotLight {
    private boolean shadowCastingEnabled;

    /** Creates a white shadow-capable light with unit intensity. */
    protected ShadowCastingLight() {
        super();
    }

    /**
     * Creates a shadow-capable light with unit intensity.
     *
     * @param color immutable linear-sRGB light color
     */
    protected ShadowCastingLight(Color color) {
        super(color);
    }

    /**
     * Creates a shadow-capable light with the supplied color and intensity.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     */
    protected ShadowCastingLight(Color color, float intensity) {
        super(color, intensity);
    }

    /**
     * Returns whether renderers should generate a shadow map for this light.
     *
     * @return {@code false} by default
     */
    public final boolean isShadowCastingEnabled() {
        return shadowCastingEnabled;
    }

    /**
     * Changes whether renderers should generate a shadow map for this light.
     *
     * @param enabled whether shadow-map generation is requested
     */
    public final void setShadowCastingEnabled(boolean enabled) {
        shadowCastingEnabled = enabled;
    }

    /**
     * Returns this light's stable owned shadow description.
     *
     * @return mutable renderer-independent shadow configuration
     */
    public abstract LightShadow shadow();
}
