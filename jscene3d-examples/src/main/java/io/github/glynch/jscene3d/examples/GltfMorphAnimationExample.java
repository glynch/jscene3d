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
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayDeque;
import java.util.Locale;

/** Plays the official Khronos Morph Stress Test glTF asset end to end. */
public final class GltfMorphAnimationExample {
    private static final String MODEL_RESOURCE =
            "/io/github/glynch/jscene3d/examples/morph-stress-test/MorphStressTest.glb";

    /** Prevents instantiation of this example entry point. */
    private GltfMorphAnimationExample() {
        throw new AssertionError("GltfMorphAnimationExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - glTF Morph Animation", GltfMorphAnimationExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded = GltfLoader.load(path(GltfMorphAnimationExample.class, MODEL_RESOURCE));
        loaded.scene().setBackground(Color.srgb(0x080b12));
        loaded.scene().add(new AmbientLight(Color.srgb(0x8ca6d7), 0.45f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffe1bd), 3.0f);
        key.setPosition(-4.0f, 6.0f, 7.0f);
        loaded.scene().add(key);

        AnimationClip clip = requireAnimation(loaded, "TheWave");
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(clip).setLoopMode(LoopMode.REPEAT).play();
        Mesh morphMesh = requireMorphMesh(loaded.scene());

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(2.8f, 2.0f, 4.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(2.0f, 16.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, loaded.scene(), camera, controls);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, action, morphMesh));
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

    /** Creates live playback and imported-target diagnostics. */
    private static ControlPanel createPanel(ExampleContext context, AnimationAction action, Mesh mesh) {
        ControlPanel panel = new ControlPanel(context.window(), "glTF Morph Animation");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addBoolean("playing", () -> action.isRunning() && !action.isPaused(), playing -> {
            if (playing) {
                action.play();
            } else {
                action.pause();
            }
        });
        playback.addFloat("speed", action::timeScale, action::setTimeScale, -2.0f, 3.0f);
        playback.addButton("restart", () -> action.reset().play());
        ControlPanel.Section imported = panel.addSection("Imported morph targets");
        for (int targetIndex = 0; targetIndex < mesh.morphTargetCount(); targetIndex++) {
            int index = targetIndex;
            String name = mesh.geometry().morphTargets().get(index).name();
            imported.addText(name, () -> String.format(Locale.ROOT, "%.2f", mesh.morphTargetInfluence(index)));
        }
        return panel;
    }

    /** Returns one required imported clip by exact source name. */
    private static AnimationClip requireAnimation(LoadedGltf loaded, String name) {
        return loaded.animations().stream()
                .filter(clip -> clip.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Morph Stress Test is missing animation: " + name));
    }

    /** Finds the first imported mesh carrying morph targets. */
    private static Mesh requireMorphMesh(Object3D root) {
        ArrayDeque<Object3D> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Object3D object = pending.removeFirst();
            if (object instanceof Mesh mesh && mesh.morphTargetCount() > 0) {
                return mesh;
            }
            pending.addAll(object.children());
        }
        throw new IllegalStateException("Morph Stress Test contains no morph-target mesh");
    }
}
