/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.animation.QuaternionKeyframeTrack;
import io.github.glynch.jscene3d.animation.Vector3KeyframeTrack;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Compares step, linear, and cubic-spline keyframe interpolation in one animated scene. */
public final class KeyframeAnimationExample {
    private static final float DURATION = 4.0f;
    private static final float[] TIMES = {0.0f, 1.0f, 2.0f, 3.0f, DURATION};
    private static final List<ControlPanel.Choice<LoopMode>> LOOP_CHOICES = List.of(
            new ControlPanel.Choice<>(LoopMode.ONCE, "once"),
            new ControlPanel.Choice<>(LoopMode.REPEAT, "repeat"),
            new ControlPanel.Choice<>(LoopMode.PING_PONG, "ping-pong"));

    /** Prevents instantiation of this example entry point. */
    private KeyframeAnimationExample() {
        throw new AssertionError("KeyframeAnimationExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Keyframe Animation", KeyframeAnimationExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        Scene scene = createScene();
        BufferGeometry boxGeometry = BoxGeometry.create(1.3f, 1.3f, 1.3f);
        BufferGeometry sphereGeometry = SphereGeometry.create(0.78f, 40, 20);
        BufferGeometry torusGeometry = TorusGeometry.create(0.78f, 0.25f, 24, 48);
        BufferGeometry pedestalGeometry = CylinderGeometry.create(1.05f, 0.28f);
        PhongMaterial stepMaterial = material(Color.srgb(0xff3d9a));
        PhongMaterial linearMaterial = material(Color.srgb(0x20d6ff));
        PhongMaterial cubicMaterial = material(Color.srgb(0xffc928));
        LambertMaterial pedestalMaterial = new LambertMaterial(Color.srgb(0x263247));

        Mesh stepMesh = new Mesh(boxGeometry, stepMaterial);
        Mesh linearMesh = new Mesh(sphereGeometry, linearMaterial);
        Mesh cubicMesh = new Mesh(torusGeometry, cubicMaterial);
        addPedestal(scene, pedestalGeometry, pedestalMaterial, -3.2f);
        addPedestal(scene, pedestalGeometry, pedestalMaterial, 0.0f);
        addPedestal(scene, pedestalGeometry, pedestalMaterial, 3.2f);
        scene.add(stepMesh);
        scene.add(linearMesh);
        scene.add(cubicMesh);

        AnimationClip clip = createClip(stepMesh, linearMesh, cubicMesh);
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(clip).setLoopMode(LoopMode.REPEAT).play();

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(5.5f, 4.0f, 8.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.6f, 0.0f);
        controls.setDistanceLimits(7.0f, 25.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(boxGeometry);
        example.own(sphereGeometry);
        example.own(torusGeometry);
        example.own(pedestalGeometry);
        example.own(stepMaterial);
        example.own(linearMaterial);
        example.own(cubicMaterial);
        example.own(pedestalMaterial);
        PlaybackSettings settings = new PlaybackSettings(action);
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

    /** Creates the dark gallery scene and balanced direct lighting. */
    private static Scene createScene() {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x060a14));
        scene.add(new AmbientLight(Color.srgb(0x6680b8), 0.22f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffe6ca), 2.5f);
        key.setPosition(-5.0f, 8.0f, 7.0f);
        scene.add(key);
        DirectionalLight rim = new DirectionalLight(Color.srgb(0x598cff), 1.2f);
        rim.setPosition(6.0f, 3.0f, -5.0f);
        scene.add(rim);
        return scene;
    }

    /** Creates a polished colored surface for one animated object. */
    private static PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecular(Color.WHITE);
        material.setShininess(90.0f);
        return material;
    }

    /** Adds one static base beneath an interpolation sample. */
    private static void addPedestal(
            Scene scene, BufferGeometry geometry, LambertMaterial material, float horizontalPosition) {
        Mesh pedestal = new Mesh(geometry, material);
        pedestal.setPosition(horizontalPosition, 0.0f, 0.0f);
        scene.add(pedestal);
    }

    /** Builds one clip with direct typed bindings to the three animated meshes. */
    private static AnimationClip createClip(Mesh stepMesh, Mesh linearMesh, Mesh cubicMesh) {
        float[] heights = {1.1f, 3.2f, 1.7f, 3.7f, 1.1f};
        return new AnimationClip(
                "Interpolation comparison",
                List.of(
                        Vector3KeyframeTrack.position(
                                stepMesh, TIMES, positionValues(-3.2f, heights), Interpolation.STEP),
                        Vector3KeyframeTrack.position(
                                linearMesh, TIMES, positionValues(0.0f, heights), Interpolation.LINEAR),
                        Vector3KeyframeTrack.position(
                                cubicMesh, TIMES, cubicPositionValues(3.2f, heights), Interpolation.CUBIC_SPLINE),
                        QuaternionKeyframeTrack.rotation(
                                stepMesh,
                                TIMES,
                                rotationValues(0.0f, PI * 0.5f, PI, PI * 1.5f, PI * 2.0f),
                                Interpolation.STEP),
                        QuaternionKeyframeTrack.rotation(
                                cubicMesh,
                                TIMES,
                                rotationValues(0.0f, PI * 0.5f, PI, PI * 1.5f, PI * 2.0f),
                                Interpolation.LINEAR)));
    }

    /** Flattens fixed-X positions for an ordinary vector track. */
    private static float[] positionValues(float horizontalPosition, float[] heights) {
        float[] values = new float[heights.length * 3];
        for (int key = 0; key < heights.length; key++) {
            int offset = key * 3;
            values[offset] = horizontalPosition;
            values[offset + 1] = heights[key];
            values[offset + 2] = 0.0f;
        }
        return values;
    }

    /** Flattens zero-tangent Hermite positions for a smooth eased vector track. */
    private static float[] cubicPositionValues(float horizontalPosition, float[] heights) {
        int groupsPerKey = 3;
        int components = 3;
        float[] values = new float[heights.length * groupsPerKey * components];
        for (int key = 0; key < heights.length; key++) {
            int valueOffset = (key * groupsPerKey + 1) * components;
            values[valueOffset] = horizontalPosition;
            values[valueOffset + 1] = heights[key];
            values[valueOffset + 2] = 0.0f;
        }
        return values;
    }

    /** Converts Y-axis angles into flat XYZW quaternion keyframes. */
    private static float[] rotationValues(float... angles) {
        float[] values = new float[angles.length * 4];
        for (int key = 0; key < angles.length; key++) {
            float halfAngle = angles[key] * 0.5f;
            int offset = key * 4;
            values[offset] = 0.0f;
            values[offset + 1] = (float) Math.sin(halfAngle);
            values[offset + 2] = 0.0f;
            values[offset + 3] = (float) Math.cos(halfAngle);
        }
        return values;
    }

    /** Creates live playback controls bound directly to the animation action. */
    private static ControlPanel createPanel(ExampleContext context, PlaybackSettings settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Keyframe Animation");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addBoolean("playing", settings::isPlaying, settings::setPlaying);
        playback.addFloat("time", settings::time, settings::setTime, 0.0f, DURATION);
        playback.addFloat("speed", settings::speed, settings::setSpeed, -2.0f, 3.0f);
        playback.addChoice("loop", settings::loopMode, settings::setLoopMode, LOOP_CHOICES);
        playback.addButton("restart", settings::restart);
        playback.addButton("stop", settings::stop);
        ControlPanel.Section guide = panel.addSection("Interpolation");
        guide.addText("left", () -> "step");
        guide.addText("centre", () -> "linear");
        guide.addText("right", () -> "cubic spline");
        return panel;
    }

    /** Explicit GUI adapter around one animation action. */
    private static final class PlaybackSettings {
        private final AnimationAction action;

        /** Retains the action controlled by the panel. */
        private PlaybackSettings(AnimationAction action) {
            this.action = action;
        }

        /** Returns whether the action is currently advancing. */
        private boolean isPlaying() {
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

        /** Returns the current local time. */
        private float time() {
            return action.time();
        }

        /** Seeks to one local time. */
        private void setTime(float time) {
            action.setTime(time);
        }

        /** Returns the playback rate. */
        private float speed() {
            return action.timeScale();
        }

        /** Changes the playback rate. */
        private void setSpeed(float speed) {
            action.setTimeScale(speed);
        }

        /** Returns the active endpoint behavior. */
        private LoopMode loopMode() {
            return action.loopMode();
        }

        /** Changes the endpoint behavior. */
        private void setLoopMode(LoopMode loopMode) {
            action.setLoopMode(loopMode);
        }

        /** Returns to the initial pose and starts playback. */
        private void restart() {
            action.reset().play();
        }

        /** Stops playback at the initial pose. */
        private void stop() {
            action.stop();
        }
    }
}
