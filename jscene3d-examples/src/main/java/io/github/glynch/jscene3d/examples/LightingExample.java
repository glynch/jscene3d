/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays diffuse materials illuminated by ambient and moving point lights. */
public final class LightingExample {
    /** Prevents instantiation of this example entry point. */
    private LightingExample() {
        throw new AssertionError("LightingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Drag with the left mouse button to orbit, drag with the right mouse button to pan, and use
     * the scroll wheel to dolly.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Lambert Lighting", LightingExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry sphereGeometry = SphereGeometry.create(0.75f);
        BufferGeometry groundGeometry = PlaneGeometry.create(12.0f, 8.0f);
        BufferGeometry markerGeometry = SphereGeometry.create(0.12f, 16, 8);
        LambertMaterial cyanMaterial = new LambertMaterial(Color.CYAN);
        LambertMaterial yellowMaterial = new LambertMaterial(Color.YELLOW);
        LambertMaterial magentaMaterial = new LambertMaterial(Color.MAGENTA);
        LambertMaterial groundMaterial = new LambertMaterial(Color.srgb(0x303640));
        BasicMaterial markerMaterial = new BasicMaterial(Color.WHITE);
        PointLight pointLight = new PointLight(Color.srgb(0xffe0b0), 35.0f);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        scene.add(new AmbientLight(Color.srgb(0x8090b0), 0.08f));
        DirectionalLight directionalLight = new DirectionalLight(Color.srgb(0x9fc5ff), 0.45f);
        directionalLight.setPosition(-4.0f, 5.0f, 3.0f);
        directionalLight.setTarget(0.0f, 0.0f, 0.0f);
        scene.add(directionalLight);
        scene.add(createSphere(sphereGeometry, cyanMaterial, -2.0f));
        scene.add(createSphere(sphereGeometry, yellowMaterial, 0.0f));
        scene.add(createSphere(sphereGeometry, magentaMaterial, 2.0f));
        Mesh ground = new Mesh(groundGeometry, groundMaterial);
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -1.0f, 0.0f);
        groundMaterial.setSide(MaterialSide.DOUBLE);
        scene.add(ground);
        pointLight.setDistance(12.0f);
        pointLight.add(new Mesh(markerGeometry, markerMaterial));
        scene.add(pointLight);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(6.0f, 4.0f, 8.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(4.0f, 20.0f);
        controls.setDampingEnabled(true);
        controls.update();
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(sphereGeometry);
        example.own(groundGeometry);
        example.own(markerGeometry);
        example.own(cyanMaterial);
        example.own(yellowMaterial);
        example.own(magentaMaterial);
        example.own(groundMaterial);
        example.own(markerMaterial);
        float[] lightAngle = {0.0f};
        example.setFrameAction((ignored, frame) -> {
            lightAngle[0] = (lightAngle[0] + frame.elapsedSeconds() * 0.6f) % TWO_PI;
            pointLight.setPosition(
                    4.0f * (float) Math.cos(lightAngle[0]), 3.0f, 4.0f * (float) Math.sin(lightAngle[0]));
        });
        return example;
    }

    /** Creates one positioned sphere sharing the supplied geometry and material. */
    private static Mesh createSphere(BufferGeometry geometry, LambertMaterial material, float x) {
        Mesh sphere = new Mesh(geometry, material);
        sphere.setPosition(x, 0.0f, 0.0f);
        return sphere;
    }
}
