/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.RendererSettingsScope;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.loaders.EnvironmentMapLoader;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.ToneMapping;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Demonstrates HDR image-based lighting across metallic and rough surfaces. */
public final class EnvironmentLightingExample {
    private static final String ENVIRONMENT_RESOURCE =
            "/io/github/glynch/jscene3d/examples/environment/studio_small_08_1k.hdr";
    private static final int GRID_SIZE = 5;
    private static final float GRID_SPACING = 1.65f;
    private static final float MINIMUM_ROUGHNESS = 0.08f;

    /** Prevents instantiation of this example entry point. */
    private EnvironmentLightingExample() {
        throw new AssertionError("EnvironmentLightingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Environment Lighting", EnvironmentLightingExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        EnvironmentMap environmentMap = EnvironmentMapLoader.load(environmentPath());
        BufferGeometry geometry = SphereGeometry.create(0.62f, 48, 24);
        List<StandardMaterial> materials = new ArrayList<>();
        Scene scene = createScene(geometry, materials, environmentMap);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 9.5f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(7.0f, 25.0f);
        controls.setDampingEnabled(true);
        controls.update();

        Renderer renderer = context.renderer();
        RendererSettingsScope rendererSettings = RendererSettingsScope.capture(renderer);
        renderer.setToneMapping(ToneMapping.ACES_FILMIC);
        renderer.setExposure(1.0f);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(rendererSettings);
        example.own(environmentMap);
        example.own(geometry);
        materials.forEach(example::own);
        EnvironmentControls settings = new EnvironmentControls(scene, environmentMap, renderer);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates a light-free material grid illuminated exclusively by the HDR environment. */
    private static Scene createScene(
            BufferGeometry geometry, List<StandardMaterial> materials, EnvironmentMap environmentMap) {
        Scene scene = new Scene();
        scene.setEnvironment(environmentMap);
        scene.setBackgroundEnvironment(environmentMap);
        scene.setEnvironmentIntensity(1.0f);
        scene.setBackgroundIntensity(0.65f);
        float origin = (GRID_SIZE - 1) * GRID_SPACING * -0.5f;
        for (int row = 0; row < GRID_SIZE; row++) {
            float roughness = 1.0f - row * (1.0f - MINIMUM_ROUGHNESS) / (GRID_SIZE - 1);
            for (int column = 0; column < GRID_SIZE; column++) {
                float metalness = (float) column / (GRID_SIZE - 1);
                StandardMaterial material = new StandardMaterial(Color.srgb(0xd8a43b));
                material.setMetalness(metalness);
                material.setRoughness(roughness);
                materials.add(material);
                Mesh sphere = new Mesh(geometry, material);
                sphere.setPosition(origin + column * GRID_SPACING, origin + row * GRID_SPACING, 0.0f);
                scene.add(sphere);
            }
        }
        return scene;
    }

    /** Creates interactive controls for independently visible and illuminating environment state. */
    private static ControlPanel createPanel(ExampleContext context, EnvironmentControls settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Environment Lighting");
        ControlPanel.Section environment = panel.addSection("Environment");
        environment.addBoolean("lighting", settings::isLightingEnabled, settings::setLightingEnabled);
        environment.addBoolean("background", settings::isBackgroundVisible, settings::setBackgroundVisible);
        environment.addFloat(
                "light intensity", settings::lightingIntensity, settings::setLightingIntensity, 0.0f, 3.0f);
        environment.addFloat(
                "background level", settings::backgroundIntensity, settings::setBackgroundIntensity, 0.0f, 2.0f);
        environment.addFloat("rotation", settings::rotation, settings::setRotation, -PI, PI);
        environment.addFloat("exposure", settings::exposure, settings::setExposure, 0.2f, 3.0f);
        ControlPanel.Section guide = panel.addSection("Grid");
        guide.addText("horizontal", () -> "metalness 0 to 1");
        guide.addText("vertical", () -> "roughness 1 to 0.08");
        return panel;
    }

    /** Resolves the required bundled HDR file path. */
    private static Path environmentPath() {
        URL resource = Objects.requireNonNull(
                EnvironmentLightingExample.class.getResource(ENVIRONMENT_RESOURCE), ENVIRONMENT_RESOURCE);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid bundled environment URI", exception);
        }
    }

    /** Mutable explicit GUI bindings for one environment-lighting scene. */
    private static final class EnvironmentControls {
        private final Scene scene;
        private final EnvironmentMap environmentMap;
        private final Renderer renderer;

        private float rotation;

        /** Retains scene, environment, and renderer state without taking ownership. */
        private EnvironmentControls(Scene scene, EnvironmentMap environmentMap, Renderer renderer) {
            this.scene = scene;
            this.environmentMap = environmentMap;
            this.renderer = renderer;
        }

        /** Returns whether image-based lighting is active. */
        private boolean isLightingEnabled() {
            return scene.environment() != null;
        }

        /** Enables or disables image-based lighting. */
        private void setLightingEnabled(boolean enabled) {
            if (enabled) {
                scene.setEnvironment(environmentMap);
            } else {
                scene.clearEnvironment();
            }
        }

        /** Returns whether the HDR environment is visible behind the spheres. */
        private boolean isBackgroundVisible() {
            return scene.backgroundEnvironment() != null;
        }

        /** Shows or hides the HDR background without changing lighting. */
        private void setBackgroundVisible(boolean visible) {
            if (visible) {
                scene.setBackgroundEnvironment(environmentMap);
            } else {
                scene.clearBackground();
            }
        }

        /** Returns the scene image-based-lighting multiplier. */
        private float lightingIntensity() {
            return scene.environmentIntensity();
        }

        /** Changes the scene image-based-lighting multiplier. */
        private void setLightingIntensity(float intensity) {
            scene.setEnvironmentIntensity(intensity);
        }

        /** Returns the visible background multiplier. */
        private float backgroundIntensity() {
            return scene.backgroundIntensity();
        }

        /** Changes the visible background multiplier. */
        private void setBackgroundIntensity(float intensity) {
            scene.setBackgroundIntensity(intensity);
        }

        /** Returns the current environment yaw in radians. */
        private float rotation() {
            return rotation;
        }

        /** Changes environment yaw in radians for both lighting and background sampling. */
        private void setRotation(float rotation) {
            this.rotation = rotation;
            scene.setEnvironmentRotation(0.0f, rotation, 0.0f);
        }

        /** Returns the renderer exposure multiplier. */
        private float exposure() {
            return renderer.exposure();
        }

        /** Changes the renderer exposure multiplier. */
        private void setExposure(float exposure) {
            renderer.setExposure(exposure);
        }
    }
}
