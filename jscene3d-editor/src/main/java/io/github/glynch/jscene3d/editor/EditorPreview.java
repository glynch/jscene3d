/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.render.RenderSurface;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.RendererOptions;
import io.github.glynch.jscene3d.scenes.Scene;

/** Owns the temporary scene and renderer displayed by the editor foundation. */
final class EditorPreview implements AutoCloseable {
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

    /** Creates the renderer and an asymmetric scene that exposes orientation and Y-flipping. */
    EditorPreview(RenderSurface surface) {
        renderer = Renderer.create(surface, RendererOptions.defaults());
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

    /** Renders one frame after applying the latest dimensions and input. */
    void render(float elapsedSeconds, RenderSurfaceSize size, ViewportInteraction.FrameInput input) {
        if (closed) {
            throw new IllegalStateException("Editor preview is closed");
        }
        camera.setAspectRatio((float) size.framebufferWidth() / size.framebufferHeight());
        apply(input, elapsedSeconds);
        renderer.render(scene, camera);
        frameCount++;
    }

    /** Returns the number of completed JScene3D frames. */
    long frameCount() {
        return frameCount;
    }

    /** Releases renderer GPU resources before closing the shared resource descriptions. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            renderer.close();
        } finally {
            cyanMaterial.close();
            greenMaterial.close();
            orangeMaterial.close();
            geometry.close();
            closed = true;
        }
    }

    /** Builds three differently positioned boxes for the temporary preview. */
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

    /** Applies one immutable interaction snapshot to the preview model. */
    private void apply(ViewportInteraction.FrameInput input, float elapsedSeconds) {
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
