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
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays a small scene that can be inspected with orbit, pan, and dolly controls. */
public final class OrbitControlsExample {
    /** Prevents instantiation of this example entry point. */
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
        ExampleLauncher.launch("JScene3D - Orbit Controls", OrbitControlsExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
        BasicMaterial centerMaterial = new BasicMaterial(Color.YELLOW);
        BasicMaterial leftMaterial = new BasicMaterial(Color.CYAN);
        BasicMaterial rightMaterial = new BasicMaterial(Color.MAGENTA);
        Scene scene = createScene(geometry, centerMaterial, leftMaterial, rightMaterial);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(4.0f, 3.0f, 6.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(2.0f, 20.0f);
        controls.setDampingEnabled(true);
        controls.update();
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(centerMaterial);
        example.own(leftMaterial);
        example.own(rightMaterial);
        ControlPanel panel = example.addOverlay(createControlPanel(context.window(), controls));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates explicit Java bindings for the interactive camera settings. */
    private static ControlPanel createControlPanel(Window window, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(window, "Orbit Controls");

        ControlPanel.Section interaction = panel.addSection("Interaction");
        interaction.addBoolean("enabled", controls::isEnabled, controls::setEnabled);
        interaction.addBoolean("rotation", controls::isRotationEnabled, controls::setRotationEnabled);
        interaction.addBoolean("panning", controls::isPanningEnabled, controls::setPanningEnabled);
        interaction.addBoolean("zoom", controls::isZoomEnabled, controls::setZoomEnabled);
        interaction.addBoolean("screen-space pan", controls::isScreenSpacePanning, controls::setScreenSpacePanning);

        ControlPanel.Section motion = panel.addSection("Motion");
        motion.addFloat("rotation speed", controls::rotationSpeed, controls::setRotationSpeed, 0.0f, 3.0f);
        motion.addFloat("pan speed", controls::panSpeed, controls::setPanSpeed, 0.0f, 3.0f);
        motion.addFloat("zoom speed", controls::zoomSpeed, controls::setZoomSpeed, 0.0f, 3.0f);
        motion.addBoolean("damping", controls::isDampingEnabled, controls::setDampingEnabled);
        motion.addFloat("damping factor", controls::dampingFactor, controls::setDampingFactor, 0.01f, 1.0f);
        motion.addBoolean("auto rotate", controls::isAutoRotationEnabled, controls::setAutoRotationEnabled);
        motion.addFloat("auto speed", controls::autoRotationSpeed, controls::setAutoRotationSpeed, -5.0f, 5.0f);

        ControlPanel.Section state = panel.addSection("State");
        state.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Creates three differently transformed boxes that make camera movement easy to see. */
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
