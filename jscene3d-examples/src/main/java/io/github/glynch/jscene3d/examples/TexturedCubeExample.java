/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.loaders.TextureLoader;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
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
        String[] copiedArguments = arguments.clone();
        ExampleLauncher.launch("JScene3D - Textured Cube", context -> create(context, copiedArguments));
    }

    /** Creates the built-in checkerboard variant for the example browser. */
    static HostedExample create(ExampleContext context) {
        return create(context, new String[0]);
    }

    /** Creates the shared hosted implementation with optional texture-path arguments. */
    private static HostedExample create(ExampleContext context, String[] arguments) {
        BufferGeometry geometry = BoxGeometry.create(1.4f, 1.4f, 1.4f);
        Texture texture = createTexture(arguments);
        BasicMaterial material = createMaterial(texture);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        Mesh cube = new Mesh(geometry, material);
        scene.add(cube);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 3.0f);
        SceneExample example = new SceneExample(context, scene, camera);
        example.own(geometry);
        example.own(texture);
        example.own(material);
        example.setFrameAction((ignored, frame) -> {
            cube.rotateX(frame.elapsedSeconds() * 0.36f);
            cube.rotateY(frame.elapsedSeconds() * 0.6f);
        });
        return example;
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
