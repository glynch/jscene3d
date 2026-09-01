/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.animation.QuaternionKeyframeTrack;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Bone;
import io.github.glynch.jscene3d.objects.Skeleton;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Demonstrates GPU linear-blend skinning with two ordinary animated Java scene nodes. */
public final class SkeletalAnimationExample {
    /** Prevents instantiation of this example entry point. */
    private SkeletalAnimationExample() {
        throw new AssertionError("SkeletalAnimationExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Skeletal Animation", SkeletalAnimationExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = ribbonGeometry();
        StandardMaterial material = new StandardMaterial(Color.srgb(0x20d6ff));
        material.setMetalness(0.1f);
        material.setRoughness(0.32f);

        Bone root = new Bone();
        Bone tip = new Bone();
        tip.setPosition(0.0f, 1.5f, 0.0f);
        root.add(tip);
        Skeleton skeleton = Skeleton.fromCurrentPose(List.of(root, tip));
        SkinnedMesh ribbon = new SkinnedMesh(geometry, material, skeleton);
        ribbon.add(root);
        ribbon.setPosition(0.0f, -1.5f, 0.0f);

        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x060a14));
        scene.add(new AmbientLight(Color.srgb(0x6078a8), 0.35f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffe4c8), 3.0f);
        key.setPosition(-4.0f, 6.0f, 6.0f);
        scene.add(key);
        scene.add(ribbon);

        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action =
                mixer.action(bendingClip(tip)).setLoopMode(LoopMode.PING_PONG).play();
        PlaybackSettings settings = new PlaybackSettings(action);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(4.5f, 2.8f, 7.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.4f, 0.0f);
        controls.setDistanceLimits(4.0f, 16.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(material);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
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

    /** Creates a subdivided ribbon with a smooth two-joint influence transition. */
    private static BufferGeometry ribbonGeometry() {
        float[] positions = {
            -0.7f, 0.0f, 0.0f, 0.7f, 0.0f, 0.0f,
            -0.7f, 1.0f, 0.0f, 0.7f, 1.0f, 0.0f,
            -0.7f, 2.0f, 0.0f, 0.7f, 2.0f, 0.0f,
            -0.7f, 3.0f, 0.0f, 0.7f, 3.0f, 0.0f
        };
        float[] normals = new float[positions.length];
        for (int vertex = 0; vertex < positions.length / 3; vertex++) {
            normals[vertex * 3 + 2] = 1.0f;
        }
        float[] joints = new float[(positions.length / 3) * 4];
        float[] weights = new float[joints.length];
        for (int vertex = 0; vertex < positions.length / 3; vertex++) {
            float height = positions[vertex * 3 + 1];
            int offset = vertex * 4;
            joints[offset + 1] = 1.0f;
            float tipWeight = Math.clamp(height / 3.0f, 0.0f, 1.0f);
            weights[offset] = 1.0f - tipWeight;
            weights[offset + 1] = tipWeight;
        }
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(positions, 3));
        geometry.setAttribute(BufferGeometry.NORMAL, BufferAttribute.of(normals, 3));
        geometry.setAttribute(BufferGeometry.JOINTS, BufferAttribute.of(joints, 4));
        geometry.setAttribute(BufferGeometry.WEIGHTS, BufferAttribute.of(weights, 4));
        geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2, 2, 1, 3, 2, 3, 4, 4, 3, 5, 4, 5, 6, 6, 5, 7}));
        return geometry;
    }

    /** Creates a repeated side-to-side bend bound directly to the tip bone. */
    private static AnimationClip bendingClip(Bone tip) {
        float halfAngle = PI_OVER_FOUR * 0.5f;
        float sine = (float) Math.sin(halfAngle);
        float cosine = (float) Math.cos(halfAngle);
        return new AnimationClip(
                "Two-bone bend",
                List.of(QuaternionKeyframeTrack.rotation(
                        tip,
                        new float[] {0.0f, 1.0f, 2.0f},
                        new float[] {0.0f, 0.0f, -sine, cosine, 0.0f, 0.0f, sine, cosine, 0.0f, 0.0f, -sine, cosine},
                        Interpolation.LINEAR)));
    }

    /** Creates compact playback controls for the programmatic skeleton. */
    private static ControlPanel createPanel(ExampleContext context, PlaybackSettings settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Skeletal Animation");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addBoolean("playing", settings::playing, settings::setPlaying);
        playback.addFloat("speed", settings::speed, settings::setSpeed, -2.0f, 3.0f);
        playback.addText("joints", () -> "2");
        playback.addText("influences", () -> "4 per vertex");
        return panel;
    }

    /** Explicit GUI adapter for the example's single animation action. */
    private record PlaybackSettings(AnimationAction action) {
        /** Returns whether the action is advancing. */
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
    }
}
