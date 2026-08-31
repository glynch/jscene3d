/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays a rotating cube using automatic transforms and typed custom uniforms. */
public final class ShaderMaterialExample {
    private static final String VERTEX_SHADER = """
            in vec3 position;

            uniform mat4 modelViewMatrix;
            uniform mat4 projectionMatrix;

            out vec3 localPosition;

            void main() {
                localPosition = position;
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
            """;
    private static final String FRAGMENT_SHADER = """
            in vec3 localPosition;

            uniform vec3 tint;
            uniform float time;

            out vec4 fragmentColor;

            void main() {
                float brightness = 1.0;
            #ifdef PULSE
                brightness = 0.7 + 0.3 * sin(time + localPosition.y * 3.0);
            #endif
                fragmentColor = vec4(tint * brightness, 1.0);
            }
            """;

    /** Prevents instantiation of this example entry point. */
    private ShaderMaterialExample() {
        throw new AssertionError("ShaderMaterialExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Shader Material", ShaderMaterialExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.4f, 1.4f, 1.4f);
        ShaderMaterial material = createMaterial();
        Mesh cube = new Mesh(geometry, material);
        Scene scene = new Scene();
        scene.setBackground(Color.BLACK);
        scene.add(cube);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 3.0f);
        SceneExample example = new SceneExample(context, scene, camera);
        example.own(geometry);
        example.own(material);
        float[] time = {0.0f};
        example.setFrameAction((ignored, frame) -> {
            time[0] = (time[0] + frame.elapsedSeconds()) % TWO_PI;
            material.setUniform("time", time[0]);
            cube.rotateX(frame.elapsedSeconds() * 0.36f);
            cube.rotateY(frame.elapsedSeconds() * 0.6f);
        });
        return example;
    }

    /** Creates immutable shader structure and initial application-controlled uniforms. */
    private static ShaderMaterial createMaterial() {
        ShaderMaterial material = ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                .define("PULSE")
                .build();
        material.setUniform("tint", Color.CYAN);
        material.setUniform("time", 0.0f);
        return material;
    }
}
