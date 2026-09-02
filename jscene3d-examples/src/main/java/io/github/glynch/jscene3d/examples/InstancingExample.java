/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.fogs.ExponentialSquaredFog;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.DirectionalLightShadow;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.raycasting.RaycastHit;
import io.github.glynch.jscene3d.raycasting.Raycaster;
import io.github.glynch.jscene3d.render.RenderStatistics;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;

/** Compares many ordinary meshes with one GPU-instanced mesh using the same scene content. */
public final class InstancingExample {
    private static final int GRID_WIDTH = 20;
    private static final int CAPACITY = GRID_WIDTH * GRID_WIDTH;
    private static final int DEFAULT_COUNT = 225;
    private static final List<Color> PALETTE = List.of(
            Color.srgb(0x00d9ff),
            Color.srgb(0xff4f9a),
            Color.srgb(0xffc857),
            Color.srgb(0x7cf29c),
            Color.srgb(0xa78bfa),
            Color.srgb(0xff7a45));
    private static final List<ControlPanel.Choice<RenderMode>> MODE_CHOICES = List.of(
            new ControlPanel.Choice<>(RenderMode.INSTANCED, "one instanced mesh"),
            new ControlPanel.Choice<>(RenderMode.ORDINARY, "ordinary meshes"));

    /** Prevents instantiation of this example entry point. */
    private InstancingExample() {
        throw new AssertionError("InstancingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Switch between rendering modes to compare draw calls for identical animated objects.
     * Click an object to select its stable instance index.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Instancing", InstancingExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry torusGeometry = TorusGeometry.create(0.46f, 0.17f, 10, 18);
        BufferGeometry groundGeometry = PlaneGeometry.create(34.0f, 34.0f);
        PhongMaterial instancedMaterial = createPhongMaterial(Color.WHITE);
        List<PhongMaterial> ordinaryMaterials =
                PALETTE.stream().map(InstancingExample::createPhongMaterial).toList();
        LambertMaterial groundMaterial = new LambertMaterial(Color.srgb(0x172538));
        groundMaterial.setSide(MaterialSide.DOUBLE);

        Scene scene = new Scene();
        Color atmosphere = Color.srgb(0x07111f);
        scene.setBackground(atmosphere);
        scene.setFog(new ExponentialSquaredFog(atmosphere, 0.026f));
        InstancingDemo demo = new InstancingDemo(torusGeometry, instancedMaterial, ordinaryMaterials);
        scene.add(demo.ordinaryGroup());
        scene.add(demo.instancedMesh());
        scene.add(createGround(groundGeometry, groundMaterial));
        scene.add(new HemisphereLight(Color.srgb(0xbce9ff), Color.srgb(0x18251c), 1.5f));
        scene.add(createKeyLight());

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 90.0f);
        camera.setPosition(15.0f, 13.0f, 18.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.0f, 0.0f);
        controls.setDistanceLimits(8.0f, 55.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(torusGeometry);
        example.own(groundGeometry);
        example.own(instancedMaterial);
        for (PhongMaterial material : ordinaryMaterials) {
            example.own(material);
        }
        example.own(groundMaterial);
        ControlPanel panel = example.addOverlay(createPanel(context, demo, controls));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        Raycaster raycaster = new Raycaster();
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            if (!frame.pointerCaptured() && !panel.capturesPointer()) {
                demo.selectFromPointer(context, camera, scene, raycaster);
            }
            demo.update(frame.elapsedSeconds());
            fps.update();
        });
        return example;
    }

    /** Creates a glossy material whose base color is supplied by an object or instance. */
    private static PhongMaterial createPhongMaterial(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecular(Color.WHITE);
        material.setShininess(72.0f);
        return material;
    }

    /** Creates the shadow-receiving comparison floor. */
    private static Mesh createGround(BufferGeometry geometry, LambertMaterial material) {
        Mesh ground = new Mesh(geometry, material);
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -1.0f, 0.0f);
        ground.setShadowReceivingEnabled(true);
        return ground;
    }

    /** Creates the shared directional light and a map encompassing the complete grid. */
    private static DirectionalLight createKeyLight() {
        DirectionalLight light = new DirectionalLight(Color.srgb(0xffe4c1), 2.8f);
        light.setPosition(-10.0f, 17.0f, 9.0f);
        light.setTarget(0.0f, 0.0f, 0.0f);
        light.setShadowCastingEnabled(true);
        DirectionalLightShadow shadow = light.shadow();
        shadow.setMapSize(1024, 1024);
        shadow.setCameraBounds(-18.0f, 18.0f, -18.0f, 18.0f);
        shadow.setCameraRange(0.5f, 55.0f);
        shadow.setBias(0.0015f);
        shadow.setNormalBias(0.045f);
        return light;
    }

    /** Creates live comparison, animation, selection, and renderer-statistics controls. */
    private static ControlPanel createPanel(ExampleContext context, InstancingDemo demo, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Instancing");
        ControlPanel.Section comparison = panel.addSection("Comparison");
        comparison.addRadioGroup("mode", demo::mode, demo::setMode, MODE_CHOICES);
        comparison.addFloat("object count", demo::count, demo::setCount, 1.0f, CAPACITY);
        comparison.addBoolean("instance colors", demo::colorsEnabled, demo::setColorsEnabled);
        comparison.addBoolean("animate", demo::animated, demo::setAnimated);
        ControlPanel.Section selection = panel.addSection("Selection");
        selection.addText("selected index", demo::selectedIndexText);
        selection.addButton("clear selection", demo::clearSelection);
        ControlPanel.Section statistics = panel.addSection("Renderer statistics");
        statistics.addText("main draws", () -> mainDraws(context));
        statistics.addText("shadow draws", () -> shadowDraws(context));
        statistics.addText("submitted instances", () -> submittedInstances(context));
        statistics.addText("instance uploads", () -> instanceUploads(context));
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Formats the current main-pass draw count. */
    private static String mainDraws(ExampleContext context) {
        return Integer.toString(statistics(context).drawCalls());
    }

    /** Formats the current shadow-pass draw count. */
    private static String shadowDraws(ExampleContext context) {
        return Integer.toString(statistics(context).shadowDrawCalls());
    }

    /** Formats the current submitted mesh-instance count. */
    private static String submittedInstances(ExampleContext context) {
        return Long.toString(statistics(context).renderedInstances());
    }

    /** Formats current instance-buffer upload activity. */
    private static String instanceUploads(ExampleContext context) {
        RenderStatistics statistics = statistics(context);
        return statistics.bufferUploads() + " / " + statistics.bufferUploadBytes() + " bytes";
    }

    /** Returns the renderer's latest completed scene statistics. */
    private static RenderStatistics statistics(ExampleContext context) {
        return context.renderer().info().statistics();
    }

    /** Comparison presentation selected by the live panel. */
    private enum RenderMode {
        INSTANCED,
        ORDINARY
    }

    /** Owns the equivalent ordinary and instanced object populations plus interaction state. */
    private static final class InstancingDemo {
        private final Group ordinaryGroup = new Group();
        private final List<Mesh> ordinaryMeshes = new ArrayList<>(CAPACITY);
        private final Map<Mesh, Integer> ordinaryIndices = new IdentityHashMap<>();
        private final InstancedMesh instancedMesh;
        private final List<PhongMaterial> ordinaryMaterials;
        private final Matrix4f transform = new Matrix4f();

        private RenderMode mode = RenderMode.INSTANCED;
        private int count = DEFAULT_COUNT;
        private int selectedIndex = -1;
        private float elapsedTime;
        private boolean colorsEnabled = true;
        private boolean animated = true;
        private boolean transformsDirty = true;

        /** Creates equivalent object populations sharing one geometry. */
        private InstancingDemo(
                BufferGeometry geometry, PhongMaterial instancedMaterial, List<PhongMaterial> ordinaryMaterials) {
            this.ordinaryMaterials = List.copyOf(ordinaryMaterials);
            instancedMesh = new InstancedMesh(geometry, instancedMaterial, CAPACITY);
            instancedMesh.setShadowCastingEnabled(true);
            for (int index = 0; index < CAPACITY; index++) {
                instancedMesh.setColorAt(index, color(index));
                Mesh mesh = new Mesh(geometry, this.ordinaryMaterials.get(index % this.ordinaryMaterials.size()));
                mesh.setShadowCastingEnabled(true);
                ordinaryMeshes.add(mesh);
                ordinaryIndices.put(mesh, index);
                ordinaryGroup.add(mesh);
            }
            instancedMesh.setCount(count);
            ordinaryGroup.setVisible(false);
            updateVisibility();
            applyTransforms();
        }

        /** Returns the ordinary scene-graph branch. */
        private Group ordinaryGroup() {
            return ordinaryGroup;
        }

        /** Returns the single instanced batch. */
        private InstancedMesh instancedMesh() {
            return instancedMesh;
        }

        /** Returns the selected comparison mode. */
        private RenderMode mode() {
            return mode;
        }

        /** Switches visible branches while retaining their equivalent transforms. */
        private void setMode(RenderMode mode) {
            this.mode = mode;
            boolean instanced = mode == RenderMode.INSTANCED;
            instancedMesh.setVisible(instanced);
            ordinaryGroup.setVisible(!instanced);
        }

        /** Returns the active object count as a GUI slider value. */
        private float count() {
            return count;
        }

        /** Applies a rounded active count without reallocating the instanced buffers. */
        private void setCount(float count) {
            int selectedCount = Math.clamp(Math.round(count), 1, CAPACITY);
            if (this.count == selectedCount) {
                return;
            }
            this.count = selectedCount;
            instancedMesh.setCount(selectedCount);
            if (selectedIndex >= selectedCount) {
                selectedIndex = -1;
            }
            updateVisibility();
            transformsDirty = true;
        }

        /** Returns whether per-object and per-instance palette colors are enabled. */
        private boolean colorsEnabled() {
            return colorsEnabled;
        }

        /** Enables palette colors or changes both populations to white. */
        private void setColorsEnabled(boolean enabled) {
            if (colorsEnabled == enabled) {
                return;
            }
            colorsEnabled = enabled;
            if (enabled) {
                restoreInstanceColors();
            } else {
                instancedMesh.clearInstanceColors();
            }
            for (int index = 0; index < ordinaryMaterials.size(); index++) {
                ordinaryMaterials.get(index).setColor(enabled ? PALETTE.get(index) : Color.WHITE);
            }
        }

        /** Restores each capacity slot's deterministic palette color. */
        private void restoreInstanceColors() {
            for (int index = 0; index < CAPACITY; index++) {
                instancedMesh.setColorAt(index, color(index));
            }
        }

        /** Returns whether wave motion is advancing. */
        private boolean animated() {
            return animated;
        }

        /** Starts or pauses deterministic transform animation. */
        private void setAnimated(boolean animated) {
            this.animated = animated;
        }

        /** Advances time and synchronizes both populations when necessary. */
        private void update(float elapsedSeconds) {
            if (animated) {
                elapsedTime += elapsedSeconds;
                transformsDirty = true;
            }
            if (transformsDirty) {
                applyTransforms();
            }
        }

        /** Selects the nearest active object under a primary-button press. */
        private void selectFromPointer(
                ExampleContext context, PerspectiveCamera camera, Scene scene, Raycaster raycaster) {
            if (!context.containsPointer() || !context.window().input().wasMouseButtonPressed(MouseButton.LEFT)) {
                return;
            }
            raycaster.setFromCamera(context.normalizedPointerX(), context.normalizedPointerY(), camera);
            selectedIndex = selectionIndex(raycaster.intersect(scene));
            transformsDirty = true;
        }

        /** Returns the first hit belonging to the currently visible comparison branch. */
        private int selectionIndex(List<RaycastHit> hits) {
            for (RaycastHit hit : hits) {
                if (hit.mesh() == instancedMesh && mode == RenderMode.INSTANCED) {
                    return hit.instanceIndex().orElse(-1);
                }
                Integer index = ordinaryIndices.get(hit.mesh());
                if (index != null && mode == RenderMode.ORDINARY) {
                    return index;
                }
            }
            return -1;
        }

        /** Clears the current selection and its scale highlight. */
        private void clearSelection() {
            selectedIndex = -1;
            transformsDirty = true;
        }

        /** Returns the current selected index for the control panel. */
        private String selectedIndexText() {
            return selectedIndex < 0 ? "none" : Integer.toString(selectedIndex);
        }

        /** Keeps the leading ordinary subset aligned with the instanced active count. */
        private void updateVisibility() {
            for (int index = 0; index < CAPACITY; index++) {
                ordinaryMeshes.get(index).setVisible(index < count);
            }
        }

        /** Applies identical positions, orientations, and scales to both representations. */
        private void applyTransforms() {
            for (int index = 0; index < count; index++) {
                float x = coordinate(index % GRID_WIDTH);
                float z = coordinate(index / GRID_WIDTH);
                float phase = index * 0.37f;
                float y = 0.25f + 0.42f * (float) Math.sin(elapsedTime * 1.8f + phase);
                float xRotation = 0.5f + 0.2f * (float) Math.sin(elapsedTime + phase);
                float yRotation = elapsedTime * 0.8f + phase;
                float scale = index == selectedIndex ? 1.45f : 1.0f;
                transform
                        .identity()
                        .translate(x, y, z)
                        .rotateX(xRotation)
                        .rotateY(yRotation)
                        .scale(scale);
                instancedMesh.setMatrixAt(index, transform);
                Mesh ordinary = ordinaryMeshes.get(index);
                ordinary.setPosition(x, y, z);
                ordinary.setRotationFromEuler(xRotation, yRotation, 0.0f, RotationOrder.XYZ);
                ordinary.setScale(scale, scale, scale);
            }
            transformsDirty = false;
        }

        /** Converts a grid coordinate into a centered world-space coordinate. */
        private static float coordinate(int index) {
            return (index - (GRID_WIDTH - 1) * 0.5f) * 1.25f;
        }

        /** Returns one stable palette color for an object index. */
        private static Color color(int index) {
            return PALETTE.get(index % PALETTE.size());
        }
    }
}
