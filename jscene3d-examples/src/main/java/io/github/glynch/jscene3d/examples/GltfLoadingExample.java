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
import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Object3D;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/** Loads and displays a bundled static glTF 2.0 metallic-roughness scene. */
public final class GltfLoadingExample {
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/gltf/imported-cubes.gltf";

    /** Prevents instantiation of this example entry point. */
    private GltfLoadingExample() {
        throw new AssertionError("GltfLoadingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - glTF Loading", GltfLoadingExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded = GltfLoader.load(modelPath());
        loaded.scene().setBackground(Color.srgb(0x07090f));
        loaded.scene().add(new AmbientLight(Color.srgb(0x88a4d8), 0.2f));
        DirectionalLight key = new DirectionalLight(Color.srgb(0xffe2b8), 2.5f);
        key.setPosition(-5.0f, 7.0f, 8.0f);
        loaded.scene().add(key);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(7.0f, 5.0f, 10.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 0.2f, 0.0f);
        controls.setDistanceLimits(5.0f, 25.0f);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, loaded.scene(), camera, controls);
        example.own(loaded);
        Object3D importedRoot = loaded.scene().children().getFirst();
        example.setFrameAction((ignored, frame) -> importedRoot.rotateY(frame.elapsedSeconds() * 0.18f));
        return example;
    }

    /** Resolves the bundled project-authored model while running from the examples build. */
    private static Path modelPath() {
        URL resource = Objects.requireNonNull(GltfLoadingExample.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid bundled glTF resource URI", exception);
        }
    }
}
