/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.RendererSettingsScope;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.loaders.EnvironmentMapLoader;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.ToneMapping;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;

/** Displays a realistic CC0 glTF asset with HDR image-based lighting. */
public final class AvocadoModelExample {
    private static final Color BACKGROUND = Color.srgb(0x10141b);
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/avocado/Avocado.glb";
    private static final String ENVIRONMENT_RESOURCE =
            "/io/github/glynch/jscene3d/examples/environment/studio_small_08_1k.hdr";

    /** Prevents instantiation of this example entry point. */
    private AvocadoModelExample() {
        throw new AssertionError("AvocadoModelExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Avocado glTF Model", AvocadoModelExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded = GltfLoader.load(path(AvocadoModelExample.class, MODEL_RESOURCE));
        EnvironmentMap environmentMap =
                EnvironmentMapLoader.load(path(AvocadoModelExample.class, ENVIRONMENT_RESOURCE));
        Scene scene = loaded.scene();
        scene.setEnvironment(environmentMap);
        scene.setBackground(BACKGROUND);
        scene.setEnvironmentIntensity(0.65f);

        Object3D model = scene.children().getFirst();
        model.setScale(45.0f, 45.0f, 45.0f);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.05f, 100.0f);
        camera.setPosition(2.6f, 2.0f, 3.2f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.3f, 0.0f);
        controls.setDistanceLimits(2.0f, 12.0f);
        controls.setDampingEnabled(true);
        controls.update();

        Renderer renderer = context.renderer();
        RendererSettingsScope rendererSettings = RendererSettingsScope.capture(renderer);
        renderer.setToneMapping(ToneMapping.ACES_FILMIC);
        renderer.setExposure(0.85f);

        ExampleState settings = new ExampleState(scene, environmentMap, renderer);
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(rendererSettings);
        example.own(environmentMap);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
            if (settings.autoRotate()) {
                model.rotateY(frame.elapsedSeconds() * 0.35f);
            }
        });
        return example;
    }

    /** Creates controls for the model presentation and independent environment roles. */
    private static ControlPanel createPanel(ExampleContext context, ExampleState settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Avocado Model");
        ControlPanel.Section model = panel.addSection("Model");
        model.addBoolean("auto rotate", settings::autoRotate, settings::setAutoRotate);
        ControlPanel.Section environment = panel.addSection("Environment");
        environment.addBoolean("background", settings::backgroundVisible, settings::setBackgroundVisible);
        environment.addFloat(
                "light intensity", settings::lightingIntensity, settings::setLightingIntensity, 0.0f, 3.0f);
        environment.addFloat("rotation", settings::rotation, settings::setRotation, -PI, PI);
        environment.addFloat("exposure", settings::exposure, settings::setExposure, 0.2f, 3.0f);
        return panel;
    }

    /** Mutable explicit GUI bindings for the realistic model presentation. */
    private static final class ExampleState {
        private final Scene scene;
        private final EnvironmentMap environmentMap;
        private final Renderer renderer;

        private boolean autoRotate = true;
        private float rotation;

        /** Retains scene, environment, and renderer state without taking ownership. */
        private ExampleState(Scene scene, EnvironmentMap environmentMap, Renderer renderer) {
            this.scene = scene;
            this.environmentMap = environmentMap;
            this.renderer = renderer;
        }

        /** Returns whether automatic model rotation is active. */
        private boolean autoRotate() {
            return autoRotate;
        }

        /** Enables or disables automatic model rotation. */
        private void setAutoRotate(boolean autoRotate) {
            this.autoRotate = autoRotate;
        }

        /** Returns whether the HDR background is visible. */
        private boolean backgroundVisible() {
            return scene.backgroundEnvironment() != null;
        }

        /** Shows or hides the HDR background without changing image-based lighting. */
        private void setBackgroundVisible(boolean visible) {
            if (visible) {
                scene.setBackgroundEnvironment(environmentMap);
            } else {
                scene.setBackground(BACKGROUND);
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

        /** Returns the current environment yaw in radians. */
        private float rotation() {
            return rotation;
        }

        /** Changes environment yaw for lighting and background sampling. */
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
