/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.LoopMode;
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
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.ToneMapping;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;

/** Plays Glen Fox's Draco-compressed, skeletally animated Littlest Tokyo scene. */
public final class LittlestTokyoExample {
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/littlest-tokyo/LittlestTokyo.glb";
    private static final String ENVIRONMENT_RESOURCE =
            "/io/github/glynch/jscene3d/examples/environment/studio_small_08_1k.hdr";

    /** Prevents instantiation of this example entry point. */
    private LittlestTokyoExample() {
        throw new AssertionError("LittlestTokyoExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Littlest Tokyo", LittlestTokyoExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded =
                GltfLoader.load(path(LittlestTokyoExample.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE));
        if (loaded.animations().isEmpty()) {
            loaded.close();
            throw new IllegalStateException("Littlest Tokyo contains no animation clip");
        }
        EnvironmentMap environment = EnvironmentMapLoader.load(
                path(LittlestTokyoExample.class.getResource(ENVIRONMENT_RESOURCE), ENVIRONMENT_RESOURCE));
        Scene scene = loaded.scene();
        scene.setEnvironment(environment);
        scene.setBackgroundEnvironment(environment);
        scene.setEnvironmentIntensity(1.0f);
        scene.setBackgroundIntensity(0.34f);

        Object3D model = scene.children().getFirst();
        model.setPosition(1.0f, 1.0f, 0.0f);
        model.setScale(0.01f, 0.01f, 0.01f);

        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(loaded.animations().getFirst())
                .setLoopMode(LoopMode.REPEAT)
                .play();
        PlaybackSettings settings = new PlaybackSettings(action, scene, environment);

        PerspectiveCamera camera =
                new PerspectiveCamera((float) Math.toRadians(40.0), context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(5.0f, 2.0f, 8.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.7f, 0.0f);
        controls.setDistanceLimits(3.0f, 22.0f);
        controls.setDampingEnabled(true);
        controls.update();

        Renderer renderer = context.renderer();
        RendererSettingsScope rendererSettings = RendererSettingsScope.capture(renderer);
        renderer.setToneMapping(ToneMapping.ACES_FILMIC);
        renderer.setExposure(1.0f);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(rendererSettings);
        example.own(environment);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, settings, renderer));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            mixer.update(frame.elapsedSeconds());
            panel.update();
            fps.update();
        });
        return example;
    }

    /** Creates playback and presentation controls without exposing importer internals. */
    private static ControlPanel createPanel(ExampleContext context, PlaybackSettings settings, Renderer renderer) {
        ControlPanel panel = new ControlPanel(context.window(), "Littlest Tokyo");
        ControlPanel.Section playback = panel.addSection("Animation");
        playback.addBoolean("playing", settings::playing, settings::setPlaying);
        playback.addFloat("speed", settings::speed, settings::setSpeed, -2.0f, 3.0f);
        playback.addButton("restart", settings::restart);
        ControlPanel.Section presentation = panel.addSection("Environment");
        presentation.addBoolean("background", settings::backgroundVisible, settings::setBackgroundVisible);
        presentation.addFloat("rotation", settings::rotation, settings::setRotation, -PI, PI);
        presentation.addFloat("exposure", renderer::exposure, renderer::setExposure, 0.2f, 3.0f);
        return panel;
    }

    /** Mutable synchronized bindings for one imported action and environment. */
    private static final class PlaybackSettings {
        private final AnimationAction action;
        private final Scene scene;
        private final EnvironmentMap environment;
        private float rotation;

        /** Retains non-owning references to the controlled animation and scene. */
        private PlaybackSettings(AnimationAction action, Scene scene, EnvironmentMap environment) {
            this.action = action;
            this.scene = scene;
            this.environment = environment;
        }

        /** Returns whether playback is advancing. */
        private boolean playing() {
            return action.isRunning() && !action.isPaused();
        }

        /** Starts or pauses playback. */
        private void setPlaying(boolean playing) {
            if (playing) {
                action.play();
            } else {
                action.pause();
            }
        }

        /** Returns the playback multiplier. */
        private float speed() {
            return action.timeScale();
        }

        /** Changes the playback multiplier. */
        private void setSpeed(float speed) {
            action.setTimeScale(speed);
        }

        /** Restarts the imported loop. */
        private void restart() {
            action.reset().play();
        }

        /** Returns whether the environment is drawn behind the model. */
        private boolean backgroundVisible() {
            return scene.backgroundEnvironment() != null;
        }

        /** Shows or hides the environment background without changing image-based lighting. */
        private void setBackgroundVisible(boolean visible) {
            scene.setBackgroundEnvironment(visible ? environment : null);
        }

        /** Returns environment rotation in radians. */
        private float rotation() {
            return rotation;
        }

        /** Rotates lighting and background together. */
        private void setRotation(float rotation) {
            this.rotation = rotation;
            scene.setEnvironmentRotation(0.0f, rotation, 0.0f);
        }
    }
}
