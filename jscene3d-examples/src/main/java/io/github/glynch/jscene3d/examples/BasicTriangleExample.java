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
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays a rotating vertex-colored triangle with the public rendering API. */
public final class BasicTriangleExample {
    /** Prevents instantiation of this example entry point. */
    private BasicTriangleExample() {
        throw new AssertionError("BasicTriangleExample cannot be instantiated");
    }

    /**
     * Runs this example as an independent native application.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Basic Triangle", BasicTriangleExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BufferGeometry.builder()
                .positions(-0.8f, -0.7f, 0.0f, 0.8f, -0.7f, 0.0f, 0.0f, 0.8f, 0.0f)
                .vertexColors(Color.RED, Color.GREEN, Color.BLUE)
                .build();
        BasicMaterial material = createMaterial();
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050810));
        Mesh triangle = new Mesh(geometry, material);
        scene.add(triangle);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 2.0f);
        SceneExample example = new SceneExample(context, scene, camera);
        example.own(geometry);
        example.own(material);
        example.setFrameAction((ignored, frame) -> triangle.rotateY(frame.elapsedSeconds() * 0.6f));
        return example;
    }

    /** Creates the double-sided vertex-color material used by the rotating triangle. */
    private static BasicMaterial createMaterial() {
        BasicMaterial material = new BasicMaterial(Color.WHITE);
        material.setUsesVertexColors(true);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }
}
