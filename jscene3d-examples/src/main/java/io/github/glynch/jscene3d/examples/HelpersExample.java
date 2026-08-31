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
import io.github.glynch.jscene3d.helpers.AxesHelper;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays an XZ reference grid and colored positive coordinate axes. */
public final class HelpersExample {
    /** Prevents instantiation of this example entry point. */
    private HelpersExample() {
        throw new AssertionError("HelpersExample cannot be instantiated");
    }

    /**
     * Runs this example as an independent native application.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Helpers", HelpersExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        AxesHelper axes = new AxesHelper(3.0f);
        GridHelper grid = new GridHelper(10.0f, 10);
        Scene scene = new Scene();
        scene.setBackground(Color.BLACK);
        scene.add(grid);
        axes.setRenderOrder(1);
        scene.add(axes);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(6.0f, 5.0f, 7.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(axes);
        example.own(grid);
        return example;
    }
}
