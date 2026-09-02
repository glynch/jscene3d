/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.loaders.TextureLoader;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Billboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureRegion;

/** Compares upright cylindrical billboards with fully camera-facing spherical billboards. */
public final class BillboardExample {
    private static final String ATLAS_RESOURCE =
            "/META-INF/jscene3d/examples/assets/kenney/new-platformer-pack/characters.png";
    private static final int FRAME_SIZE = 128;
    private static final int ATLAS_SIZE = 902;

    /** Prevents instantiation of this example entry point. */
    private BillboardExample() {
        throw new AssertionError("BillboardExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Orbit above and around the scene. The grounded explorers turn only around the vertical
     * axis, while the floating explorers tilt to face the camera in every direction.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Billboards", BillboardExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        Texture atlas = loadAtlas();
        BasicMaterial spriteMaterial = createSpriteMaterial(atlas);
        Billboard green = createBillboard(spriteMaterial, region(645, 129), BillboardAlignment.CYLINDRICAL, true);
        Billboard pink = createBillboard(spriteMaterial, region(0, 387), BillboardAlignment.CYLINDRICAL, true);
        Billboard beige = createBillboard(spriteMaterial, region(774, 0), BillboardAlignment.SPHERICAL, false);
        Billboard yellow = createBillboard(spriteMaterial, region(0, 774), BillboardAlignment.SPHERICAL, false);
        placeBillboard(green, -2.3f, 0.14f, 0.6f, 2.8f);
        placeBillboard(pink, 2.3f, 0.14f, -0.4f, 2.8f);
        placeBillboard(beige, -2.3f, 3.3f, -0.5f, 1.9f);
        placeBillboard(yellow, 2.3f, 3.3f, -1.5f, 1.9f);

        BufferGeometry groundGeometry = PlaneGeometry.create(14.0f, 10.0f);
        BasicMaterial groundMaterial = new BasicMaterial(Color.srgb(0x14283b));
        groundMaterial.setSide(MaterialSide.DOUBLE);
        BufferGeometry platformGeometry = CylinderGeometry.create(1.2f, 0.28f);
        BasicMaterial greenPlatformMaterial = new BasicMaterial(Color.srgb(0x227b79));
        BasicMaterial pinkPlatformMaterial = new BasicMaterial(Color.srgb(0x944765));
        Scene scene = createScene();
        addGround(scene, groundGeometry, groundMaterial);
        addPlatform(scene, platformGeometry, greenPlatformMaterial, -2.3f, 0.6f);
        addPlatform(scene, platformGeometry, pinkPlatformMaterial, 2.3f, -0.4f);
        scene.add(green);
        scene.add(pink);
        scene.add(beige);
        scene.add(yellow);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 50.0f);
        camera.setPosition(7.5f, 5.8f, 10.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.6f, 0.0f);
        controls.setDistanceLimits(5.0f, 24.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        ownResources(
                example,
                atlas,
                spriteMaterial,
                groundGeometry,
                groundMaterial,
                platformGeometry,
                greenPlatformMaterial,
                pinkPlatformMaterial,
                green,
                pink,
                beige,
                yellow);
        ControlPanel panel = example.addOverlay(createPanel(context, controls));
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> panel.update());
        return example;
    }

    /** Creates one billboard backed by a region of the shared Kenney character atlas. */
    private static Billboard createBillboard(
            BasicMaterial material, TextureRegion textureRegion, BillboardAlignment alignment, boolean anchoredToFeet) {
        Billboard billboard = new Billboard(material);
        billboard.setTextureRegion(textureRegion);
        billboard.setAlignment(alignment);
        if (anchoredToFeet) {
            billboard.setAnchor(0.5f, 0.0f);
        }
        return billboard;
    }

    /** Positions and uniformly sizes one billboard without mixing placement into resource setup. */
    private static void placeBillboard(Billboard billboard, float x, float y, float z, float size) {
        billboard.setPosition(x, y, z);
        billboard.setScale(size, size, 1.0f);
    }

    /** Creates the dark gallery background. */
    private static Scene createScene() {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x071525));
        return scene;
    }

    /** Adds the horizontal reference plane slightly below the display platforms. */
    private static void addGround(Scene scene, BufferGeometry geometry, BasicMaterial material) {
        Mesh ground = new Mesh(geometry, material);
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -0.15f, 0.0f);
        scene.add(ground);
    }

    /** Adds one colour-coded display platform beneath an upright character. */
    private static void addPlatform(Scene scene, BufferGeometry geometry, BasicMaterial material, float x, float z) {
        Mesh platform = new Mesh(geometry, material);
        platform.setPosition(x, 0.0f, z);
        scene.add(platform);
    }

    /** Creates the behavioral explanation and camera reset control. */
    private static ControlPanel createPanel(ExampleContext context, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Billboards");
        ControlPanel.Section modes = panel.addSection("Alignment modes");
        modes.addText("grounded explorers", () -> "cylindrical: yaw only");
        modes.addText("floating explorers", () -> "spherical: yaw and pitch");
        ControlPanel.Section anchors = panel.addSection("Anchors");
        anchors.addText("grounded explorers", () -> "bottom centre");
        anchors.addText("floating explorers", () -> "centre");
        ControlPanel.Section resource = panel.addSection("Artwork");
        resource.addText("source", () -> "Kenney New Platformer Pack");
        resource.addText("storage", () -> "one shared texture atlas");
        ControlPanel.Section view = panel.addSection("View");
        view.addText("camera", () -> "drag / scroll");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Creates a smoothly blended material using the shared sprite atlas. */
    private static BasicMaterial createSpriteMaterial(Texture atlas) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(atlas);
        material.setAlphaMode(AlphaMode.BLEND);
        material.setDepthWriteEnabled(false);
        return material;
    }

    /** Loads the required archive-safe CC0 character atlas. */
    private static Texture loadAtlas() {
        return TextureLoader.load(path(BillboardExample.class.getResource(ATLAS_RESOURCE), ATLAS_RESOURCE));
    }

    /** Returns one top-row-first region from Kenney's documented atlas metadata. */
    private static TextureRegion region(int x, int y) {
        return TextureRegion.fromPixels(x, y, FRAME_SIZE, FRAME_SIZE, ATLAS_SIZE, ATLAS_SIZE);
    }

    /** Registers resources in dependency order so dependants close first. */
    private static void ownResources(SceneExample example, AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            example.own(resource);
        }
    }
}
