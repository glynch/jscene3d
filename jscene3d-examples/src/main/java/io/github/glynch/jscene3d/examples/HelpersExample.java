/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.core.AxesHelper;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.GridHelper;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;

/** Displays an XZ reference grid and colored positive coordinate axes. */
public final class HelpersExample {
    /** Prevents instantiation of this example entry point. */
    private HelpersExample() {
        throw new AssertionError("HelpersExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Helpers");
                AxesHelper axes = new AxesHelper(3.0f);
                GridHelper grid = new GridHelper(10.0f, 10);
                Renderer renderer = Renderer.create(window)) {
            Scene scene = new Scene();
            scene.setBackground(Color.BLACK);
            scene.add(grid);
            axes.setRenderOrder(1);
            scene.add(axes);

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(6.0f, 5.0f, 7.0f);
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
}
