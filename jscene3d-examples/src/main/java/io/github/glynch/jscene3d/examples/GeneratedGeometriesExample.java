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
import io.github.glynch.jscene3d.geometries.CircleGeometry;
import io.github.glynch.jscene3d.geometries.ConeGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays circle, cylinder, cone, and torus generated geometry. */
public final class GeneratedGeometriesExample {
    /** Prevents instantiation of this example entry point. */
    private GeneratedGeometriesExample() {
        throw new AssertionError("GeneratedGeometriesExample cannot be instantiated");
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
        ExampleLauncher.launch("JScene3D - Generated Geometries", GeneratedGeometriesExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry circleGeometry = CircleGeometry.create(1.1f);
        BufferGeometry cylinderGeometry = CylinderGeometry.create(0.9f, 2.0f);
        BufferGeometry coneGeometry = ConeGeometry.create(1.0f, 2.0f);
        BufferGeometry torusGeometry = TorusGeometry.create(1.0f, 0.35f);
        LambertMaterial circleMaterial = new LambertMaterial(Color.MAGENTA);
        LambertMaterial cylinderMaterial = new LambertMaterial(Color.CYAN);
        LambertMaterial coneMaterial = new LambertMaterial(Color.YELLOW);
        LambertMaterial torusMaterial = new LambertMaterial(Color.srgb(0xff7043));
        Scene scene = createScene();
        circleMaterial.setSide(MaterialSide.DOUBLE);
        scene.add(createMesh(circleGeometry, circleMaterial, -4.2f));
        scene.add(createMesh(cylinderGeometry, cylinderMaterial, -1.4f));
        scene.add(createMesh(coneGeometry, coneMaterial, 1.4f));
        scene.add(createMesh(torusGeometry, torusMaterial, 4.2f));
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(8.0f, 4.5f, 12.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(6.0f, 30.0f);
        controls.setDampingEnabled(true);
        controls.update();
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(circleGeometry);
        example.own(cylinderGeometry);
        example.own(coneGeometry);
        example.own(torusGeometry);
        example.own(circleMaterial);
        example.own(cylinderMaterial);
        example.own(coneMaterial);
        example.own(torusMaterial);
        return example;
    }

    /** Creates the lit gallery scene. */
    private static Scene createScene() {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x080b12));
        scene.add(new AmbientLight(Color.WHITE, 0.18f));
        PointLight light = new PointLight(Color.srgb(0xffe4c4), 65.0f);
        light.setPosition(3.0f, 6.0f, 7.0f);
        light.setDistance(30.0f);
        scene.add(light);
        return scene;
    }

    /** Creates one positioned gallery mesh. */
    private static Mesh createMesh(BufferGeometry geometry, LambertMaterial material, float x) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, 0.0f, 0.0f);
        return mesh;
    }
}
