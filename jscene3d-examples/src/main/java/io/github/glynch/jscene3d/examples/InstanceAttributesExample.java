/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;
import org.joml.Matrix4f;

/** Demonstrates custom scalar and vector inputs changing every instance in one draw call. */
public final class InstanceAttributesExample {
    private static final int GRID_SIZE = 16;
    private static final int INSTANCE_COUNT = GRID_SIZE * GRID_SIZE;
    private static final List<Color> PALETTE = List.of(
            Color.srgb(0x00d9ff),
            Color.srgb(0xff4f9a),
            Color.srgb(0xffc857),
            Color.srgb(0x7cf29c),
            Color.srgb(0xa78bfa));
    private static final String VERTEX_SHADER = """
            in vec3 position;
            in vec4 instanceMatrixColumn0;
            in vec4 instanceMatrixColumn1;
            in vec4 instanceMatrixColumn2;
            in vec4 instanceMatrixColumn3;
            in float phase;
            in float baseScale;
            in vec3 instanceTint;

            uniform mat4 modelViewMatrix;
            uniform mat4 projectionMatrix;
            uniform float time;
            uniform float amplitude;

            out vec3 vertexTint;
            out float heightShade;

            void main() {
                mat4 instanceMatrix = mat4(
                        instanceMatrixColumn0,
                        instanceMatrixColumn1,
                        instanceMatrixColumn2,
                        instanceMatrixColumn3);
                float pulse = baseScale * (1.0 + amplitude * sin(time + phase));
                vec3 animatedPosition = position * vec3(pulse, 0.65 + pulse, pulse);
                vertexTint = instanceTint;
                heightShade = 0.72 + 0.28 * (position.y + 0.5);
                gl_Position = projectionMatrix * modelViewMatrix * instanceMatrix * vec4(animatedPosition, 1.0);
            }
            """;
    private static final String FRAGMENT_SHADER = """
            in vec3 vertexTint;
            in float heightShade;
            out vec4 fragmentColor;
            void main() {
                fragmentColor = vec4(vertexTint * heightShade, 1.0);
            }
            """;

    /** Prevents instantiation of this example entry point. */
    private InstanceAttributesExample() {
        throw new AssertionError("InstanceAttributesExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Custom Instance Attributes", InstanceAttributesExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(0.72f, 0.72f, 0.72f);
        ShaderMaterial material = createMaterial();
        InstancedMesh mesh = createInstances(geometry, material);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x07111f));
        scene.add(mesh);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 80.0f);
        camera.setPosition(11.0f, 13.0f, 16.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(8.0f, 40.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(material);
        Settings settings = new Settings();
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            settings.time = (settings.time + frame.elapsedSeconds() * settings.speed) % TWO_PI;
            material.setUniform("time", settings.time);
            material.setUniform("amplitude", settings.amplitude);
            panel.update();
            fps.update();
        });
        return example;
    }

    /** Creates one shader declaring all renderer-managed and custom instance inputs. */
    private static ShaderMaterial createMaterial() {
        ShaderMaterial material = ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                .requireInstanceAttribute("phase", 1)
                .requireInstanceAttribute("baseScale", 1)
                .requireInstanceAttribute("instanceTint", 3)
                .build();
        material.setUniform("time", 0.0f);
        material.setUniform("amplitude", 0.28f);
        return material;
    }

    /** Populates the fixed transforms and three custom input streams. */
    private static InstancedMesh createInstances(BufferGeometry geometry, ShaderMaterial material) {
        InstancedMesh mesh = new InstancedMesh(geometry, material, INSTANCE_COUNT);
        float[] phases = new float[INSTANCE_COUNT];
        float[] scales = new float[INSTANCE_COUNT];
        float[] tints = new float[INSTANCE_COUNT * 3];
        Matrix4f transform = new Matrix4f();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                int index = row * GRID_SIZE + column;
                float x = column - (GRID_SIZE - 1) * 0.5f;
                float z = row - (GRID_SIZE - 1) * 0.5f;
                mesh.setMatrixAt(
                        index, transform.identity().translation(x, 0.0f, z).rotateY((row + column) * 0.17f));
                phases[index] = (row * 0.42f + column * 0.31f) % TWO_PI;
                scales[index] = 0.72f + 0.18f * (float) Math.sin(row * 0.7f + column * 0.45f);
                Color color = PALETTE.get((row + column) % PALETTE.size());
                int colorOffset = index * 3;
                tints[colorOffset] = color.red();
                tints[colorOffset + 1] = color.green();
                tints[colorOffset + 2] = color.blue();
            }
        }
        mesh.setInstanceAttribute("phase", BufferAttribute.of(phases, 1));
        mesh.setInstanceAttribute("baseScale", BufferAttribute.of(scales, 1));
        mesh.setInstanceAttribute("instanceTint", BufferAttribute.of(tints, 3));
        return mesh;
    }

    /** Creates live controls for the uniform values shared by the complete batch. */
    private static ControlPanel createPanel(ExampleContext context, Settings settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Custom Instance Attributes");
        ControlPanel.Section animation = panel.addSection("Animation");
        animation.addFloat("speed", () -> settings.speed, value -> settings.speed = value, 0.0f, 3.0f);
        animation.addFloat("amplitude", () -> settings.amplitude, value -> settings.amplitude = value, 0.0f, 0.55f);
        panel.addSection("Batch").addText("drawn instances", () -> Integer.toString(INSTANCE_COUNT));
        return panel;
    }

    /** Mutable GUI bindings for the example's two shared uniforms. */
    private static final class Settings {
        private float speed = 1.0f;
        private float amplitude = 0.28f;
        private float time;
    }
}
