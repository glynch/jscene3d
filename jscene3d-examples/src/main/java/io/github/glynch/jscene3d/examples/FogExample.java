/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.fogs.ExponentialSquaredFog;
import io.github.glynch.jscene3d.fogs.LinearFog;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.ConeGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Demonstrates linear and exponential-squared distance fog across meshes and lines. */
public final class FogExample {
    private static final List<ControlPanel.Choice<FogMode>> MODE_CHOICES = List.of(
            new ControlPanel.Choice<>(FogMode.NONE, "none"),
            new ControlPanel.Choice<>(FogMode.LINEAR, "linear"),
            new ControlPanel.Choice<>(FogMode.EXPONENTIAL_SQUARED, "exponential squared"));
    private static final List<ControlPanel.Choice<FogPalette>> COLOR_CHOICES = List.of(
            new ControlPanel.Choice<>(FogPalette.COOL_MIST, "cool mist"),
            new ControlPanel.Choice<>(FogPalette.WARM_HAZE, "warm haze"),
            new ControlPanel.Choice<>(FogPalette.NIGHT, "night"));

    /** Prevents instantiation of this example entry point. */
    private FogExample() {
        throw new AssertionError("FogExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Fog", FogExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry[] geometries = createGeometries();
        Material[] materials = createMaterials();
        GridHelper grid = new GridHelper(150.0f, 30, Color.srgb(0x607786), Color.srgb(0x6f8490));
        grid.setPosition(0.0f, -0.02f, -27.0f);

        Scene scene = new Scene();
        addObjectField(scene, geometries, materials);
        scene.add(grid);
        scene.add(new HemisphereLight(Color.srgb(0xe8f6ff), Color.srgb(0x30404a), 2.4f));
        scene.add(createKeyLight());

        FogSettings settings = new FogSettings(scene);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 180.0f);
        camera.setPosition(14.0f, 8.0f, 22.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 2.0f, -24.0f);
        controls.setDistanceLimits(10.0f, 100.0f);
        controls.setPolarAngleLimits(0.12f, PI_OVER_TWO);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        ownResources(example, geometries);
        ownResources(example, materials);
        example.own(grid);
        ControlPanel panel = example.addOverlay(createPanel(context, settings, controls));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fps.update();
        });
        return example;
    }

    /** Creates the four shared shapes repeated through the deterministic depth field. */
    private static BufferGeometry[] createGeometries() {
        return new BufferGeometry[] {
            BoxGeometry.create(2.7f, 2.7f, 2.7f),
            SphereGeometry.create(1.55f, 32, 18),
            ConeGeometry.create(1.65f, 3.2f, 32),
            TorusGeometry.create(1.25f, 0.48f, 18, 40)
        };
    }

    /** Creates one shared representative of each built-in mesh-material family. */
    private static Material[] createMaterials() {
        BasicMaterial basic = new BasicMaterial(Color.srgb(0xffb74d));
        LambertMaterial lambert = new LambertMaterial(Color.srgb(0x35c9d0));
        PhongMaterial phong = new PhongMaterial(Color.srgb(0xff5c93));
        phong.setSpecular(Color.WHITE);
        phong.setShininess(72.0f);
        NormalMaterial normal = new NormalMaterial();
        StandardMaterial standard = new StandardMaterial(Color.srgb(0xd7ee58));
        standard.setMetalness(0.15f);
        standard.setRoughness(0.38f);
        return new Material[] {basic, lambert, phong, normal, standard};
    }

    /** Adds a reproducible field whose rows make distance attenuation immediately visible. */
    private static void addObjectField(Scene scene, BufferGeometry[] geometries, Material[] materials) {
        for (int row = 0; row < 8; row++) {
            float z = 8.0f - row * 10.0f;
            for (int column = 0; column < materials.length; column++) {
                BufferGeometry geometry = geometries[(row + column) % geometries.length];
                Mesh mesh = new Mesh(geometry, materials[column]);
                mesh.setPosition((column - 2.0f) * 5.2f, 1.65f, z);
                mesh.rotateY((row * 0.29f) + (column * 0.21f));
                if (geometry == geometries[3]) {
                    mesh.rotateX(PI_OVER_TWO);
                }
                scene.add(mesh);
            }
        }
    }

    /** Creates a warm directional light looking down the length of the field. */
    private static DirectionalLight createKeyLight() {
        DirectionalLight light = new DirectionalLight(Color.srgb(0xffe2bd), 2.8f);
        light.setPosition(-12.0f, 18.0f, 18.0f);
        light.setTarget(0.0f, 0.0f, -28.0f);
        return light;
    }

    /** Creates live controls for selecting and tuning both supported fog models. */
    private static ControlPanel createPanel(ExampleContext context, FogSettings settings, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Distance Fog");
        ControlPanel.Section fog = panel.addSection("Fog");
        fog.addChoice("mode", settings::mode, settings::setMode, MODE_CHOICES);
        fog.addChoice("color", settings::palette, settings::setPalette, COLOR_CHOICES);
        ControlPanel.Section linear = panel.addSection("Linear");
        linear.setEnabled(() -> settings.mode() == FogMode.LINEAR);
        linear.addFloat("near", settings::nearDistance, settings::setNearDistance, 0.0f, 100.0f);
        linear.addFloat("far", settings::farDistance, settings::setFarDistance, 1.0f, 140.0f);
        ControlPanel.Section exponential = panel.addSection("Exponential squared");
        exponential.setEnabled(() -> settings.mode() == FogMode.EXPONENTIAL_SQUARED);
        exponential.addFloat("density", settings::density, settings::setDensity, 0.0f, 0.08f);
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera and fog", () -> {
            settings.reset();
            controls.reset();
        });
        return panel;
    }

    /** Registers a homogeneous group of closeable example resources. */
    private static void ownResources(SceneExample example, AutoCloseable[] resources) {
        for (AutoCloseable resource : resources) {
            example.own(resource);
        }
    }

    /** Fog model selected by the example control panel. */
    private enum FogMode {
        NONE,
        LINEAR,
        EXPONENTIAL_SQUARED
    }

    /** Coordinated fog and background color presets. */
    private enum FogPalette {
        COOL_MIST(Color.srgb(0x8397a5)),
        WARM_HAZE(Color.srgb(0xb89b7a)),
        NIGHT(Color.srgb(0x202b3a));

        private final Color color;

        /** Retains the immutable preset color. */
        FogPalette(Color color) {
            this.color = color;
        }
    }

    /** Owns the two shared fog descriptions and keeps their GUI constraints coherent. */
    private static final class FogSettings {
        private static final float MINIMUM_INTERVAL = 1.0f;
        private static final float DEFAULT_NEAR = 18.0f;
        private static final float DEFAULT_FAR = 72.0f;
        private static final float DEFAULT_DENSITY = 0.025f;

        private final Scene scene;
        private final LinearFog linearFog;
        private final ExponentialSquaredFog exponentialFog;

        private FogMode mode;
        private FogPalette palette;

        /** Creates both fog descriptions and applies the default linear presentation. */
        private FogSettings(Scene scene) {
            this.scene = scene;
            palette = FogPalette.COOL_MIST;
            linearFog = new LinearFog(palette.color, DEFAULT_NEAR, DEFAULT_FAR);
            exponentialFog = new ExponentialSquaredFog(palette.color, DEFAULT_DENSITY);
            setPalette(palette);
            setMode(FogMode.LINEAR);
        }

        /** Returns the selected fog model. */
        private FogMode mode() {
            return mode;
        }

        /** Applies the selected fog model or disables fog without losing its settings. */
        private void setMode(FogMode mode) {
            this.mode = mode;
            switch (mode) {
                case NONE -> scene.clearFog();
                case LINEAR -> scene.setFog(linearFog);
                case EXPONENTIAL_SQUARED -> scene.setFog(exponentialFog);
            }
        }

        /** Returns the selected coordinated color preset. */
        private FogPalette palette() {
            return palette;
        }

        /** Changes the background and both retained fog colors together. */
        private void setPalette(FogPalette palette) {
            this.palette = palette;
            scene.setBackground(palette.color);
            linearFog.setColor(palette.color);
            exponentialFog.setColor(palette.color);
        }

        /** Returns the current linear clear distance. */
        private float nearDistance() {
            return linearFog.nearDistance();
        }

        /** Changes the clear distance while preserving a valid fog interval. */
        private void setNearDistance(float nearDistance) {
            if (nearDistance >= linearFog.farDistance()) {
                linearFog.setFarDistance(nearDistance + MINIMUM_INTERVAL);
            }
            linearFog.setNearDistance(nearDistance);
        }

        /** Returns the current fully fogged distance. */
        private float farDistance() {
            return linearFog.farDistance();
        }

        /** Changes the fully fogged distance while preserving a valid fog interval. */
        private void setFarDistance(float farDistance) {
            if (farDistance <= linearFog.nearDistance()) {
                linearFog.setNearDistance(Math.max(0.0f, farDistance - MINIMUM_INTERVAL));
            }
            linearFog.setFarDistance(farDistance);
        }

        /** Returns the current exponential-squared density. */
        private float density() {
            return exponentialFog.density();
        }

        /** Changes the exponential-squared density. */
        private void setDensity(float density) {
            exponentialFog.setDensity(density);
        }

        /** Restores the default color, parameters, and linear fog selection. */
        private void reset() {
            setPalette(FogPalette.COOL_MIST);
            linearFog.setNearDistance(0.0f);
            linearFog.setFarDistance(DEFAULT_FAR);
            linearFog.setNearDistance(DEFAULT_NEAR);
            exponentialFog.setDensity(DEFAULT_DENSITY);
            setMode(FogMode.LINEAR);
        }
    }
}
