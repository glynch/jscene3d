/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Cross-fades a rigged Fox model between idle, walking, and running skeletal clips. */
public final class AnimationBlendingExample {
    private static final Color BACKGROUND = Color.srgb(0x0a0f18);
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/fox/Fox.glb";
    private static final String ENVIRONMENT_RESOURCE =
            "/io/github/glynch/jscene3d/examples/environment/studio_small_08_1k.hdr";
    private static final List<ControlPanel.Choice<Motion>> MOTION_CHOICES = List.of(
            new ControlPanel.Choice<>(Motion.IDLE, "idle"),
            new ControlPanel.Choice<>(Motion.WALK, "walk"),
            new ControlPanel.Choice<>(Motion.RUN, "run"));

    /** Prevents instantiation of this example entry point. */
    private AnimationBlendingExample() {
        throw new AssertionError("AnimationBlendingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Animation Blending", AnimationBlendingExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded =
                GltfLoader.load(path(AnimationBlendingExample.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE));
        EnvironmentMap environmentMap = EnvironmentMapLoader.load(
                path(AnimationBlendingExample.class.getResource(ENVIRONMENT_RESOURCE), ENVIRONMENT_RESOURCE));
        Scene scene = loaded.scene();
        scene.setBackground(BACKGROUND);
        scene.setEnvironment(environmentMap);
        scene.setEnvironmentIntensity(0.8f);

        Object3D model = scene.children().getFirst();
        model.setScale(0.025f, 0.025f, 0.025f);
        model.rotateY(PI);

        AnimationMixer mixer = new AnimationMixer();
        BlendState settings = new BlendState(mixer, loaded.animations());

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.05f, 100.0f);
        camera.setPosition(2.2f, 1.55f, 3.5f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.85f, 0.0f);
        controls.setDistanceLimits(1.8f, 14.0f);
        controls.setDampingEnabled(true);
        controls.update();

        Renderer renderer = context.renderer();
        RendererSettingsScope rendererSettings = RendererSettingsScope.capture(renderer);
        renderer.setToneMapping(ToneMapping.ACES_FILMIC);
        renderer.setExposure(1.0f);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(rendererSettings);
        example.own(environmentMap);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            settings.update(frame.elapsedSeconds());
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates live controls for clip selection, transition timing, and playback. */
    private static ControlPanel createPanel(ExampleContext context, BlendState settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Animation Blending");
        ControlPanel.Section transition = panel.addSection("Transition");
        transition.addRadioGroup("motion", settings::motion, settings::setMotion, MOTION_CHOICES);
        transition.addFloat("cross-fade", settings::crossFadeDuration, settings::setCrossFadeDuration, 0.0f, 2.0f);
        transition.addBoolean("automatic", settings::automatic, settings::setAutomatic);
        transition.addButton("next motion", settings::nextMotion);
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addFloat("speed", settings::speed, settings::setSpeed, 0.0f, 2.0f);
        playback.addText("idle weight", () -> formatWeight(settings.weight(Motion.IDLE)));
        playback.addText("walk weight", () -> formatWeight(settings.weight(Motion.WALK)));
        playback.addText("run weight", () -> formatWeight(settings.weight(Motion.RUN)));
        return panel;
    }

    /** Formats one live action weight compactly for the control panel. */
    private static String formatWeight(float weight) {
        return String.format(Locale.ROOT, "%.2f", weight);
    }

    /** User-facing movement states and their corresponding imported clip names. */
    private enum Motion {
        IDLE("Survey"),
        WALK("Walk"),
        RUN("Run");

        private final String clipName;

        /** Retains the exact imported clip name. */
        Motion(String clipName) {
            this.clipName = clipName;
        }
    }

    /** Owns the mixer actions and state-machine transitions displayed by the example. */
    private static final class BlendState {
        private static final float AUTOMATIC_HOLD_SECONDS = 3.5f;

        private final AnimationMixer mixer;
        private final EnumMap<Motion, AnimationAction> actions = new EnumMap<>(Motion.class);

        private Motion motion = Motion.IDLE;
        private float crossFadeDuration = 0.55f;
        private float automaticElapsed;
        private boolean automatic = true;

        /** Creates one repeating action per required clip and starts the idle presentation. */
        private BlendState(AnimationMixer mixer, List<AnimationClip> clips) {
            this.mixer = mixer;
            for (Motion candidate : Motion.values()) {
                AnimationClip clip = requireClip(clips, candidate.clipName);
                actions.put(candidate, mixer.action(clip).setLoopMode(LoopMode.REPEAT));
            }
            action(Motion.IDLE).play();
        }

        /** Advances animation and periodically selects the next movement when enabled. */
        private void update(float elapsedSeconds) {
            mixer.update(elapsedSeconds);
            if (!automatic) {
                return;
            }
            automaticElapsed += elapsedSeconds;
            if (automaticElapsed >= AUTOMATIC_HOLD_SECONDS) {
                nextMotion();
            }
        }

        /** Returns the selected movement. */
        private Motion motion() {
            return motion;
        }

        /** Cross-fades to a newly selected movement without restarting the current movement. */
        private void setMotion(Motion selected) {
            automaticElapsed = 0.0f;
            if (selected == motion) {
                return;
            }
            AnimationAction source = action(motion);
            AnimationAction destination = action(selected);
            mixer.crossFade(source, destination, crossFadeDuration);
            motion = selected;
        }

        /** Selects the next movement in idle, walk, run order. */
        private void nextMotion() {
            Motion[] motions = Motion.values();
            setMotion(motions[(motion.ordinal() + 1) % motions.length]);
        }

        /** Returns the configured transition duration in seconds. */
        private float crossFadeDuration() {
            return crossFadeDuration;
        }

        /** Changes the duration used by subsequent transitions. */
        private void setCrossFadeDuration(float duration) {
            crossFadeDuration = duration;
        }

        /** Returns whether automatic movement changes are enabled. */
        private boolean automatic() {
            return automatic;
        }

        /** Enables or disables automatic movement changes and restarts the hold interval. */
        private void setAutomatic(boolean automatic) {
            this.automatic = automatic;
            automaticElapsed = 0.0f;
        }

        /** Returns the common playback-rate multiplier. */
        private float speed() {
            return action(Motion.IDLE).timeScale();
        }

        /** Applies one playback-rate multiplier to every movement action. */
        private void setSpeed(float speed) {
            actions.values().forEach(action -> action.setTimeScale(speed));
        }

        /** Returns one movement's current post-fade contribution. */
        private float weight(Motion candidate) {
            return action(candidate).effectiveWeight();
        }

        /** Returns the required action registered for one movement. */
        private AnimationAction action(Motion candidate) {
            AnimationAction action = actions.get(candidate);
            if (action == null) {
                throw new IllegalStateException("Missing animation action: " + candidate);
            }
            return action;
        }

        /** Finds one required clip by its exact imported name. */
        private static AnimationClip requireClip(List<AnimationClip> clips, String name) {
            return clips.stream()
                    .filter(clip -> clip.name().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing required animation clip: " + name));
        }
    }
}
