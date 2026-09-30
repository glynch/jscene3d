/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.lwjgl.opengl.GL11.glFlush;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceBridge;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceDescriptor;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceRenderSurface;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import org.jspecify.annotations.Nullable;

/** Product-owned deterministic scene used to validate the packaged renderer runtime. */
final class ValidationRendererSession implements RendererSession {
    private static final float ROTATION_PER_FRAME = 0.01f;
    private static final float DRAG_SENSITIVITY = 0.005f;

    private final SessionResources resources;
    private final Scene scene;
    private final Mesh mesh;
    private final PerspectiveCamera camera;
    private boolean closed;

    private ValidationRendererSession(SessionResources resources, Scene scene, Mesh mesh, PerspectiveCamera camera) {
        this.resources = resources;
        this.scene = scene;
        this.mesh = mesh;
        this.camera = camera;
    }

    static ValidationRendererSession create(RendererConfiguration configuration) {
        long initialSurface = IOSurfaceBridge.lookup(configuration.electronBundleId());
        if (initialSurface == 0L) {
            throw new IllegalStateException("Unable to acquire the initial IOSurface");
        }

        Window window = null;
        NormalMaterial material = null;
        BufferGeometry geometry = null;
        Renderer renderer = null;
        try {
            window = Window.create(64, 64, "JScene3D Editor renderer");
            IOSurfaceDescriptor descriptor = new IOSurfaceDescriptor(
                    initialSurface, configuration.initialWidth(), configuration.initialHeight());
            IOSurfaceRenderSurface surface = new IOSurfaceRenderSurface(descriptor);
            initialSurface = 0L;
            material = new NormalMaterial();
            geometry = BoxGeometry.create(2.0f, 2.0f, 2.0f);
            renderer = Renderer.create(surface);
            Scene scene = new Scene();
            scene.setBackground(Color.srgb(0x172033));
            Mesh mesh = new Mesh(geometry, material);
            mesh.rotateX(0.3f);
            mesh.rotateY(0.4f);
            scene.add(mesh);
            PerspectiveCamera camera = new PerspectiveCamera(
                    (float) Math.toRadians(60.0),
                    (float) configuration.initialWidth() / configuration.initialHeight(),
                    0.1f,
                    100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            return new ValidationRendererSession(
                    new SessionResources(window, material, geometry, renderer, surface), scene, mesh, camera);
        } catch (RuntimeException failure) {
            closeAfterFailure(renderer, geometry, material, window, failure);
            if (initialSurface != 0L) {
                IOSurfaceBridge.release(initialSurface);
            }
            throw failure;
        }
    }

    @Override
    public void renderFrame() {
        requireOpen();
        mesh.rotateY(ROTATION_PER_FRAME);
        resources.renderer().render(scene, camera);
        resources.surface().present();
        glFlush();
    }

    @Override
    public SurfaceSize receiveReplacementSurface() {
        requireOpen();
        IOSurfaceDescriptor replacement = IOSurfaceBridge.receiveSurface();
        if (replacement == null) {
            throw new IllegalStateException("Unable to receive replacement IOSurface");
        }
        resources.surface().replaceSurface(replacement);
        camera.setAspectRatio((float) replacement.width() / replacement.height());
        return new SurfaceSize(replacement.width(), replacement.height());
    }

    @Override
    public void applyValidationDrag(float horizontal, float vertical) {
        requireOpen();
        mesh.rotateY(horizontal * DRAG_SENSITIVITY);
        mesh.rotateX(vertical * DRAG_SENSITIVITY);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            resources.renderer().close();
        } finally {
            try {
                resources.geometry().close();
            } finally {
                try {
                    resources.material().close();
                } finally {
                    resources.window().close();
                }
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Renderer session is closed");
        }
    }

    private static void closeAfterFailure(
            @Nullable Renderer renderer,
            @Nullable BufferGeometry geometry,
            @Nullable NormalMaterial material,
            @Nullable Window window,
            RuntimeException failure) {
        closeAfterFailure(renderer, failure);
        closeAfterFailure(geometry, failure);
        closeAfterFailure(material, failure);
        closeAfterFailure(window, failure);
    }

    private static void closeAfterFailure(@Nullable AutoCloseable resource, RuntimeException failure) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private record SessionResources(
            Window window,
            NormalMaterial material,
            BufferGeometry geometry,
            Renderer renderer,
            IOSurfaceRenderSurface surface) {}
}
