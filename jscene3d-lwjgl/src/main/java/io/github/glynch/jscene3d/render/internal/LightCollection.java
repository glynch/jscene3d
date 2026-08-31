/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.Light;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.math.Color;
import java.util.ArrayList;

/** Reusable renderer-internal collection of visible lights for one frame. */
public final class LightCollection {
    private final int maximumPointLights;
    private final ArrayList<PointLight> pointLights;

    private float ambientRed;
    private float ambientGreen;
    private float ambientBlue;

    /**
     * Creates a retained collection with a deterministic point-light capacity.
     *
     * @param maximumPointLights maximum accepted visible point lights
     */
    public LightCollection(int maximumPointLights) {
        this.maximumPointLights = maximumPointLights;
        pointLights = new ArrayList<>(maximumPointLights);
    }

    /**
     * Adds one visible supported light in deterministic scene order.
     *
     * @param light visible light to collect
     */
    public void add(Light light) {
        switch (light) {
            case AmbientLight ambientLight -> addAmbient(ambientLight);
            case PointLight pointLight -> addPoint(pointLight);
            default ->
                throw new IllegalStateException(
                        "Unsupported light type: " + light.getClass().getName());
        }
    }

    /**
     * Returns the combined linear red ambient contribution.
     *
     * @return red ambient contribution
     */
    public float ambientRed() {
        return ambientRed;
    }

    /**
     * Returns the combined linear green ambient contribution.
     *
     * @return green ambient contribution
     */
    public float ambientGreen() {
        return ambientGreen;
    }

    /**
     * Returns the combined linear blue ambient contribution.
     *
     * @return blue ambient contribution
     */
    public float ambientBlue() {
        return ambientBlue;
    }

    /**
     * Returns the number of visible point lights.
     *
     * @return visible point-light count
     */
    public int pointLightCount() {
        return pointLights.size();
    }

    /**
     * Returns a visible point light by deterministic scene order.
     *
     * @param index zero-based light position
     * @return visible point light
     */
    public PointLight pointLight(int index) {
        return pointLights.get(index);
    }

    /** Releases active references and values while retaining collection capacity. */
    public void clear() {
        pointLights.clear();
        ambientRed = 0.0f;
        ambientGreen = 0.0f;
        ambientBlue = 0.0f;
    }

    /** Accumulates one ambient contribution, rejecting floating-point overflow. */
    private void addAmbient(AmbientLight light) {
        Color color = light.color();
        float intensity = light.intensity();
        ambientRed = requireFiniteAmbient(ambientRed + color.red() * intensity, "red");
        ambientGreen = requireFiniteAmbient(ambientGreen + color.green() * intensity, "green");
        ambientBlue = requireFiniteAmbient(ambientBlue + color.blue() * intensity, "blue");
    }

    /** Adds one point light while enforcing the deterministic renderer limit. */
    private void addPoint(PointLight light) {
        if (pointLights.size() == maximumPointLights) {
            throw new IllegalStateException("Scene has more visible point lights than Renderer supports: "
                    + (pointLights.size() + 1)
                    + " > "
                    + maximumPointLights);
        }
        pointLights.add(light);
    }

    /** Requires one accumulated ambient channel to remain finite. */
    private static float requireFiniteAmbient(float value, String channel) {
        if (!Float.isFinite(value)) {
            throw new IllegalStateException("Combined AmbientLight " + channel + " contribution is not finite");
        }
        return value;
    }
}
