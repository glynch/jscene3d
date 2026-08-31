/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_TWO;
import static io.github.glynch.jscene3d.core.Angles.TWO_PI;

import io.github.glynch.jscene3d.core.AmbientLight;
import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.LambertMaterial;
import io.github.glynch.jscene3d.core.MaterialSide;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PointLight;
import io.github.glynch.jscene3d.core.RotationOrder;
import io.github.glynch.jscene3d.core.Scene;
import java.util.ArrayList;
import java.util.List;

/** Builds, animates, and owns the illustrative Solar System scene. */
final class SolarSystemScene implements AutoCloseable {
    private static final float DEFAULT_TIME_SCALE = 1.0f;

    private final SolarSystemResources resources;
    private final Scene scene = new Scene();
    private final List<OrbitingBody> orbitingBodies = new ArrayList<>();
    private final Mesh starField;
    private final Mesh sun;

    private boolean paused;
    private float timeScale = DEFAULT_TIME_SCALE;
    private float sunRotation;

    /** Builds the complete scene while retaining its resource owner. */
    private SolarSystemScene(SolarSystemResources resources) {
        this.resources = resources;
        scene.setBackground(Color.BLACK);
        starField = createStarField();
        scene.add(starField);
        sun = createSun();
        scene.add(sun);
        scene.add(new AmbientLight(Color.srgb(0x8fa0c0), 0.06f));

        PointLight sunlight = new PointLight(Color.srgb(0xfff1d0), 1.35f);
        sunlight.setDecay(0.0f);
        scene.add(sunlight);

        addPlanet("2k_mercury.jpg", 0.32f, 4.0f, 0.72f, 0.35f, (float) Math.toRadians(0.03), 0.0f);
        addPlanet("2k_venus_surface.jpg", 0.52f, 5.5f, 0.54f, -0.18f, (float) Math.toRadians(177.4), 0.8f);
        OrbitingBody earth =
                addPlanet("2k_earth_daymap.jpg", 0.58f, 7.2f, 0.42f, 1.35f, (float) Math.toRadians(23.4), 1.6f);
        addMoon(earth);
        addPlanet("2k_mars.jpg", 0.42f, 8.8f, 0.34f, 1.15f, (float) Math.toRadians(25.2), 2.4f);
        addPlanet("2k_jupiter.jpg", 1.25f, 12.0f, 0.22f, 1.75f, (float) Math.toRadians(3.1), 3.2f);
        OrbitingBody saturn =
                addPlanet("2k_saturn.jpg", 1.05f, 15.5f, 0.17f, 1.45f, (float) Math.toRadians(26.7), 4.0f);
        addSaturnRings(saturn);
        addPlanet("2k_uranus.jpg", 0.78f, 19.0f, 0.12f, -1.05f, (float) Math.toRadians(97.8), 4.8f);
        addPlanet("2k_neptune.jpg", 0.76f, 22.0f, 0.09f, 1.0f, (float) Math.toRadians(28.3), 5.6f);
    }

    /** Creates the fully initialized scene and closes partial resources if assembly fails. */
    static SolarSystemScene create() {
        SolarSystemResources resources = new SolarSystemResources();
        try {
            return new SolarSystemScene(resources);
        } catch (RuntimeException exception) {
            resources.close();
            throw exception;
        }
    }

    /** Returns the scene passed to the renderer. */
    Scene scene() {
        return scene;
    }

    /** Returns whether orbital and rotational animation is paused. */
    boolean isPaused() {
        return paused;
    }

    /** Pauses or resumes orbital and rotational animation. */
    void setPaused(boolean paused) {
        this.paused = paused;
    }

    /** Returns the simulation-speed multiplier. */
    float timeScale() {
        return timeScale;
    }

    /** Changes the finite non-negative simulation-speed multiplier. */
    void setTimeScale(float timeScale) {
        if (!Float.isFinite(timeScale) || timeScale < 0.0f) {
            throw new IllegalArgumentException("timeScale must be finite and non-negative: " + timeScale);
        }
        this.timeScale = timeScale;
    }

    /** Returns whether the star-field sphere participates in rendering. */
    boolean isStarFieldVisible() {
        return starField.isVisible();
    }

    /** Shows or hides the star-field sphere. */
    void setStarFieldVisible(boolean visible) {
        starField.setVisible(visible);
    }

    /** Advances every orbit and axial rotation by a frame duration. */
    void update(float elapsedSeconds) {
        if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0.0f) {
            throw new IllegalArgumentException("elapsedSeconds must be finite and non-negative: " + elapsedSeconds);
        }
        if (paused) {
            return;
        }
        float scaledSeconds = elapsedSeconds * timeScale;
        for (int index = 0; index < orbitingBodies.size(); index++) {
            orbitingBodies.get(index).advance(scaledSeconds);
        }
        sunRotation = (sunRotation + scaledSeconds * 0.12f) % TWO_PI;
        sun.setRotationFromEuler(0.0f, sunRotation, 0.0f, RotationOrder.XYZ);
    }

    /** Restores the initial arrangement without changing pause or display settings. */
    void reset() {
        for (int index = 0; index < orbitingBodies.size(); index++) {
            orbitingBodies.get(index).reset();
        }
        sunRotation = 0.0f;
        sun.setRotationFromEuler(0.0f, 0.0f, 0.0f, RotationOrder.XYZ);
    }

    /** Closes all geometry, texture, and material descriptions owned by this scene. */
    @Override
    public void close() {
        resources.close();
    }

    /** Creates the unlit inward-facing background sphere. */
    private Mesh createStarField() {
        BasicMaterial material = resources.createBasicMaterial("2k_stars_milky_way.jpg");
        material.setSide(MaterialSide.BACK);
        material.setDepthWriteEnabled(false);
        Mesh mesh = new Mesh(resources.sphereGeometry(), material);
        mesh.setScale(100.0f, 100.0f, 100.0f);
        return mesh;
    }

    /** Creates the unlit textured Sun at the scene origin. */
    private Mesh createSun() {
        BasicMaterial material = resources.createBasicMaterial("2k_sun.jpg");
        Mesh mesh = new Mesh(resources.sphereGeometry(), material);
        mesh.setScale(2.2f, 2.2f, 2.2f);
        return mesh;
    }

    /** Adds one textured planet and returns its animated hierarchy nodes. */
    private OrbitingBody addPlanet(
            String textureFileName,
            float radius,
            float orbitRadius,
            float orbitSpeed,
            float rotationSpeed,
            float axialTilt,
            float initialOrbitAngle) {
        Group orbit = new Group();
        Group anchor = new Group();
        anchor.setPosition(orbitRadius, 0.0f, 0.0f);
        Group tilt = new Group();
        tilt.setRotationFromEuler(0.0f, 0.0f, axialTilt, RotationOrder.XYZ);

        LambertMaterial material = resources.createLambertMaterial(textureFileName);
        Mesh surface = new Mesh(resources.sphereGeometry(), material);
        surface.setScale(radius, radius, radius);

        tilt.add(surface);
        anchor.add(tilt);
        orbit.add(anchor);
        scene.add(orbit);

        OrbitingBody body =
                new OrbitingBody(orbit, anchor, tilt, surface, orbitSpeed, rotationSpeed, initialOrbitAngle);
        orbitingBodies.add(body);
        body.reset();
        return body;
    }

    /** Adds the Moon beneath Earth's orbital anchor so it follows Earth around the Sun. */
    private void addMoon(OrbitingBody earth) {
        Group moonOrbit = new Group();
        Group moonAnchor = new Group();
        moonAnchor.setPosition(1.15f, 0.0f, 0.0f);
        Group moonTilt = new Group();
        moonTilt.setRotationFromEuler(0.0f, 0.0f, (float) Math.toRadians(6.7), RotationOrder.XYZ);
        LambertMaterial moonMaterial = resources.createLambertMaterial("2k_moon.jpg");
        Mesh moonSurface = new Mesh(resources.sphereGeometry(), moonMaterial);
        moonSurface.setScale(0.16f, 0.16f, 0.16f);

        moonTilt.add(moonSurface);
        moonAnchor.add(moonTilt);
        moonOrbit.add(moonAnchor);
        earth.anchor().add(moonOrbit);

        OrbitingBody moon = new OrbitingBody(moonOrbit, moonAnchor, moonTilt, moonSurface, 1.8f, 0.35f, 0.4f);
        orbitingBodies.add(moon);
        moon.reset();
    }

    /** Adds a transparent radial-textured annulus to Saturn's tilted local frame. */
    private void addSaturnRings(OrbitingBody saturn) {
        LambertMaterial ringMaterial = resources.createLambertMaterial("2k_saturn_ring_alpha.png");
        ringMaterial.setSide(MaterialSide.DOUBLE);
        ringMaterial.setTransparent(true);
        ringMaterial.setDepthWriteEnabled(false);
        Mesh rings = new Mesh(resources.ringGeometry(), ringMaterial);
        rings.rotateX(-PI_OVER_TWO);
        saturn.tilt().add(rings);
    }

    /** Retains the hierarchy and rates for one animated spherical body. */
    private static final class OrbitingBody {
        private final Group orbit;
        private final Group anchor;
        private final Group tilt;
        private final Mesh surface;
        private final float orbitSpeed;
        private final float rotationSpeed;
        private final float initialOrbitAngle;

        private float orbitAngle;
        private float rotationAngle;

        /** Retains the nodes, angular rates, and initial orbital position. */
        private OrbitingBody(
                Group orbit,
                Group anchor,
                Group tilt,
                Mesh surface,
                float orbitSpeed,
                float rotationSpeed,
                float initialOrbitAngle) {
            this.orbit = orbit;
            this.anchor = anchor;
            this.tilt = tilt;
            this.surface = surface;
            this.orbitSpeed = orbitSpeed;
            this.rotationSpeed = rotationSpeed;
            this.initialOrbitAngle = initialOrbitAngle;
        }

        /** Returns the non-spinning position node for satellites. */
        private Group anchor() {
            return anchor;
        }

        /** Returns the axially tilted local frame for ring geometry. */
        private Group tilt() {
            return tilt;
        }

        /** Advances and applies orbital and axial angles. */
        private void advance(float elapsedSeconds) {
            orbitAngle = (orbitAngle + orbitSpeed * elapsedSeconds) % TWO_PI;
            rotationAngle = (rotationAngle + rotationSpeed * elapsedSeconds) % TWO_PI;
            apply();
        }

        /** Restores and applies the initial angles. */
        private void reset() {
            orbitAngle = initialOrbitAngle;
            rotationAngle = 0.0f;
            apply();
        }

        /** Writes the retained angles through controlled scene-node setters. */
        private void apply() {
            orbit.setRotationFromEuler(0.0f, orbitAngle, 0.0f, RotationOrder.XYZ);
            surface.setRotationFromEuler(0.0f, rotationAngle, 0.0f, RotationOrder.XYZ);
        }
    }
}
