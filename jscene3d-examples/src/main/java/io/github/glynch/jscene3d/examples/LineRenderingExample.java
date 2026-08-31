/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays a connected orbit and independent vertex-colored coordinate axes. */
public final class LineRenderingExample {
    private static final int ORBIT_SEGMENTS = 96;

    /** Prevents instantiation of this example entry point. */
    private LineRenderingExample() {
        throw new AssertionError("LineRenderingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Line Rendering", LineRenderingExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry orbitGeometry = createOrbitGeometry();
        BufferGeometry axesGeometry = createAxesGeometry();
        LineBasicMaterial orbitMaterial = new LineBasicMaterial(Color.CYAN);
        LineBasicMaterial axesMaterial = createAxesMaterial();
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        scene.add(new Line(orbitGeometry, orbitMaterial));
        scene.add(new LineSegments(axesGeometry, axesMaterial));
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(3.0f, 2.5f, 4.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(orbitGeometry);
        example.own(axesGeometry);
        example.own(orbitMaterial);
        example.own(axesMaterial);
        return example;
    }

    /** Creates a closed connected line strip in the XZ plane. */
    private static BufferGeometry createOrbitGeometry() {
        float[] positions = new float[(ORBIT_SEGMENTS + 1) * 3];
        for (int index = 0; index <= ORBIT_SEGMENTS; index++) {
            double angle = TWO_PI * index / ORBIT_SEGMENTS;
            int offset = index * 3;
            positions[offset] = (float) (2.0 * Math.cos(angle));
            positions[offset + 1] = 0.0f;
            positions[offset + 2] = (float) (2.0 * Math.sin(angle));
        }
        return BufferGeometry.builder().positions(positions).build();
    }

    /** Creates three independent axis segments with endpoint colors. */
    private static BufferGeometry createAxesGeometry() {
        return BufferGeometry.builder()
                .positions(
                        0.0f, 0.0f, 0.0f, 2.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 2.75f, 0.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 2.75f)
                .vertexColors(Color.RED, Color.RED, Color.GREEN, Color.GREEN, Color.BLUE, Color.BLUE)
                .build();
    }

    /** Creates the white vertex-colored material used for the axes. */
    private static LineBasicMaterial createAxesMaterial() {
        LineBasicMaterial material = new LineBasicMaterial();
        material.setUsesVertexColors(true);
        return material;
    }
}
