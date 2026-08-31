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
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Compares built-in mesh materials and exposes live Phong controls. */
public final class MaterialsExample {
    /** Prevents instantiation of this example entry point. */
    private MaterialsExample() {
        throw new AssertionError("MaterialsExample cannot be instantiated");
    }

    /**
     * Runs this example as an independent native application.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Materials", MaterialsExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = SphereGeometry.create(1.0f, 48, 24);
        BasicMaterial basicMaterial = new BasicMaterial(Color.CYAN);
        LambertMaterial lambertMaterial = new LambertMaterial(Color.CYAN);
        NormalMaterial normalMaterial = new NormalMaterial();
        PhongMaterial phongMaterial = new PhongMaterial(Color.CYAN);
        phongMaterial.setEmissive(Color.srgb(0x080018));
        phongMaterial.setSpecular(Color.WHITE);
        phongMaterial.setShininess(80.0f);

        Scene scene = createScene(geometry, basicMaterial, lambertMaterial, normalMaterial, phongMaterial);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(7.0f, 3.5f, 11.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(6.0f, 30.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(basicMaterial);
        example.own(lambertMaterial);
        example.own(normalMaterial);
        example.own(phongMaterial);
        ControlPanel panel = example.addOverlay(createPanel(context, phongMaterial));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates the four-sphere comparison scene and shared lighting. */
    private static Scene createScene(
            BufferGeometry geometry,
            BasicMaterial basicMaterial,
            LambertMaterial lambertMaterial,
            NormalMaterial normalMaterial,
            PhongMaterial phongMaterial) {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        scene.add(createMesh(geometry, basicMaterial, -3.6f));
        scene.add(createMesh(geometry, lambertMaterial, -1.2f));
        scene.add(createMesh(geometry, normalMaterial, 1.2f));
        scene.add(createMesh(geometry, phongMaterial, 3.6f));
        scene.add(new AmbientLight(Color.srgb(0xb0c0ff), 0.12f));
        DirectionalLight light = new DirectionalLight(Color.WHITE, 1.2f);
        light.setPosition(2.0f, 5.0f, 7.0f);
        scene.add(light);
        return scene;
    }

    /** Creates one positioned sphere using a material of any supported mesh family. */
    private static Mesh createMesh(BufferGeometry geometry, Material material, float x) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, 0.0f, 0.0f);
        return mesh;
    }

    /** Creates the material legend and live Phong property controls. */
    private static ControlPanel createPanel(ExampleContext context, PhongMaterial material) {
        ControlPanel panel = new ControlPanel(context.window(), "Materials");
        ControlPanel.Section legend = panel.addSection("Left to right");
        legend.addText("1", () -> "Basic");
        legend.addText("2", () -> "Lambert");
        legend.addText("3", () -> "Normal");
        legend.addText("4", () -> "Phong");
        ControlPanel.Section phong = panel.addSection("Phong");
        phong.addFloat("shininess", material::shininess, material::setShininess, 0.0f, 200.0f);
        phong.addFloat("emissive intensity", material::emissiveIntensity, material::setEmissiveIntensity, 0.0f, 4.0f);
        return panel;
    }
}
