/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.fogs.ExponentialSquaredFog;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;
import org.joml.Matrix4f;

/** Demonstrates independent morph weights across one instanced draw. */
public final class InstancedMorphTargetsExample {
    private static final int GRID_SIZE = 16;
    private static final int INSTANCE_COUNT = GRID_SIZE * GRID_SIZE;
    private static final List<Color> PALETTE = List.of(
            Color.srgb(0x19d3ff),
            Color.srgb(0xff4f9a),
            Color.srgb(0xffc857),
            Color.srgb(0x7cf29c),
            Color.srgb(0xa78bfa));

    private InstancedMorphTargetsExample() {
        throw new AssertionError("InstancedMorphTargetsExample cannot be instantiated");
    }

    /**
     * Opens the standalone instanced morph-target example.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Instanced Morph Targets", InstancedMorphTargetsExample::create);
    }

    /** Creates the hosted implementation used by standalone and browser modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = MorphExampleGeometry.create();
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setSpecular(Color.WHITE);
        material.setShininess(70.0f);
        InstancedMorphField field = new InstancedMorphField(geometry, material);

        Color atmosphere = Color.srgb(0x07111f);
        Scene scene = new Scene();
        scene.setBackground(atmosphere);
        scene.setFog(new ExponentialSquaredFog(atmosphere, 0.018f));
        scene.add(field.mesh());
        scene.add(new HemisphereLight(Color.srgb(0xbfeaff), Color.srgb(0x18251d), 1.6f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffd8b0), 2.8f);
        key.setPosition(-9.0f, 15.0f, 8.0f);
        scene.add(key);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(14.0f, 15.0f, 19.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(10.0f, 55.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(material);
        ControlPanel panel = example.addOverlay(panel(context, field, controls));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            field.update(frame.elapsedSeconds());
            panel.update();
            fps.update();
        });
        return example;
    }

    /** Creates controls that make the independent weight-wave behavior explicit. */
    private static ControlPanel panel(ExampleContext context, InstancedMorphField field, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Instanced Morphing");
        ControlPanel.Section deformation = panel.addSection("Deformation wave");
        deformation.addBoolean("animate", field::animated, field::setAnimated);
        deformation.addFloat("speed", field::speed, field::setSpeed, 0.0f, 3.0f);
        deformation.addFloat("amplitude", field::amplitude, field::setAmplitude, 0.0f, 1.0f);
        ControlPanel.Section renderer = panel.addSection("Renderer");
        renderer.addText("instances", () -> Integer.toString(INSTANCE_COUNT));
        renderer.addText(
                "draw calls",
                () -> Integer.toString(context.renderer().info().statistics().drawCalls()));
        renderer.addText(
                "morph resources",
                () -> Integer.toString(context.renderer().info().resources().activeMorphResources()));
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Owns one fixed batch and advances its capacity-major weight texture. */
    private static final class InstancedMorphField {
        private final InstancedMesh mesh;
        private final Matrix4f transform = new Matrix4f();
        private float elapsed;
        private float speed = 1.0f;
        private float amplitude = 1.0f;
        private boolean animated = true;

        private InstancedMorphField(BufferGeometry geometry, PhongMaterial material) {
            mesh = new InstancedMesh(geometry, material, INSTANCE_COUNT);
            for (int index = 0; index < INSTANCE_COUNT; index++) {
                int row = index / GRID_SIZE;
                int column = index % GRID_SIZE;
                float x = (column - (GRID_SIZE - 1) * 0.5f) * 1.35f;
                float z = (row - (GRID_SIZE - 1) * 0.5f) * 1.35f;
                mesh.setMatrixAt(index, transform.translation(x, 0.0f, z).scale(0.48f));
                mesh.setColorAt(index, PALETTE.get((row + column) % PALETTE.size()));
            }
            updateWeights();
        }

        private InstancedMesh mesh() {
            return mesh;
        }

        private boolean animated() {
            return animated;
        }

        private void setAnimated(boolean animated) {
            this.animated = animated;
        }

        private float speed() {
            return speed;
        }

        private void setSpeed(float speed) {
            this.speed = speed;
        }

        private float amplitude() {
            return amplitude;
        }

        private void setAmplitude(float amplitude) {
            this.amplitude = amplitude;
            updateWeights();
        }

        private void update(float elapsedSeconds) {
            if (!animated) {
                return;
            }
            elapsed += elapsedSeconds * speed;
            updateWeights();
        }

        /** Writes three phase-shifted waves into independent instance weight rows. */
        private void updateWeights() {
            for (int index = 0; index < INSTANCE_COUNT; index++) {
                int row = index / GRID_SIZE;
                int column = index % GRID_SIZE;
                float phase = elapsed * 2.0f + row * 0.36f + column * 0.29f;
                mesh.setMorphTargetInfluenceAt(index, 0, wave(phase));
                mesh.setMorphTargetInfluenceAt(index, 1, wave(phase + 2.094f));
                mesh.setMorphTargetInfluenceAt(index, 2, wave(phase + 4.189f));
            }
        }

        private float wave(float phase) {
            return amplitude * (0.5f + 0.5f * (float) Math.sin(phase));
        }
    }
}
