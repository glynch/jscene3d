/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.core.Angles.TWO_PI;

import io.github.glynch.jscene3d.core.BoxGeometry;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.core.ShaderMaterial;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;

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
        try (Window window = Window.create("JScene3D - Shader Material");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = BoxGeometry.create(1.4f, 1.4f, 1.4f);
                ShaderMaterial material = createMaterial()) {
            Mesh cube = new Mesh(geometry, material);
            Scene scene = new Scene();
            scene.setBackground(Color.BLACK);
            scene.add(cube);

            PerspectiveCamera camera =
                    new PerspectiveCamera(PI_OVER_THREE, window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 3.0f);
            window.show();

            float time = 0.0f;
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
                time = (time + 0.02f) % TWO_PI;
                material.setUniform("time", time);
                cube.rotateX(0.006f);
                cube.rotateY(0.01f);
                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
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
