/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.platform.Window;

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
        ExampleLauncher.launch("JScene3D - Solar System Viewer", SolarSystemViewer::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        return new Content(context);
    }

    /** Owns the complete viewer state while delegating its window and renderer to a host. */
    private static final class Content implements HostedExample {
        private final ExampleContext context;
        private final SolarSystemScene solarSystem = SolarSystemScene.create();
        private final PerspectiveCamera camera;
        private final OrbitControls controls;
        private final FpsMonitor fpsMonitor = new FpsMonitor();
        private final ControlPanel panel;

        /** Builds the scene, camera, controls, and overlays against the shared context. */
        private Content(ExampleContext context) {
            this.context = context;
            camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 250.0f);
            camera.setPosition(0.0f, 16.0f, 34.0f);
            controls = new OrbitControls(camera, context.window());
            controls.setDistanceLimits(6.0f, 70.0f);
            controls.setDampingEnabled(true);
            controls.update();
            controls.saveState();
            fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
            panel = createControlPanel(context.window());
        }

        /** Creates explicit Java bindings for simulation, display, and camera controls. */
        private ControlPanel createControlPanel(Window window) {
            ControlPanel createdPanel = new ControlPanel(window, "Solar System");

            ControlPanel.Section simulation = createdPanel.addSection("Simulation");
            simulation.addBoolean("paused", solarSystem::isPaused, solarSystem::setPaused);
            simulation.addFloat("time scale", solarSystem::timeScale, solarSystem::setTimeScale, 0.0f, 4.0f);
            simulation.addButton("reset orbits", solarSystem::reset);

            ControlPanel.Section display = createdPanel.addSection("Display");
            display.addBoolean("star field", solarSystem::isStarFieldVisible, solarSystem::setStarFieldVisible);
            display.addBoolean("FPS monitor", fpsMonitor::isVisible, fpsMonitor::setVisible);

            ControlPanel.Section cameraSection = createdPanel.addSection("Camera");
            cameraSection.addButton("reset camera", controls::reset);
            return createdPanel;
        }

        /** Updates the projection and camera-control scale for the current content area. */
        @Override
        public void resize() {
            camera.setAspectRatio(context.aspectRatio());
            controls.setViewportSize(context.logicalWidth(), context.logicalHeight());
        }

        /** Advances simulation, panel state, FPS state, and camera controls. */
        @Override
        public void update(ExampleFrame frame) {
            panel.update();
            float elapsedSeconds = Math.min(frame.elapsedSeconds(), MAXIMUM_FRAME_SECONDS);
            solarSystem.update(elapsedSeconds);
            if (frame.pointerCaptured() || panel.capturesPointer()) {
                controls.updateWithoutPointerInput(elapsedSeconds);
            } else {
                controls.update(elapsedSeconds);
            }
            fpsMonitor.update();
        }

        /** Renders the scene and optional overlays without swapping host buffers. */
        @Override
        public void render() {
            context.renderer().render(solarSystem.scene(), camera);
            context.renderer().render(panel);
            context.renderer().render(fpsMonitor);
        }

        /** Renders the representative solar-system scene without control overlays. */
        @Override
        public void renderThumbnail() {
            context.renderer().render(solarSystem.scene(), camera);
        }

        /** Closes the viewer's geometry, textures, and materials. */
        @Override
        public void close() {
            solarSystem.close();
        }
    }
}
