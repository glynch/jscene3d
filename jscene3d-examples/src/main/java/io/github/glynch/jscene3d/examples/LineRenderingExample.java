/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.core.Angles.TWO_PI;

import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Line;
import io.github.glynch.jscene3d.core.LineBasicMaterial;
import io.github.glynch.jscene3d.core.LineSegments;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;

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
        try (Window window = Window.create("JScene3D - Line Rendering");
                Renderer renderer = Renderer.create(window);
                BufferGeometry orbitGeometry = createOrbitGeometry();
                BufferGeometry axesGeometry = createAxesGeometry();
                LineBasicMaterial orbitMaterial = new LineBasicMaterial(Color.CYAN);
                LineBasicMaterial axesMaterial = createAxesMaterial()) {
            Scene scene = new Scene();
            scene.add(new Line(orbitGeometry, orbitMaterial));
            scene.add(new LineSegments(axesGeometry, axesMaterial));

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(3.0f, 2.5f, 4.0f);
            camera.lookAt(0.0f, 0.0f, 0.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            window.show();

            while (!window.shouldClose()) {
                Window.pollEvents();
                if (window.input().wasKeyPressed(Key.ESCAPE)) {
                    window.requestClose();
                }
                if (window.framebufferSizeChanged()
                        && window.framebufferWidth() > 0
                        && window.framebufferHeight() > 0) {
                    camera.setAspectRatio(window.framebufferAspectRatio());
                }
                controls.update();
                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
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
