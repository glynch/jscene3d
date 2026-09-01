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
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Demonstrates per-draw material customization through object render callbacks. */
public final class RenderCallbacksExample {
    /** Prevents instantiation of this example entry point. */
    private RenderCallbacksExample() {
        throw new AssertionError("RenderCallbacksExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Render Callbacks", RenderCallbacksExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
        BasicMaterial sharedMaterial = new BasicMaterial(Color.WHITE);
        List<CallbackBox> callbackBoxes = List.of(
                callbackBox(geometry, sharedMaterial, Color.RED, -1.4f),
                callbackBox(geometry, sharedMaterial, Color.GREEN, 0.0f),
                callbackBox(geometry, sharedMaterial, Color.BLUE, 1.4f));
        List<Mesh> comparisonBoxes = List.of(
                comparisonBox(geometry, sharedMaterial, -1.4f),
                comparisonBox(geometry, sharedMaterial, 0.0f),
                comparisonBox(geometry, sharedMaterial, 1.4f));
        List<Mesh> callbackMeshes =
                callbackBoxes.stream().map(CallbackBox::mesh).toList();
        CallbackDemo demo = new CallbackDemo(sharedMaterial, callbackBoxes);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x060A12));
        callbackMeshes.forEach(scene::add);
        comparisonBoxes.forEach(scene::add);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.8f, 6.5f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        SceneExample example = new SceneExample(context, scene, camera, controls);
        ControlPanel panel = example.addOverlay(createPanel(context.window(), demo));
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            rotate(callbackMeshes, frame.elapsedSeconds());
            rotate(comparisonBoxes, frame.elapsedSeconds());
        });
        example.own(geometry);
        example.own(sharedMaterial);
        return example;
    }

    /** Creates one top-row box with a retained per-draw color. */
    private static CallbackBox callbackBox(
            BufferGeometry geometry, BasicMaterial sharedMaterial, Color color, float x) {
        Mesh box = new Mesh(geometry, sharedMaterial);
        box.setPosition(x, 0.75f, 0.0f);
        return new CallbackBox(box, color);
    }

    /** Creates one bottom-row box that draws the unchanged shared white material. */
    private static Mesh comparisonBox(BufferGeometry geometry, BasicMaterial sharedMaterial, float x) {
        Mesh box = new Mesh(geometry, sharedMaterial);
        box.setPosition(x, -0.75f, 0.0f);
        return box;
    }

    /** Applies visibly different incremental rotations to one row of boxes. */
    private static void rotate(List<Mesh> boxes, float elapsedSeconds) {
        for (int index = 0; index < boxes.size(); index++) {
            float direction = index == 1 ? -1.0f : 1.0f;
            Mesh box = boxes.get(index);
            box.rotateX(elapsedSeconds * (0.45f + index * 0.12f));
            box.rotateY(elapsedSeconds * direction * (0.65f + index * 0.1f));
        }
    }

    /** Creates the live explanation and callback controls. */
    private static ControlPanel createPanel(Window window, CallbackDemo demo) {
        ControlPanel panel = new ControlPanel(window, "Render Callbacks");
        ControlPanel.Section comparison = panel.addSection("Same shared material");
        comparison.addText("top row", demo::topRowDescription);
        comparison.addText("bottom row", () -> "no callbacks: white");
        comparison.addText("materials", () -> "1 shared by all 6 boxes");
        ControlPanel.Section callbacks = panel.addSection("Try disabling callbacks");
        callbacks.addBoolean("enabled", demo::callbacksEnabled, demo::setCallbacksEnabled);
        callbacks.addText("before calls", demo::beforeInvocationCount);
        callbacks.addText("after calls", demo::afterInvocationCount);
        callbacks.addButton("reset counts", demo::resetInvocationCounts);
        return panel;
    }

    /** Associates one top-row mesh with the color applied immediately before its draw. */
    private record CallbackBox(Mesh mesh, Color color) {}

    /** Owns the explicit, inspectable callback state used by the demonstration. */
    private static final class CallbackDemo {
        private final BasicMaterial sharedMaterial;
        private final List<CallbackBox> callbackBoxes;
        private boolean callbacksEnabled;
        private long beforeInvocationCount;
        private long afterInvocationCount;

        /** Retains the shared resources and enables the demonstration callbacks. */
        private CallbackDemo(BasicMaterial sharedMaterial, List<CallbackBox> callbackBoxes) {
            this.sharedMaterial = sharedMaterial;
            this.callbackBoxes = List.copyOf(callbackBoxes);
            setCallbacksEnabled(true);
        }

        /** Returns whether the top row currently customizes the material per draw. */
        private boolean callbacksEnabled() {
            return callbacksEnabled;
        }

        /** Enables or clears all top-row callbacks and restores the shared base color. */
        private void setCallbacksEnabled(boolean enabled) {
            if (callbacksEnabled == enabled) {
                return;
            }
            callbacksEnabled = enabled;
            callbackBoxes.forEach(this::configureCallbacks);
            sharedMaterial.setColor(Color.WHITE);
        }

        /** Configures or clears the callback pair for one top-row box. */
        private void configureCallbacks(CallbackBox callbackBox) {
            Mesh mesh = callbackBox.mesh();
            if (!callbacksEnabled) {
                mesh.clearBeforeRenderCallback();
                mesh.clearAfterRenderCallback();
                return;
            }
            mesh.setBeforeRenderCallback(ignored -> {
                beforeInvocationCount++;
                sharedMaterial.setColor(callbackBox.color());
            });
            mesh.setAfterRenderCallback(ignored -> {
                afterInvocationCount++;
                sharedMaterial.setColor(Color.WHITE);
            });
        }

        /** Describes the visible state of the top row. */
        private String topRowDescription() {
            return callbacksEnabled ? "callbacks: red / green / blue" : "callbacks disabled: white";
        }

        /** Returns the live number of before-render invocations. */
        private String beforeInvocationCount() {
            return Long.toString(beforeInvocationCount);
        }

        /** Returns the live number of after-render invocations. */
        private String afterInvocationCount() {
            return Long.toString(afterInvocationCount);
        }

        /** Resets both live invocation counters without changing callback configuration. */
        private void resetInvocationCounts() {
            beforeInvocationCount = 0L;
            afterInvocationCount = 0L;
        }
    }
}
