/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BoxGeometry;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;

/** Displays a small scene that can be inspected with orbit, pan, and dolly controls. */
public final class OrbitControlsExample {
    private OrbitControlsExample() {
        throw new AssertionError("OrbitControlsExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Drag with the left mouse button to orbit, drag with the right mouse button or Shift-left
     * to pan, and use the middle mouse button or scroll wheel to dolly. Arrow keys pan; hold Shift
     * while using them to rotate.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Orbit Controls");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BasicMaterial centerMaterial = new BasicMaterial(Color.YELLOW);
                BasicMaterial leftMaterial = new BasicMaterial(Color.CYAN);
                BasicMaterial rightMaterial = new BasicMaterial(Color.MAGENTA)) {
            Scene scene = createScene(geometry, centerMaterial, leftMaterial, rightMaterial);
            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(4.0f, 3.0f, 6.0f);

            OrbitControls controls = new OrbitControls(camera, window);
            controls.setDistanceLimits(2.0f, 20.0f);
            controls.setDampingEnabled(true);
            controls.update();
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

    private static Scene createScene(
            BufferGeometry geometry,
            BasicMaterial centerMaterial,
            BasicMaterial leftMaterial,
            BasicMaterial rightMaterial) {
        Mesh center = new Mesh(geometry, centerMaterial);

        Mesh left = new Mesh(geometry, leftMaterial);
        left.setPosition(-2.0f, 0.0f, 0.0f);
        left.setScale(0.6f, 1.4f, 0.6f);

        Mesh right = new Mesh(geometry, rightMaterial);
        right.setPosition(2.0f, 0.0f, 0.0f);
        right.setScale(0.8f, 0.8f, 0.8f);

        Scene scene = new Scene();
        scene.setBackground(Color.BLACK);
        scene.add(center);
        scene.add(left);
        scene.add(right);
        return scene;
    }
}
