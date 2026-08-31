/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.scenes.Scene;

/** Demonstrates editable spotlight cones and sky-to-ground hemisphere illumination. */
public final class SpotAndHemisphereLightsExample {
    /** Prevents instantiation of this example entry point. */
    private SpotAndHemisphereLightsExample() {
        throw new AssertionError("SpotAndHemisphereLightsExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Use the panel to change spotlight and hemisphere-light properties. Drag with the left
     * mouse button to orbit, drag with the right mouse button to pan, and use the scroll wheel to
     * dolly. Panel interaction is not passed to the camera controls.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Spot and Hemisphere Lights", SpotAndHemisphereLightsExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry sphereGeometry = SphereGeometry.create(0.9f, 48, 24);
        BufferGeometry groundGeometry = PlaneGeometry.create(14.0f, 10.0f);
        PhongMaterial cyanMaterial = shiny(Color.CYAN);
        PhongMaterial yellowMaterial = shiny(Color.YELLOW);
        PhongMaterial magentaMaterial = shiny(Color.MAGENTA);
        PhongMaterial groundMaterial = new PhongMaterial(Color.srgb(0x30343c));
        HemisphereLight hemisphereLight = new HemisphereLight(Color.srgb(0xb1e1ff), Color.srgb(0x5b3215), 0.65f);
        hemisphereLight.setPosition(0.0f, 5.0f, 0.0f);
        SpotLight spotLight = new SpotLight(Color.WHITE, 55.0f);
        spotLight.setPosition(0.0f, 6.0f, 4.0f);
        spotLight.setTarget(0.0f, 0.0f, 0.0f);
        spotLight.setDistance(25.0f);
        spotLight.setAngle(0.45f);
        spotLight.setPenumbra(0.45f);
        GalleryResources gallery = new GalleryResources(
                sphereGeometry, groundGeometry, cyanMaterial, yellowMaterial, magentaMaterial, groundMaterial);
        Scene scene = createScene(gallery, hemisphereLight, spotLight);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(7.0f, 5.0f, 10.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(5.0f, 30.0f);
        controls.setDampingEnabled(true);
        controls.update();
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(sphereGeometry);
        example.own(groundGeometry);
        example.own(cyanMaterial);
        example.own(yellowMaterial);
        example.own(magentaMaterial);
        example.own(groundMaterial);
        ControlPanel panel = example.addOverlay(createPanel(context.window(), hemisphereLight, spotLight));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates a moderately shiny Phong material for one gallery sphere. */
    private static PhongMaterial shiny(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecular(Color.WHITE);
        material.setShininess(70.0f);
        return material;
    }

    /** Creates the lit scene with three spheres and a receiving ground plane. */
    private static Scene createScene(GalleryResources gallery, HemisphereLight hemisphereLight, SpotLight spotLight) {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        scene.add(createSphere(gallery.sphereGeometry(), gallery.cyanMaterial(), -2.2f));
        scene.add(createSphere(gallery.sphereGeometry(), gallery.yellowMaterial(), 0.0f));
        scene.add(createSphere(gallery.sphereGeometry(), gallery.magentaMaterial(), 2.2f));
        gallery.groundMaterial().setSide(MaterialSide.DOUBLE);
        Mesh ground = new Mesh(gallery.groundGeometry(), gallery.groundMaterial());
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -1.0f, 0.0f);
        scene.add(ground);
        scene.add(hemisphereLight);
        scene.add(spotLight);
        return scene;
    }

    /** Creates one positioned sphere sharing the gallery geometry. */
    private static Mesh createSphere(BufferGeometry geometry, PhongMaterial material, float x) {
        Mesh sphere = new Mesh(geometry, material);
        sphere.setPosition(x, 0.0f, 0.0f);
        return sphere;
    }

    /** Creates live controls for the two demonstrated light types. */
    private static ControlPanel createPanel(Window window, HemisphereLight hemisphereLight, SpotLight spotLight) {
        ControlPanel panel = new ControlPanel(window, "Lights");
        ControlPanel.Section hemisphere = panel.addSection("Hemisphere");
        hemisphere.addFloat("intensity", hemisphereLight::intensity, hemisphereLight::setIntensity, 0.0f, 3.0f);
        ControlPanel.Section spot = panel.addSection("Spot");
        spot.addFloat("intensity", spotLight::intensity, spotLight::setIntensity, 0.0f, 100.0f);
        spot.addFloat("distance", spotLight::distance, spotLight::setDistance, 0.0f, 40.0f);
        spot.addFloat("angle", spotLight::angle, spotLight::setAngle, 0.05f, PI_OVER_TWO);
        spot.addFloat("penumbra", spotLight::penumbra, spotLight::setPenumbra, 0.0f, 1.0f);
        spot.addFloat("decay", spotLight::decay, spotLight::setDecay, 0.0f, 3.0f);
        return panel;
    }

    /** Resources shared by the objects in the example scene. */
    private record GalleryResources(
            BufferGeometry sphereGeometry,
            BufferGeometry groundGeometry,
            PhongMaterial cyanMaterial,
            PhongMaterial yellowMaterial,
            PhongMaterial magentaMaterial,
            PhongMaterial groundMaterial) {}
}
