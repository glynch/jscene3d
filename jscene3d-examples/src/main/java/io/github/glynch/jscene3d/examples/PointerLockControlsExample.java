/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.PointerLockControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.scenes.Scene;

/** Demonstrates captured relative mouse motion without adding movement or game rules. */
public final class PointerLockControlsExample {
    /** Prevents instantiation of this example entry point. */
    private PointerLockControlsExample() {
        throw new AssertionError("PointerLockControlsExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed.
     *
     * <p>Click the rendered view to capture the pointer, move the mouse to look around, and press
     * Escape to release it. Press Escape again after release to close the standalone example.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Pointer Lock Controls", PointerLockControlsExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.0f, 2.0f, 1.0f);
        BasicMaterial cyan = new BasicMaterial(Color.CYAN);
        BasicMaterial magenta = new BasicMaterial(Color.MAGENTA);
        BasicMaterial yellow = new BasicMaterial(Color.YELLOW);
        BasicMaterial red = new BasicMaterial(Color.RED);
        BasicMaterial green = new BasicMaterial(Color.GREEN);
        GridHelper grid = new GridHelper(20.0f, 20, Color.srgb(0x335066), Color.srgb(0x243342));
        Scene scene = createScene(geometry, grid, cyan, magenta, yellow, red, green);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 1.5f, 5.0f);
        camera.lookAt(0.0f, 1.25f, 0.0f);
        PointerLockControls controls = new PointerLockControls(camera, context.window());

        SceneExample example = new SceneExample(context, scene, camera);
        example.own(geometry);
        example.own(cyan);
        example.own(magenta);
        example.own(yellow);
        example.own(red);
        example.own(green);
        example.own(grid);
        example.own(controls);

        ControlPanel panel = example.addOverlay(createControlPanel(context.window(), controls));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            captureFromRenderedView(context, controls, panel, frame.pointerCaptured());
            controls.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates the live pointer-lock configuration panel. */
    private static ControlPanel createControlPanel(Window window, PointerLockControls controls) {
        ControlPanel panel = new ControlPanel(window, "Pointer Lock");
        ControlPanel.Section interaction = panel.addSection("Interaction");
        interaction.addText("status", () -> controls.isLocked() ? "captured" : "released");
        interaction.addButton("capture pointer", () -> !controls.isLocked(), controls::lock);
        interaction.addText("release key", () -> "Escape");
        addRawMouseMotionControls(interaction, window, controls);

        ControlPanel.Section orientation = panel.addSection("Orientation");
        orientation.addFloat("sensitivity", controls::sensitivity, controls::setSensitivity, 0.0005f, 0.01f, 4);
        orientation.addText("yaw", () -> Float.toString(controls.yaw()));
        orientation.addText("pitch", () -> Float.toString(controls.pitch()));
        orientation.addButton("reset view", controls::reset);
        return panel;
    }

    /** Adds only raw-motion controls that are meaningful on the current platform. */
    private static void addRawMouseMotionControls(
            ControlPanel.Section interaction, Window window, PointerLockControls controls) {
        if (!window.isRawMouseMotionSupported()) {
            interaction.addText("raw motion", () -> "unavailable");
            return;
        }
        interaction.addBoolean(
                "prefer raw motion", controls::isRawMouseMotionPreferred, controls::setRawMouseMotionPreferred);
        interaction.addText("raw motion", () -> window.isRawMouseMotionEnabled() ? "enabled" : "available");
    }

    /** Captures a primary click inside the rendered content when no overlay owns it. */
    private static void captureFromRenderedView(
            ExampleContext context, PointerLockControls controls, ControlPanel panel, boolean pointerCaptured) {
        InputState input = context.window().input();
        if (!controls.isLocked()
                && !pointerCaptured
                && !panel.capturesPointer()
                && context.containsPointer()
                && input.wasMouseButtonPressed(MouseButton.LEFT)) {
            controls.lock();
        }
    }

    /** Builds distinct landmarks around the camera's starting view. */
    private static Scene createScene(
            BufferGeometry geometry,
            GridHelper grid,
            BasicMaterial cyan,
            BasicMaterial magenta,
            BasicMaterial yellow,
            BasicMaterial red,
            BasicMaterial green) {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x07101a));
        scene.add(grid);
        scene.add(box(geometry, cyan, -3.0f, 1.0f, 0.0f));
        scene.add(box(geometry, magenta, 3.0f, 1.0f, 0.0f));
        scene.add(box(geometry, yellow, 0.0f, 1.0f, -4.0f));
        scene.add(box(geometry, red, -4.0f, 1.0f, 5.0f));
        scene.add(box(geometry, green, 4.0f, 1.0f, 5.0f));
        return scene;
    }

    /** Creates one positioned landmark sharing the example's geometry. */
    private static Mesh box(BufferGeometry geometry, BasicMaterial material, float x, float y, float z) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, y, z);
        return mesh;
    }
}
