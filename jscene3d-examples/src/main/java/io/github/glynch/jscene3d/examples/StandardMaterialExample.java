/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayList;
import java.util.List;

/** Displays the metallic-roughness response of {@link StandardMaterial} across a sphere grid. */
public final class StandardMaterialExample {
    private static final int GRID_SIZE = 5;
    private static final float GRID_SPACING = 1.65f;
    private static final float MINIMUM_ROUGHNESS = 0.08f;

    /** Prevents instantiation of this example entry point. */
    private StandardMaterialExample() {
        throw new AssertionError("StandardMaterialExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Metalness increases from left to right and roughness decreases from bottom to top. Drag
     * with the left mouse button to orbit, drag with the right mouse button to pan, and use the
     * scroll wheel to dolly.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Standard Material", StandardMaterialExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = SphereGeometry.create(0.62f, 48, 24);
        List<StandardMaterial> materials = new ArrayList<>();
        Scene scene = createScene(geometry, materials);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 12.5f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(7.0f, 25.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        materials.forEach(example::own);
        ControlPanel panel = example.addOverlay(createPanel(context));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates the material grid and direct-lighting setup. */
    private static Scene createScene(BufferGeometry geometry, List<StandardMaterial> materials) {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x05070d));
        float origin = (GRID_SIZE - 1) * GRID_SPACING * -0.5f;
        for (int row = 0; row < GRID_SIZE; row++) {
            float roughness = 1.0f - row * (1.0f - MINIMUM_ROUGHNESS) / (GRID_SIZE - 1);
            for (int column = 0; column < GRID_SIZE; column++) {
                float metalness = (float) column / (GRID_SIZE - 1);
                StandardMaterial material = new StandardMaterial(Color.srgb(0xd8a43b));
                material.setMetalness(metalness);
                material.setRoughness(roughness);
                materials.add(material);
                Mesh sphere = new Mesh(geometry, material);
                sphere.setPosition(origin + column * GRID_SPACING, origin + row * GRID_SPACING, 0.0f);
                scene.add(sphere);
            }
        }
        scene.add(new AmbientLight(Color.srgb(0x8ca4d8), 0.12f));
        DirectionalLight keyLight = new DirectionalLight(Color.srgb(0xffe3c2), 2.2f);
        keyLight.setPosition(-4.0f, 6.0f, 8.0f);
        scene.add(keyLight);
        PointLight rimLight = new PointLight(Color.srgb(0x5c8dff), 55.0f);
        rimLight.setPosition(5.0f, -2.0f, 5.0f);
        rimLight.setDistance(30.0f);
        scene.add(rimLight);
        return scene;
    }

    /** Creates a compact guide to reading the material grid. */
    private static ControlPanel createPanel(ExampleContext context) {
        ControlPanel panel = new ControlPanel(context.window(), "Standard Material");
        ControlPanel.Section guide = panel.addSection("Grid");
        guide.addText("horizontal", () -> "metalness 0 to 1");
        guide.addText("vertical", () -> "roughness 1 to 0.08");
        guide.addText("model", () -> "metallic-roughness PBR");
        return panel;
    }
}
