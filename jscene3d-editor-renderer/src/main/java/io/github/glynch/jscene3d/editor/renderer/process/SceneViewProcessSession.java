/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.lwjgl.opengl.GL11.glFlush;

import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.renderer.process.RendererConfiguration.SceneViewLaunch;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceBridge;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceDescriptor;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceRenderSurface;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.render.Renderer;
import org.jspecify.annotations.Nullable;

/** Isolated runtime-free renderer for Java-projected authored Scene state. */
final class SceneViewProcessSession implements RendererSession {
    private final Window window;
    private final IOSurfaceRenderSurface surface;
    private final Renderer renderer;
    private final EditorProjectSession project;
    private final SceneViewRendererSession sceneView;
    private final AssetId scene;
    private int width;
    private int height;
    private boolean closed;

    private SceneViewProcessSession(
            Window window,
            IOSurfaceRenderSurface surface,
            Renderer renderer,
            EditorProjectSession project,
            SceneViewRendererSession sceneView,
            AssetId scene,
            SurfaceSize initialSize) {
        this.window = window;
        this.surface = surface;
        this.renderer = renderer;
        this.project = project;
        this.sceneView = sceneView;
        this.scene = scene;
        this.width = initialSize.width();
        this.height = initialSize.height();
    }

    static SceneViewProcessSession create(RendererConfiguration configuration, SceneViewLaunch launch) {
        long initialSurface = IOSurfaceBridge.lookup(configuration.electronBundleId());
        if (initialSurface == 0L) {
            throw new IllegalStateException("Unable to acquire the initial IOSurface");
        }

        Window window = null;
        Renderer renderer = null;
        EditorProjectSession project = null;
        SceneViewRendererSession sceneView = null;
        try {
            window = Window.create(64, 64, "JScene3D Editor Scene View renderer");
            IOSurfaceDescriptor descriptor = new IOSurfaceDescriptor(
                    initialSurface, configuration.initialWidth(), configuration.initialHeight());
            IOSurfaceRenderSurface surface = new IOSurfaceRenderSurface(descriptor);
            initialSurface = 0L;
            renderer = Renderer.create(surface);
            project = loadProject(launch);
            sceneView = new SceneViewRendererSession(ProductSceneViewResourceResolver.create(
                    project.project(), project.types(), launch.publishedContentRoot()));
            return new SceneViewProcessSession(
                    window,
                    surface,
                    renderer,
                    project,
                    sceneView,
                    launch.sceneAssetId(),
                    new SurfaceSize(configuration.initialWidth(), configuration.initialHeight()));
        } catch (RuntimeException | LinkageError failure) {
            closeAfterFailure(sceneView, project, renderer, window, failure);
            if (initialSurface != 0L) {
                IOSurfaceBridge.release(initialSurface);
            }
            throw failure;
        }
    }

    /** Loads the fixed editor-authoring model and verifies the prepared project identity. */
    static EditorProjectSession loadProject(SceneViewLaunch launch) {
        EditorProjectLoadResult result = new EditorProjectLoader(
                        launch.engineVersion(), SceneViewProcessSession.class.getClassLoader())
                .load(launch.projectRoot());
        EditorProjectSession session =
                result.session().orElseThrow(() -> new IllegalStateException("Safe Scene View project loading failed"));
        if (!session.project().identity().id().equals(launch.projectId())) {
            session.close();
            throw new IllegalStateException("Loaded project identity does not match the prepared Scene View launch");
        }
        return session;
    }

    @Override
    public void renderFrame() {
        requireOpen();
        if (sceneView.revision().isPresent()) {
            sceneView.render(renderer, (float) width / height);
        } else {
            renderer.clear();
        }
        surface.present();
        glFlush();
    }

    @Override
    public SurfaceSize receiveReplacementSurface() {
        requireOpen();
        IOSurfaceDescriptor replacement = IOSurfaceBridge.receiveSurface();
        if (replacement == null) {
            throw new IllegalStateException("Unable to receive replacement IOSurface");
        }
        surface.replaceSurface(replacement);
        width = replacement.width();
        height = replacement.height();
        return new SurfaceSize(width, height);
    }

    @Override
    public void applyValidationDrag(float horizontal, float vertical) {
        requireOpen();
    }

    @Override
    public void replaceSceneViewSnapshot(SceneViewSnapshot snapshot) {
        requireOpen();
        if (!snapshot.scene().equals(scene)) {
            throw new IllegalArgumentException("Scene View snapshot does not match the prepared Scene identity");
        }
        SceneViewRealizationResult result = sceneView.replaceSnapshot(snapshot);
        if (result.status() == SceneViewRealizationResult.Status.FAILED) {
            throw new IllegalStateException("Scene View snapshot realization failed: " + result.diagnostics());
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            sceneView.close();
        } finally {
            try {
                project.close();
            } finally {
                try {
                    renderer.close();
                } finally {
                    window.close();
                }
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene View renderer session is closed");
        }
    }

    private static void closeAfterFailure(
            @Nullable SceneViewRendererSession sceneView,
            @Nullable EditorProjectSession project,
            @Nullable Renderer renderer,
            @Nullable Window window,
            Throwable failure) {
        closeAfterFailure(sceneView, failure);
        closeAfterFailure(project, failure);
        closeAfterFailure(renderer, failure);
        closeAfterFailure(window, failure);
    }

    private static void closeAfterFailure(@Nullable AutoCloseable resource, Throwable failure) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
