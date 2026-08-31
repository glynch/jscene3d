/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays overlapping transparent planes rendered in deterministic back-to-front order. */
public final class TransparencyExample {
    /** Prevents instantiation of this example entry point. */
    private TransparencyExample() {
        throw new AssertionError("TransparencyExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>The near red plane is deliberately inserted before the farther blue plane. Correct
     * blending therefore visibly depends on the renderer's camera-space transparent sort.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Transparency");
                Renderer renderer = Renderer.create(window);
                BufferGeometry backdropGeometry = PlaneGeometry.create(4.5f, 3.0f);
                BufferGeometry transparentGeometry = PlaneGeometry.create(2.4f, 1.8f);
                BasicMaterial backdropMaterial = new BasicMaterial(Color.GRAY);
                BasicMaterial nearMaterial = createTransparentMaterial(Color.RED);
                BasicMaterial farMaterial = createTransparentMaterial(Color.BLUE)) {
            Scene scene =
                    createScene(backdropGeometry, transparentGeometry, backdropMaterial, nearMaterial, farMaterial);

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 4.0f);
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
                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
    }

    /** Creates the planes with insertion order intentionally different from depth order. */
    private static Scene createScene(
            BufferGeometry backdropGeometry,
            BufferGeometry transparentGeometry,
            BasicMaterial backdropMaterial,
            BasicMaterial nearMaterial,
            BasicMaterial farMaterial) {
        Mesh backdrop = new Mesh(backdropGeometry, backdropMaterial);
        backdrop.setPosition(0.0f, 0.0f, -0.5f);

        Mesh nearPlane = new Mesh(transparentGeometry, nearMaterial);
        nearPlane.setPosition(0.45f, 0.0f, 0.3f);

        Mesh farPlane = new Mesh(transparentGeometry, farMaterial);
        farPlane.setPosition(-0.45f, 0.0f, 0.0f);

        Scene scene = new Scene();
        scene.setBackground(Color.BLACK);
        scene.add(backdrop);
        scene.add(nearPlane);
        scene.add(farPlane);
        return scene;
    }

    /** Creates a conventional alpha-blended material that leaves the depth buffer unchanged. */
    private static BasicMaterial createTransparentMaterial(Color color) {
        BasicMaterial material = new BasicMaterial(color);
        material.setOpacity(0.55f);
        material.setTransparent(true);
        material.setDepthWriteEnabled(false);
        return material;
    }
}
