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
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Billboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;

/** Compares upright cylindrical billboards with fully camera-facing spherical billboards. */
public final class BillboardExample {
    private static final int CHARACTER_WIDTH = 96;
    private static final int CHARACTER_HEIGHT = 128;
    private static final int MARKER_SIZE = 96;

    /** Prevents instantiation of this example entry point. */
    private BillboardExample() {
        throw new AssertionError("BillboardExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Orbit above and around the scene. The character cards turn only around the vertical axis,
     * while the floating markers tilt to face the camera in every direction.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Billboards", BillboardExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        Texture cyanCharacter = createCharacterTexture(0x12d9d0, 0x063f4c);
        Texture magentaCharacter = createCharacterTexture(0xff4f9a, 0x60133d);
        Texture yellowMarker = createMarkerTexture(0xffc857, 0xff7a00);
        Texture blueMarker = createMarkerTexture(0x5ce1ff, 0x3167ff);
        BasicMaterial cyanMaterial = createMaskedMaterial(cyanCharacter);
        BasicMaterial magentaMaterial = createMaskedMaterial(magentaCharacter);
        BasicMaterial yellowMaterial = createBlendedMaterial(yellowMarker);
        BasicMaterial blueMaterial = createBlendedMaterial(blueMarker);
        BufferGeometry groundGeometry = PlaneGeometry.create(12.0f, 10.0f);
        BasicMaterial groundMaterial = new BasicMaterial(Color.srgb(0x14243a));
        groundMaterial.setSide(MaterialSide.DOUBLE);

        Billboard cyan = createCharacter(cyanMaterial, -2.2f, 0.0f, 0.7f);
        Billboard magenta = createCharacter(magentaMaterial, 2.2f, 0.0f, -0.5f);
        Billboard yellow = createMarker(yellowMaterial, -2.2f, 3.1f, -0.2f);
        Billboard blue = createMarker(blueMaterial, 2.2f, 3.1f, -1.4f);
        Scene scene = createScene(groundGeometry, groundMaterial, cyan, magenta, yellow, blue);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 50.0f);
        camera.setPosition(8.0f, 6.0f, 10.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.5f, 0.0f);
        controls.setDistanceLimits(5.0f, 24.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        ownResources(
                example,
                cyanCharacter,
                magentaCharacter,
                yellowMarker,
                blueMarker,
                cyanMaterial,
                magentaMaterial,
                yellowMaterial,
                blueMaterial,
                groundGeometry,
                groundMaterial,
                cyan,
                magenta,
                yellow,
                blue);
        ControlPanel panel = example.addOverlay(createPanel(context, controls));
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> panel.update());
        return example;
    }

    /** Registers resources in dependency order so dependants close first. */
    private static void ownResources(SceneExample example, AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            example.own(resource);
        }
    }

    /** Creates the scene and its horizontal reference plane. */
    private static Scene createScene(
            BufferGeometry groundGeometry,
            BasicMaterial groundMaterial,
            Billboard cyan,
            Billboard magenta,
            Billboard yellow,
            Billboard blue) {
        Mesh ground = new Mesh(groundGeometry, groundMaterial);
        ground.rotateX(-PI_OVER_TWO);

        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x07111f));
        scene.add(ground);
        scene.add(cyan);
        scene.add(magenta);
        scene.add(yellow);
        scene.add(blue);
        return scene;
    }

    /** Creates one bottom-centred sprite that remains vertical while facing the camera. */
    private static Billboard createCharacter(BasicMaterial material, float x, float y, float z) {
        Billboard billboard = new Billboard(material);
        billboard.setAlignment(BillboardAlignment.CYLINDRICAL);
        billboard.setAnchor(0.5f, 0.0f);
        billboard.setPosition(x, y, z);
        billboard.setScale(1.8f, 3.0f, 1.0f);
        return billboard;
    }

    /** Creates one centred sprite that follows both camera yaw and pitch. */
    private static Billboard createMarker(BasicMaterial material, float x, float y, float z) {
        Billboard billboard = new Billboard(material);
        billboard.setPosition(x, y, z);
        billboard.setScale(1.2f, 1.2f, 1.0f);
        return billboard;
    }

    /** Creates the compact behavioral explanation and camera reset control. */
    private static ControlPanel createPanel(ExampleContext context, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Billboards");
        ControlPanel.Section modes = panel.addSection("Alignment modes");
        modes.addText("characters", () -> "cylindrical: yaw only");
        modes.addText("markers", () -> "spherical: yaw and pitch");
        ControlPanel.Section anchors = panel.addSection("Anchors");
        anchors.addText("characters", () -> "bottom centre");
        anchors.addText("markers", () -> "centre");
        ControlPanel.Section view = panel.addSection("View");
        view.addText("camera", () -> "drag / scroll");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Creates a cutout material for a sharply edged character sprite. */
    private static BasicMaterial createMaskedMaterial(Texture texture) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(texture);
        material.setAlphaMode(AlphaMode.MASK);
        material.setAlphaCutoff(0.5f);
        return material;
    }

    /** Creates a blended material for a soft-edged floating marker. */
    private static BasicMaterial createBlendedMaterial(Texture texture) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(texture);
        material.setAlphaMode(AlphaMode.BLEND);
        material.setDepthWriteEnabled(false);
        return material;
    }

    /** Generates a transparent, two-tone character icon. */
    private static Texture createCharacterTexture(int bodyColor, int detailColor) {
        byte[] pixels = new byte[CHARACTER_WIDTH * CHARACTER_HEIGHT * 4];
        for (int y = 0; y < CHARACTER_HEIGHT; y++) {
            for (int x = 0; x < CHARACTER_WIDTH; x++) {
                writePixel(pixels, CHARACTER_WIDTH, x, y, characterColor(x, y, bodyColor, detailColor));
            }
        }
        return Texture.baseColor(CHARACTER_WIDTH, CHARACTER_HEIGHT, pixels);
    }

    /** Resolves one character pixel as packed RGBA. */
    private static int characterColor(int x, int y, int bodyColor, int detailColor) {
        boolean head = insideEllipse(x, y, 48, 28, 21, 21);
        boolean torso = x >= 23 && x <= 72 && y >= 48 && y <= 102;
        boolean leftLeg = x >= 25 && x <= 43 && y >= 94 && y <= 126;
        boolean rightLeg = x >= 53 && x <= 71 && y >= 94 && y <= 126;
        if (!(head || torso || leftLeg || rightLeg)) {
            return 0;
        }
        boolean visor = y >= 23 && y <= 34 && x >= 34 && x <= 62;
        boolean belt = y >= 76 && y <= 84;
        return packedRgba(visor || belt ? detailColor : bodyColor, 255);
    }

    /** Generates a transparent marker with a soft halo and opaque centre. */
    private static Texture createMarkerTexture(int innerColor, int outerColor) {
        byte[] pixels = new byte[MARKER_SIZE * MARKER_SIZE * 4];
        for (int y = 0; y < MARKER_SIZE; y++) {
            for (int x = 0; x < MARKER_SIZE; x++) {
                writePixel(pixels, MARKER_SIZE, x, y, markerColor(x, y, innerColor, outerColor));
            }
        }
        return Texture.baseColor(MARKER_SIZE, MARKER_SIZE, pixels);
    }

    /** Resolves one radial marker pixel as packed RGBA. */
    private static int markerColor(int x, int y, int innerColor, int outerColor) {
        float dx = x - (MARKER_SIZE - 1) * 0.5f;
        float dy = y - (MARKER_SIZE - 1) * 0.5f;
        float distance = (float) Math.sqrt(dx * dx + dy * dy) / (MARKER_SIZE * 0.5f);
        if (distance >= 1.0f) {
            return 0;
        }
        int color = distance < 0.58f ? innerColor : outerColor;
        int alpha = Math.round(255.0f * Math.clamp((1.0f - distance) * 4.0f, 0.0f, 1.0f));
        return packedRgba(color, alpha);
    }

    /** Returns whether one integer pixel lies inside an ellipse. */
    private static boolean insideEllipse(int x, int y, int centerX, int centerY, int radiusX, int radiusY) {
        float normalizedX = (x - centerX) / (float) radiusX;
        float normalizedY = (y - centerY) / (float) radiusY;
        return normalizedX * normalizedX + normalizedY * normalizedY <= 1.0f;
    }

    /** Combines an RGB hexadecimal color and alpha byte into packed RGBA. */
    private static int packedRgba(int rgb, int alpha) {
        return rgb << 8 | alpha;
    }

    /** Writes one packed RGBA pixel into a top-row-first byte array. */
    private static void writePixel(byte[] pixels, int width, int x, int y, int rgba) {
        int offset = (y * width + x) * 4;
        pixels[offset] = (byte) (rgba >>> 24);
        pixels[offset + 1] = (byte) (rgba >>> 16);
        pixels[offset + 2] = (byte) (rgba >>> 8);
        pixels[offset + 3] = (byte) rgba;
    }
}
