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
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.helpers.BoxHelper;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;

/** Displays dynamic world-axis-aligned bounds around a rotating object hierarchy. */
public final class BoxHelperExample {
    /** Prevents instantiation of this example entry point. */
    private BoxHelperExample() {
        throw new AssertionError("BoxHelperExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * <p>Drag with the left mouse button to orbit, drag with the right mouse button to pan, and use
     * the scroll wheel to dolly.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Box Helper", BoxHelperExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry geometry = BoxGeometry.create(1.5f, 1.0f, 1.0f);
        BasicMaterial cyanMaterial = new BasicMaterial(Color.CYAN);
        BasicMaterial magentaMaterial = new BasicMaterial(Color.MAGENTA);
        Group target = createTarget(geometry, cyanMaterial, magentaMaterial);
        BoxHelper helper = new BoxHelper(target, Color.YELLOW);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x080b12));
        scene.add(target);
        scene.add(helper);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(6.0f, 4.0f, 8.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(geometry);
        example.own(cyanMaterial);
        example.own(magentaMaterial);
        example.own(helper);
        example.setFrameAction((ignored, frame) -> {
            target.rotateY(frame.elapsedSeconds() * 0.6f);
            helper.update();
        });
        return example;
    }

    /** Creates two offset boxes beneath one animated target group. */
    private static Group createTarget(
            BufferGeometry geometry, BasicMaterial cyanMaterial, BasicMaterial magentaMaterial) {
        Mesh left = new Mesh(geometry, cyanMaterial);
        left.setPosition(-1.25f, 0.0f, 0.0f);
        left.rotateZ(0.25f);
        Mesh right = new Mesh(geometry, magentaMaterial);
        right.setPosition(1.25f, 0.5f, 0.5f);
        right.setScale(0.75f, 1.5f, 0.75f);
        Group group = new Group();
        group.add(left);
        group.add(right);
        return group;
    }
}
