/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.render.Renderer;

/** Interactive textured Solar System Viewer demonstrating the complete version 0.1 interface. */
public final class SolarSystemViewer {
    private static final float MAXIMUM_FRAME_SECONDS = 0.1f;

    /** Prevents instantiation of this example entry point. */
    private SolarSystemViewer() {
        throw new AssertionError("SolarSystemViewer cannot be instantiated");
    }

    /**
     * Opens the viewer and renders until the window is closed or Escape is pressed.
     *
     * <p>Drag with the left mouse button to orbit, drag with the right mouse button or Shift-left
     * to pan, and use the middle mouse button or scroll wheel to dolly.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(1400, 900)
                .title("JScene3D - Solar System Viewer")
                .preferredFramebufferSampleCount(4)
                .build();
        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                SolarSystemScene solarSystem = SolarSystemScene.create()) {
            window.setTitle("JScene3D - Solar System Viewer (" + window.framebufferSampleCount() + "x MSAA)");
            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 250.0f);
            camera.setPosition(0.0f, 16.0f, 34.0f);

            OrbitControls controls = new OrbitControls(camera, window);
            controls.setTarget(0.0f, 0.0f, 0.0f);
            controls.setDistanceLimits(6.0f, 70.0f);
            controls.setDampingEnabled(true);
            controls.update();
            controls.saveState();

            FpsMonitor fpsMonitor = new FpsMonitor();
            ControlPanel panel = createControlPanel(window, solarSystem, controls, fpsMonitor);
            window.show();

            long previousFrameNanos = System.nanoTime();
            while (!window.shouldClose()) {
                long frameNanos = System.nanoTime();
                double frameSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000.0;
                float elapsedSeconds = (float) Math.min(frameSeconds, MAXIMUM_FRAME_SECONDS);
                previousFrameNanos = frameNanos;

                Window.pollEvents();
                if (window.input().wasKeyPressed(Key.ESCAPE)) {
                    window.requestClose();
                }
                if (window.framebufferSizeChanged()
                        && window.framebufferWidth() > 0
                        && window.framebufferHeight() > 0) {
                    camera.setAspectRatio(window.framebufferAspectRatio());
                }

                panel.update();
                solarSystem.update(elapsedSeconds);
                if (panel.capturesPointer()) {
                    controls.updateWithoutPointerInput(elapsedSeconds);
                } else {
                    controls.update(elapsedSeconds);
                }

                renderer.render(solarSystem.scene(), camera);
                renderer.render(panel);
                renderer.render(fpsMonitor);
                window.swapBuffers();
                fpsMonitor.update();
            }
        }
    }

    /** Creates explicit Java bindings for simulation, display, and camera controls. */
    private static ControlPanel createControlPanel(
            Window window, SolarSystemScene solarSystem, OrbitControls controls, FpsMonitor fpsMonitor) {
        ControlPanel panel = new ControlPanel(window, "Solar System");

        ControlPanel.Section simulation = panel.addSection("Simulation");
        simulation.addBoolean("paused", solarSystem::isPaused, solarSystem::setPaused);
        simulation.addFloat("time scale", solarSystem::timeScale, solarSystem::setTimeScale, 0.0f, 4.0f);
        simulation.addButton("reset orbits", solarSystem::reset);

        ControlPanel.Section display = panel.addSection("Display");
        display.addBoolean("star field", solarSystem::isStarFieldVisible, solarSystem::setStarFieldVisible);
        display.addBoolean("FPS monitor", fpsMonitor::isVisible, fpsMonitor::setVisible);

        ControlPanel.Section camera = panel.addSection("Camera");
        camera.addButton("reset camera", controls::reset);
        return panel;
    }
}
