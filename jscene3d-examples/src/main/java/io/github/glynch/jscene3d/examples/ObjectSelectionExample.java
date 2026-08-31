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
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.raycasting.RaycastHit;
import io.github.glynch.jscene3d.raycasting.Raycaster;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Demonstrates pointer-based mesh selection with nearest-first CPU raycasting. */
public final class ObjectSelectionExample {
    /** Prevents instantiation of this example entry point. */
    private ObjectSelectionExample() {
        throw new AssertionError("ObjectSelectionExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Click a box to select it. Drag with the left mouse button to orbit, drag with the right
     * mouse button to pan, and use the scroll wheel to dolly. Pointer input over the panel is not
     * passed to selection or camera controls.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Object Selection", ObjectSelectionExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.4f, 1.4f, 1.4f);
        BasicMaterial cyanMaterial = new BasicMaterial(Color.CYAN);
        BasicMaterial yellowMaterial = new BasicMaterial(Color.YELLOW);
        BasicMaterial magentaMaterial = new BasicMaterial(Color.MAGENTA);
        SelectionTarget[] targets = {
            target("cyan box", geometry, cyanMaterial, Color.CYAN, -2.0f),
            target("yellow box", geometry, yellowMaterial, Color.YELLOW, 0.0f),
            target("magenta box", geometry, magentaMaterial, Color.MAGENTA, 2.0f)
        };
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        for (SelectionTarget target : targets) {
            scene.add(target.mesh());
        }
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(4.0f, 3.0f, 7.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDampingEnabled(true);
        controls.setDistanceLimits(3.0f, 20.0f);
        controls.update();
        SelectionState selection = new SelectionState(targets);
        ControlPanel panel = createPanel(context.window(), selection);
        FpsMonitor fpsMonitor = new FpsMonitor();
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        Raycaster raycaster = new Raycaster();
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(cyanMaterial);
        example.own(yellowMaterial);
        example.own(magentaMaterial);
        example.addOverlay(panel);
        example.addOverlay(fpsMonitor);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            if (!frame.pointerCaptured() && !panel.capturesPointer()) {
                selectOnPointerPress(context, camera, scene, raycaster, selection);
            }
            fpsMonitor.update();
        });
        return example;
    }

    /** Creates one named selectable box at a horizontal position. */
    private static SelectionTarget target(
            String name, BufferGeometry geometry, BasicMaterial material, Color color, float x) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, 0.0f, 0.0f);
        return new SelectionTarget(name, mesh, material, color);
    }

    /** Creates the read-only selection display and explicit clear action. */
    private static ControlPanel createPanel(Window window, SelectionState selection) {
        ControlPanel panel = new ControlPanel(window, "Object Selection");
        ControlPanel.Section section = panel.addSection("Selection");
        section.addText("selected", selection::selectedName);
        section.addText("distance", selection::distanceText);
        section.addButton("clear selection", selection::clear);
        return panel;
    }

    /** Selects the nearest mesh under a primary-button press or clears an empty click. */
    private static void selectOnPointerPress(
            ExampleContext context,
            PerspectiveCamera camera,
            Scene scene,
            Raycaster raycaster,
            SelectionState selection) {
        if (!context.window().input().wasMouseButtonPressed(MouseButton.LEFT)) {
            return;
        }
        raycaster.setFromCamera(context.normalizedPointerX(), context.normalizedPointerY(), camera);
        List<RaycastHit> hits = raycaster.intersect(scene);
        if (hits.isEmpty()) {
            selection.clear();
        } else {
            selection.select(hits.getFirst());
        }
    }

    /** One named mesh with the base color restored when selection moves elsewhere. */
    private record SelectionTarget(String name, Mesh mesh, BasicMaterial material, Color color) {}

    /** Owns the example's current highlighted target and read-only display values. */
    private static final class SelectionState {
        private final SelectionTarget[] targets;

        private @Nullable SelectionTarget selected;
        private float distance;

        /** Retains the fixed selection targets. */
        private SelectionState(SelectionTarget[] targets) {
            this.targets = targets.clone();
        }

        /** Highlights the target represented by the nearest raycast hit. */
        private void select(RaycastHit hit) {
            SelectionTarget target = find(hit.mesh());
            if (selected != target) {
                restoreSelectedColor();
                selected = target;
                target.material().setColor(Color.WHITE);
            }
            distance = hit.distance();
        }

        /** Clears the current target and restores its base color. */
        private void clear() {
            restoreSelectedColor();
            selected = null;
            distance = 0.0f;
        }

        /** Returns the current target name for the control panel. */
        private String selectedName() {
            return selected == null ? "none" : selected.name();
        }

        /** Returns a compact current hit distance for the control panel. */
        private String distanceText() {
            if (selected == null) {
                return "-";
            }
            return Float.toString(Math.round(distance * 100.0f) / 100.0f);
        }

        /** Finds the fixed target owning one intersected mesh. */
        private SelectionTarget find(Mesh mesh) {
            for (SelectionTarget target : targets) {
                if (target.mesh() == mesh) {
                    return target;
                }
            }
            throw new IllegalStateException("Raycast hit does not belong to a selectable target");
        }

        /** Restores the base color of the current target when one exists. */
        private void restoreSelectedColor() {
            if (selected != null) {
                selected.material().setColor(selected.color());
            }
        }
    }
}
