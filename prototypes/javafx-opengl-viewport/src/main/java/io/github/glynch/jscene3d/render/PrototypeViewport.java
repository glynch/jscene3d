/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.scenes.Scene;

/** Owns the actual JScene3D scene, renderer, and GPU resources displayed by the prototype. */
final class PrototypeViewport implements AutoCloseable {
    private static final float DRAG_SCALE = 0.008f;

    private final Renderer renderer;
    private final BufferGeometry geometry;
    private final BasicMaterial cyanMaterial;
    private final BasicMaterial greenMaterial;
    private final BasicMaterial orangeMaterial;
    private final Group model;
    private final Scene scene;
    private final PerspectiveCamera camera;

    private long frameCount;
    private boolean closed;

    /** Creates the renderer and a deliberately asymmetric three-box scene. */
    PrototypeViewport(RendererContext context) {
        renderer = Renderer.create(context, RendererOptions.defaults());
        geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
        cyanMaterial = new BasicMaterial(Color.srgb(0x23d9e8));
        greenMaterial = new BasicMaterial(Color.srgb(0x68c96a));
        orangeMaterial = new BasicMaterial(Color.srgb(0xffa33a));
        model = createModel();
        scene = new Scene();
        scene.setBackground(Color.srgb(0x101820));
        scene.add(model);
        camera = new PerspectiveCamera(PI_OVER_THREE, 1.0f, 0.1f, 100.0f);
        camera.setPosition(4.2f, 3.0f, 6.4f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
    }

    /** Renders one frame after applying the latest dimensions and JavaFX input. */
    void render(float elapsedSeconds, int width, int height, PrototypeInteraction.FrameInput input) {
        if (closed) {
            throw new IllegalStateException("Prototype viewport is closed");
        }
        camera.setAspectRatio((float) width / height);
        apply(input, elapsedSeconds);
        renderer.render(scene, camera);
        frameCount++;
    }

    /** Returns the number of completed JScene3D frames. */
    long frameCount() {
        return frameCount;
    }

    /** Releases the renderer before its externally owned OpenGLFX context is destroyed. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        renderer.close();
        cyanMaterial.close();
        greenMaterial.close();
        orangeMaterial.close();
        geometry.close();
        closed = true;
    }

    /** Builds three differently positioned boxes so orientation and Y-flipping remain visible. */
    private Group createModel() {
        Group group = new Group();
        Mesh left = new Mesh(geometry, greenMaterial);
        left.setPosition(-1.35f, -0.55f, 0.0f);
        left.setScale(1.2f, 0.45f, 1.2f);
        group.add(left);
        Mesh center = new Mesh(geometry, cyanMaterial);
        center.setPosition(0.0f, 0.25f, 0.0f);
        center.setScale(0.8f, 2.0f, 0.8f);
        group.add(center);
        Mesh right = new Mesh(geometry, orangeMaterial);
        right.setPosition(1.35f, -0.2f, 0.0f);
        right.setScale(0.65f, 1.15f, 0.65f);
        group.add(right);
        return group;
    }

    /** Applies one immutable interaction snapshot to the prototype model. */
    private void apply(PrototypeInteraction.FrameInput input, float elapsedSeconds) {
        if (input.resetRequested()) {
            model.setRotationFromEuler(0.0f, 0.0f, 0.0f, RotationOrder.XYZ);
        }
        model.rotateY(input.horizontalDrag() * DRAG_SCALE);
        model.rotateX(input.verticalDrag() * DRAG_SCALE);
        if (input.spinning()) {
            model.rotateY(elapsedSeconds * input.rotationSpeed());
        }
    }
}
