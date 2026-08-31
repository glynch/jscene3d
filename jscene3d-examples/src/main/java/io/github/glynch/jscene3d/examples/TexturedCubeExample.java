/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BoxGeometry;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.core.Texture;
import io.github.glynch.jscene3d.loaders.TextureLoader;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import java.nio.file.Path;

/** Displays a rotating textured cube using a generated checkerboard or caller-supplied image. */
public final class TexturedCubeExample {
    private static final int CHECKERBOARD_SIZE = 8;

    /** Prevents instantiation of this example entry point. */
    private TexturedCubeExample() {
        throw new AssertionError("TexturedCubeExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments optional first PNG or JPEG path; a checkerboard is generated when omitted
     */
    public static void main(String[] arguments) {
        try (Window window = Window.create("JScene3D - Textured Cube");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = BoxGeometry.create(1.4f, 1.4f, 1.4f);
                Texture texture = createTexture(arguments);
                BasicMaterial material = createMaterial(texture)) {
            Scene scene = new Scene();
            Mesh cube = new Mesh(geometry, material);
            scene.add(cube);

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 3.0f);
            window.show();

            while (!window.shouldClose()) {
                Window.pollEvents();
                if (window.input().wasKeyPressed(Key.ESCAPE)) {
                    window.requestClose();
                }
                if (window.framebufferSizeChanged()
                        && window.framebufferWidth() > 0
                        && window.framebufferHeight() > 0) {
                    camera.setAspectRatio(window.framebufferAspectRatio());
                }
                cube.rotateX(0.006f);
                cube.rotateY(0.01f);
                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
    }

    /** Loads the optional image or creates the built-in checkerboard. */
    private static Texture createTexture(String[] arguments) {
        if (arguments.length > 0) {
            return TextureLoader.load(Path.of(arguments[0]));
        }
        byte[] pixels = new byte[CHECKERBOARD_SIZE * CHECKERBOARD_SIZE * 4];
        for (int y = 0; y < CHECKERBOARD_SIZE; y++) {
            for (int x = 0; x < CHECKERBOARD_SIZE; x++) {
                boolean alternate = ((x / 2) + (y / 2)) % 2 == 0;
                int offset = (y * CHECKERBOARD_SIZE + x) * 4;
                pixels[offset] = alternate ? (byte) 0x10 : (byte) 0xff;
                pixels[offset + 1] = alternate ? (byte) 0xc0 : (byte) 0x40;
                pixels[offset + 2] = alternate ? (byte) 0xff : (byte) 0x90;
                pixels[offset + 3] = (byte) 0xff;
            }
        }
        return Texture.baseColor(CHECKERBOARD_SIZE, CHECKERBOARD_SIZE, pixels);
    }

    /** Creates the basic material that shares the supplied color map. */
    private static BasicMaterial createMaterial(Texture texture) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(texture);
        return material;
    }
}
