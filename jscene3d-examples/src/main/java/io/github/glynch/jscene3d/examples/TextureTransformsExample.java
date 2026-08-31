/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;

/** Demonstrates interactive texture offset, repeat, rotation, center, and wrapping. */
public final class TextureTransformsExample {
    private static final int PATTERN_SIZE = 64;
    private static final int GRID_SIZE = 8;

    /** Prevents instantiation of this example entry point. */
    private TextureTransformsExample() {
        throw new AssertionError("TextureTransformsExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>The generated asymmetric UV pattern makes offset, rotation, and mirroring visible without
     * requiring an external image. Use the panel to edit the shared texture transform in real time.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Texture Transforms");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = PlaneGeometry.create(4.8f, 3.2f);
                Texture texture = createPatternTexture();
                BasicMaterial material = createMaterial(texture)) {
            Scene scene = new Scene();
            scene.setBackground(Color.srgb(0x080b12));
            scene.add(new Mesh(geometry, material));

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 4.2f);
            ControlPanel panel = createPanel(window, texture);
            FpsMonitor fpsMonitor = new FpsMonitor();
            window.show();

            while (!window.shouldClose()) {
                Window.pollEvents();
                handleWindowState(window, camera);
                panel.update();
                renderer.render(scene, camera);
                renderer.render(panel);
                renderer.render(fpsMonitor);
                window.swapBuffers();
                fpsMonitor.update();
            }
        }
    }

    /** Creates the transformed color map and enables repeat wrapping in both directions. */
    private static Texture createPatternTexture() {
        Texture texture = Texture.baseColor(PATTERN_SIZE, PATTERN_SIZE, createPatternPixels());
        texture.setMinificationFilter(TextureFilter.NEAREST_MIPMAP_LINEAR);
        texture.setMagnificationFilter(TextureFilter.NEAREST);
        texture.setHorizontalWrap(TextureWrap.REPEAT);
        texture.setVerticalWrap(TextureWrap.REPEAT);
        texture.setRepeat(2.0f, 2.0f);
        texture.setCenter(0.5f, 0.5f);
        return texture;
    }

    /** Creates a basic material sharing the interactive texture. */
    private static BasicMaterial createMaterial(Texture texture) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(texture);
        return material;
    }

    /** Creates the transform and wrapping controls. */
    private static ControlPanel createPanel(Window window, Texture texture) {
        ControlPanel panel = new ControlPanel(window, "Texture Transform");
        ControlPanel.Section transform = panel.addSection("Transform");
        transform.addFloat(
                "offset u", texture::offsetU, value -> texture.setOffset(value, texture.offsetV()), -1.0f, 1.0f);
        transform.addFloat(
                "offset v", texture::offsetV, value -> texture.setOffset(texture.offsetU(), value), -1.0f, 1.0f);
        transform.addFloat(
                "repeat u", texture::repeatU, value -> texture.setRepeat(value, texture.repeatV()), 0.25f, 5.0f);
        transform.addFloat(
                "repeat v", texture::repeatV, value -> texture.setRepeat(texture.repeatU(), value), 0.25f, 5.0f);
        transform.addFloat("rotation", texture::rotation, texture::setRotation, -PI, PI);
        transform.addFloat(
                "center u", texture::centerU, value -> texture.setCenter(value, texture.centerV()), 0.0f, 1.0f);
        transform.addFloat(
                "center v", texture::centerV, value -> texture.setCenter(texture.centerU(), value), 0.0f, 1.0f);
        transform.addButton("reset transform", () -> resetTransform(texture));

        ControlPanel.Section wrapping = panel.addSection("Wrapping");
        wrapping.addText("horizontal", () -> texture.horizontalWrap().name());
        wrapping.addButton("cycle horizontal", () -> texture.setHorizontalWrap(next(texture.horizontalWrap())));
        wrapping.addText("vertical", () -> texture.verticalWrap().name());
        wrapping.addButton("cycle vertical", () -> texture.setVerticalWrap(next(texture.verticalWrap())));
        return panel;
    }

    /** Applies close and aspect-ratio changes from the latest event poll. */
    private static void handleWindowState(Window window, PerspectiveCamera camera) {
        if (window.input().wasKeyPressed(Key.ESCAPE)) {
            window.requestClose();
        }
        if (window.framebufferSizeChanged() && window.framebufferWidth() > 0 && window.framebufferHeight() > 0) {
            camera.setAspectRatio(window.framebufferAspectRatio());
        }
    }

    /** Restores the example's initial transform while retaining the selected wrapping modes. */
    private static void resetTransform(Texture texture) {
        texture.setOffset(0.0f, 0.0f);
        texture.setRepeat(2.0f, 2.0f);
        texture.setRotation(0.0f);
        texture.setCenter(0.5f, 0.5f);
    }

    /** Returns the next wrap mode in stable display order. */
    private static TextureWrap next(TextureWrap wrap) {
        return switch (wrap) {
            case CLAMP_TO_EDGE -> TextureWrap.REPEAT;
            case REPEAT -> TextureWrap.MIRRORED_REPEAT;
            case MIRRORED_REPEAT -> TextureWrap.CLAMP_TO_EDGE;
        };
    }

    /** Creates an asymmetric four-quadrant image with a black grid. */
    private static byte[] createPatternPixels() {
        byte[] pixels = new byte[PATTERN_SIZE * PATTERN_SIZE * 4];
        for (int y = 0; y < PATTERN_SIZE; y++) {
            for (int x = 0; x < PATTERN_SIZE; x++) {
                writePatternPixel(pixels, x, y);
            }
        }
        return pixels;
    }

    /** Writes one opaque grid or quadrant pixel into the generated image. */
    private static void writePatternPixel(byte[] pixels, int x, int y) {
        int offset = (y * PATTERN_SIZE + x) * 4;
        if (x % GRID_SIZE == 0 || y % GRID_SIZE == 0) {
            setPixel(pixels, offset, 8, 10, 14);
        } else if (x < PATTERN_SIZE / 2 && y < PATTERN_SIZE / 2) {
            setPixel(pixels, offset, 255, 70, 90);
        } else if (x >= PATTERN_SIZE / 2 && y < PATTERN_SIZE / 2) {
            setPixel(pixels, offset, 70, 220, 120);
        } else if (x < PATTERN_SIZE / 2) {
            setPixel(pixels, offset, 60, 130, 255);
        } else {
            setPixel(pixels, offset, 255, 210, 50);
        }
    }

    /** Writes one opaque RGB pixel at a validated array offset. */
    private static void setPixel(byte[] pixels, int offset, int red, int green, int blue) {
        pixels[offset] = (byte) red;
        pixels[offset + 1] = (byte) green;
        pixels[offset + 2] = (byte) blue;
        pixels[offset + 3] = (byte) 0xff;
    }
}
