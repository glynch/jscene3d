/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.MaterialSide;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;

/** Displays a rotating vertex-colored triangle with the public rendering API. */
public final class BasicTriangleExample {
    /** Prevents instantiation of this example entry point. */
    private BasicTriangleExample() {
        throw new AssertionError("BasicTriangleExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Basic Triangle");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = BufferGeometry.builder()
                        .positions(-0.8f, -0.7f, 0.0f, 0.8f, -0.7f, 0.0f, 0.0f, 0.8f, 0.0f)
                        .vertexColors(Color.RED, Color.GREEN, Color.BLUE)
                        .build();
                BasicMaterial material = createMaterial()) {
            Scene scene = new Scene();
            Mesh triangle = new Mesh(geometry, material);
            scene.add(triangle);

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);
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
                triangle.rotateY(0.01f);
                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
    }

    /** Creates the double-sided vertex-color material used by the rotating triangle. */
    private static BasicMaterial createMaterial() {
        BasicMaterial material = new BasicMaterial(Color.WHITE);
        material.setUsesVertexColors(true);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }
}
