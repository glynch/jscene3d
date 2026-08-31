/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.lights.Light;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.math.Color;
import java.util.ArrayList;

/** Reusable renderer-internal collection of visible lights for one frame. */
public final class LightCollection {
    private final int maximumPointLights;
    private final int maximumDirectionalLights;
    private final int maximumSpotLights;
    private final int maximumHemisphereLights;
    private final ArrayList<PointLight> pointLights;
    private final ArrayList<DirectionalLight> directionalLights;
    private final ArrayList<SpotLight> spotLights;
    private final ArrayList<HemisphereLight> hemisphereLights;

    private float ambientRed;
    private float ambientGreen;
    private float ambientBlue;

    /**
     * Creates a retained collection with deterministic light capacities.
     *
     * @param maximumPointLights maximum accepted visible point lights
     * @param maximumDirectionalLights maximum accepted visible directional lights
     * @param maximumSpotLights maximum accepted visible spotlights
     * @param maximumHemisphereLights maximum accepted visible hemisphere lights
     */
    public LightCollection(
            int maximumPointLights, int maximumDirectionalLights, int maximumSpotLights, int maximumHemisphereLights) {
        this.maximumPointLights = maximumPointLights;
        this.maximumDirectionalLights = maximumDirectionalLights;
        this.maximumSpotLights = maximumSpotLights;
        this.maximumHemisphereLights = maximumHemisphereLights;
        pointLights = new ArrayList<>(maximumPointLights);
        directionalLights = new ArrayList<>(maximumDirectionalLights);
        spotLights = new ArrayList<>(maximumSpotLights);
        hemisphereLights = new ArrayList<>(maximumHemisphereLights);
    }

    /**
     * Adds one visible supported light in deterministic scene order.
     *
     * @param light visible light to collect
     */
    public void add(Light light) {
        switch (light) {
            case AmbientLight ambientLight -> addAmbient(ambientLight);
            case DirectionalLight directionalLight -> addDirectional(directionalLight);
            case HemisphereLight hemisphereLight -> addHemisphere(hemisphereLight);
            case PointLight pointLight -> addPoint(pointLight);
            case SpotLight spotLight -> addSpot(spotLight);
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

    /**
     * Returns the number of visible directional lights.
     *
     * @return visible directional-light count
     */
    public int directionalLightCount() {
        return directionalLights.size();
    }

    /**
     * Returns a visible directional light by deterministic scene order.
     *
     * @param index zero-based light position
     * @return visible directional light
     */
    public DirectionalLight directionalLight(int index) {
        return directionalLights.get(index);
    }

    /**
     * Returns the number of visible spotlights.
     *
     * @return visible spotlight count
     */
    public int spotLightCount() {
        return spotLights.size();
    }

    /**
     * Returns a visible spotlight by deterministic scene order.
     *
     * @param index zero-based light position
     * @return visible spotlight
     */
    public SpotLight spotLight(int index) {
        return spotLights.get(index);
    }

    /**
     * Returns the number of visible hemisphere lights.
     *
     * @return visible hemisphere-light count
     */
    public int hemisphereLightCount() {
        return hemisphereLights.size();
    }

    /**
     * Returns a visible hemisphere light by deterministic scene order.
     *
     * @param index zero-based light position
     * @return visible hemisphere light
     */
    public HemisphereLight hemisphereLight(int index) {
        return hemisphereLights.get(index);
    }

    /** Releases active references and values while retaining collection capacity. */
    public void clear() {
        pointLights.clear();
        directionalLights.clear();
        spotLights.clear();
        hemisphereLights.clear();
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

    /** Adds one directional light while enforcing the deterministic renderer limit. */
    private void addDirectional(DirectionalLight light) {
        if (directionalLights.size() == maximumDirectionalLights) {
            throw new IllegalStateException("Scene has more visible directional lights than Renderer supports: "
                    + (directionalLights.size() + 1)
                    + " > "
                    + maximumDirectionalLights);
        }
        directionalLights.add(light);
    }

    /** Adds one spotlight while enforcing the deterministic renderer limit. */
    private void addSpot(SpotLight light) {
        if (spotLights.size() == maximumSpotLights) {
            throw new IllegalStateException("Scene has more visible spotlights than Renderer supports: "
                    + (spotLights.size() + 1)
                    + " > "
                    + maximumSpotLights);
        }
        spotLights.add(light);
    }

    /** Adds one hemisphere light while enforcing the deterministic renderer limit. */
    private void addHemisphere(HemisphereLight light) {
        if (hemisphereLights.size() == maximumHemisphereLights) {
            throw new IllegalStateException("Scene has more visible hemisphere lights than Renderer supports: "
                    + (hemisphereLights.size() + 1)
                    + " > "
                    + maximumHemisphereLights);
        }
        hemisphereLights.add(light);
    }

    /** Requires one accumulated ambient channel to remain finite. */
    private static float requireFiniteAmbient(float value, String channel) {
        if (!Float.isFinite(value)) {
            throw new IllegalStateException("Combined AmbientLight " + channel + " contribution is not finite");
        }
        return value;
    }
}
