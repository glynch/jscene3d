/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.animation.MorphTargetKeyframeTrack;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Demonstrates named morph targets driven by a vector animation track. */
public final class MorphTargetsExample {
    private static final float DURATION = 8.0f;

    private MorphTargetsExample() {
        throw new AssertionError("MorphTargetsExample cannot be instantiated");
    }

    /**
     * Opens the standalone morph-target example.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Morph Targets", MorphTargetsExample::create);
    }

    /** Creates the hosted implementation used by standalone and browser modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = MorphExampleGeometry.create();
        PhongMaterial material = new PhongMaterial(Color.srgb(0x35d4ff));
        material.setSpecular(Color.WHITE);
        material.setShininess(96.0f);
        Mesh mesh = new Mesh(geometry, material);

        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x060a14));
        scene.add(mesh);
        scene.add(new HemisphereLight(Color.srgb(0xb9e9ff), Color.srgb(0x182036), 1.5f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffd5b5), 3.1f);
        key.setPosition(-4.0f, 6.0f, 5.0f);
        scene.add(key);

        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action =
                mixer.action(clip(mesh)).setLoopMode(LoopMode.REPEAT).play().setTime(1.6f);
        MorphSettings settings = new MorphSettings(mesh, action);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 50.0f);
        camera.setPosition(3.8f, 2.6f, 5.2f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(3.0f, 14.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(material);
        ControlPanel panel = example.addOverlay(panel(context, settings, controls));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            if (settings.playing()) {
                mixer.update(frame.elapsedSeconds());
            }
            mesh.rotateY(frame.elapsedSeconds() * 0.28f);
            panel.update();
            fps.update();
        });
        return example;
    }

    /** Creates one cyclic vector track whose components map to the geometry's ordered targets. */
    private static AnimationClip clip(Mesh mesh) {
        float[] times = {0.0f, 1.6f, 3.2f, 4.8f, 6.4f, DURATION};
        float[] weights = {
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            0.45f, 0.35f, 0.65f,
            0.0f, 0.0f, 0.0f
        };
        return new AnimationClip(
                "Morph cycle",
                List.of(MorphTargetKeyframeTrack.influences(mesh, times, weights, Interpolation.LINEAR)));
    }

    /** Creates playback and direct target-weight controls. */
    private static ControlPanel panel(ExampleContext context, MorphSettings settings, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Morph Targets");
        ControlPanel.Section animation = panel.addSection("Animation");
        animation.addBoolean("playing", settings::playing, settings::setPlaying);
        animation.addFloat("speed", settings::speed, settings::setSpeed, 0.0f, 2.0f);
        ControlPanel.Section weights = panel.addSection("Direct weights");
        weights.setEnabled(() -> !settings.playing());
        weights.addFloat("stretch", settings::stretch, settings::setStretch, 0.0f, 1.0f);
        weights.addFloat("flatten", settings::flatten, settings::setFlatten, 0.0f, 1.0f);
        weights.addFloat("twist", settings::twist, settings::setTwist, 0.0f, 1.0f);
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera and animation", () -> {
            settings.reset();
            controls.reset();
        });
        return panel;
    }

    /** Mutable panel-facing playback state for one animated mesh. */
    private static final class MorphSettings {
        private final Mesh mesh;
        private final AnimationAction action;
        private boolean playing = true;
        private float speed = 1.0f;

        private MorphSettings(Mesh mesh, AnimationAction action) {
            this.mesh = mesh;
            this.action = action;
        }

        private boolean playing() {
            return playing;
        }

        private void setPlaying(boolean playing) {
            this.playing = playing;
        }

        private float speed() {
            return speed;
        }

        private void setSpeed(float speed) {
            this.speed = speed;
            action.setTimeScale(speed);
        }

        private float stretch() {
            return mesh.morphTargetInfluence(0);
        }

        private void setStretch(float value) {
            mesh.setMorphTargetInfluence(0, value);
        }

        private float flatten() {
            return mesh.morphTargetInfluence(1);
        }

        private void setFlatten(float value) {
            mesh.setMorphTargetInfluence(1, value);
        }

        private float twist() {
            return mesh.morphTargetInfluence(2);
        }

        private void setTwist(float value) {
            mesh.setMorphTargetInfluence(2, value);
        }

        private void reset() {
            playing = true;
            speed = 1.0f;
            action.setTimeScale(1.0f).reset().play();
        }
    }
}
