/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.math.Color;
import java.util.ArrayList;
import java.util.List;

/** Plays every transform-animation clip from the Khronos glTF Interpolation Test asset. */
public final class GltfAnimationExample {
    private static final String MODEL_RESOURCE =
            "/io/github/glynch/jscene3d/examples/interpolation-test/InterpolationTest.glb";
    private static final List<ControlPanel.Choice<LoopMode>> LOOP_CHOICES = List.of(
            new ControlPanel.Choice<>(LoopMode.ONCE, "once"),
            new ControlPanel.Choice<>(LoopMode.REPEAT, "repeat"),
            new ControlPanel.Choice<>(LoopMode.PING_PONG, "ping-pong"));

    /** Prevents instantiation of this example entry point. */
    private GltfAnimationExample() {
        throw new AssertionError("GltfAnimationExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - glTF Animation", GltfAnimationExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded =
                GltfLoader.load(path(GltfAnimationExample.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE));
        loaded.scene().setBackground(Color.srgb(0x060a14));
        loaded.scene().add(new AmbientLight(Color.srgb(0x8aa2d0), 0.3f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffe4c8), 2.6f);
        key.setPosition(-5.0f, 9.0f, 8.0f);
        loaded.scene().add(key);

        AnimationMixer mixer = new AnimationMixer();
        List<AnimationAction> actions = new ArrayList<>(loaded.animations().size());
        for (AnimationClip clip : loaded.animations()) {
            actions.add(mixer.action(clip).setLoopMode(LoopMode.REPEAT).play());
        }
        PlaybackSettings settings = new PlaybackSettings(actions);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(7.5f, 6.2f, 11.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 3.0f, 0.0f);
        controls.setDistanceLimits(10.0f, 35.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, loaded.scene(), camera, controls);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            mixer.update(frame.elapsedSeconds());
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates controls that apply consistently to every imported clip. */
    private static ControlPanel createPanel(ExampleContext context, PlaybackSettings settings) {
        ControlPanel panel = new ControlPanel(context.window(), "glTF Animation");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addBoolean("playing", settings::isPlaying, settings::setPlaying);
        playback.addFloat("time", settings::time, settings::setTime, 0.0f, settings.duration());
        playback.addFloat("speed", settings::speed, settings::setSpeed, -2.0f, 3.0f);
        playback.addSelect("loop", settings::loopMode, settings::setLoopMode, LOOP_CHOICES);
        playback.addButton("restart", settings::restart);
        playback.addButton("stop", settings::stop);
        ControlPanel.Section imported = panel.addSection("Imported clips");
        imported.addText("count", () -> Integer.toString(settings.clipCount()));
        imported.addText("columns", () -> "linear / step / cubic");
        imported.addText("rows", () -> "scale / rotation / position");
        return panel;
    }

    /** Explicit synchronized GUI adapter around the imported action set. */
    private static final class PlaybackSettings {
        private final List<AnimationAction> actions;
        private final float duration;

        /** Copies a required non-empty set of actions and records their shared maximum duration. */
        private PlaybackSettings(List<AnimationAction> actions) {
            this.actions = List.copyOf(actions);
            if (this.actions.isEmpty()) {
                throw new IllegalArgumentException("actions must not be empty");
            }
            float maximumDuration = 0.0f;
            for (AnimationAction action : this.actions) {
                maximumDuration = Math.max(maximumDuration, action.clip().duration());
            }
            duration = maximumDuration;
        }

        /** Returns the longest imported clip duration. */
        private float duration() {
            return duration;
        }

        /** Returns the imported clip count. */
        private int clipCount() {
            return actions.size();
        }

        /** Returns whether every imported action is currently advancing. */
        private boolean isPlaying() {
            return actions.stream().allMatch(action -> action.isRunning() && !action.isPaused());
        }

        /** Starts or pauses every imported action. */
        private void setPlaying(boolean playing) {
            actions.forEach(playing ? AnimationAction::play : AnimationAction::pause);
        }

        /** Returns the first action's current local time. */
        private float time() {
            return actions.getFirst().time();
        }

        /** Seeks every action, clamping to each clip's own duration. */
        private void setTime(float time) {
            actions.forEach(action ->
                    action.setTime(Math.clamp(time, 0.0f, action.clip().duration())));
        }

        /** Returns the common playback rate. */
        private float speed() {
            return actions.getFirst().timeScale();
        }

        /** Changes every action's playback rate. */
        private void setSpeed(float speed) {
            actions.forEach(action -> action.setTimeScale(speed));
        }

        /** Returns the common endpoint behavior. */
        private LoopMode loopMode() {
            return actions.getFirst().loopMode();
        }

        /** Changes every action's endpoint behavior. */
        private void setLoopMode(LoopMode loopMode) {
            actions.forEach(action -> action.setLoopMode(loopMode));
        }

        /** Returns every action to its initial pose and starts playback. */
        private void restart() {
            actions.forEach(action -> action.reset().play());
        }

        /** Stops every action at its initial pose. */
        private void stop() {
            actions.forEach(AnimationAction::stop);
        }
    }
}
