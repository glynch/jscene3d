/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.lwjgl.opengl.GL11.glFlush;

import io.github.glynch.jscene3d.editor.renderer.process.RendererConfiguration.ProjectLaunch;
import io.github.glynch.jscene3d.game.WorldFrameDriver;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.game.presentation.PresentationWorldModule;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceBridge;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceDescriptor;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceRenderSurface;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.desktop.StandardProjectEnvironment;
import io.github.glynch.jscene3d.project.runtime.HostedProject;
import io.github.glynch.jscene3d.project.runtime.ProjectLaunchRequest;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeEnvironment;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeHost;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.render.Renderer;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/** Isolated live runtime for one Java-authoritative project world. */
final class ProjectRendererSession implements RendererSession {
    private final Window window;
    private final IOSurfaceRenderSurface surface;
    private final Renderer renderer;
    private final HostedProject project;
    private final Spatial3dWorldModule spatial;
    private final PresentationWorldModule presentation;
    private final WorldFrameDriver frames;
    private long previousFrameNanos;
    private int width;
    private int height;
    private boolean closed;

    private ProjectRendererSession(
            Window window,
            IOSurfaceRenderSurface surface,
            Renderer renderer,
            HostedProject project,
            int width,
            int height) {
        this.window = window;
        this.surface = surface;
        this.renderer = renderer;
        this.project = project;
        this.width = width;
        this.height = height;
        spatial = project.world().requireModule(Spatial3dWorldModule.class);
        presentation = project.world().requireModule(PresentationWorldModule.class);
        InputWorldModule input = project.world().requireModule(InputWorldModule.class);
        if (!(input instanceof ProjectInput projectInput)) {
            throw new IllegalStateException("standard project environment did not bind ProjectInput");
        }
        frames = new WorldFrameDriver(project.world(), projectInput);
        project.world().activate();
        previousFrameNanos = System.nanoTime();
    }

    static ProjectRendererSession create(RendererConfiguration configuration, ProjectLaunch launch) {
        long initialSurface = IOSurfaceBridge.lookup(configuration.electronBundleId());
        if (initialSurface == 0L) {
            throw new IllegalStateException("Unable to acquire the initial IOSurface");
        }

        Window window = null;
        Renderer renderer = null;
        HostedProject project = null;
        try {
            window = Window.create(64, 64, "JScene3D Editor project renderer");
            IOSurfaceDescriptor descriptor = new IOSurfaceDescriptor(
                    initialSurface, configuration.initialWidth(), configuration.initialHeight());
            IOSurfaceRenderSurface surface = new IOSurfaceRenderSurface(descriptor);
            initialSurface = 0L;
            renderer = Renderer.create(surface);
            project = loadProject(launch);
            return new ProjectRendererSession(
                    window, surface, renderer, project, configuration.initialWidth(), configuration.initialHeight());
        } catch (RuntimeException | LinkageError failure) {
            closeAfterFailure(project, renderer, window, failure);
            if (initialSurface != 0L) {
                IOSurfaceBridge.release(initialSurface);
            }
            throw failure;
        }
    }

    /** Loads and verifies the exact Java-authoritative project launch before native rendering begins. */
    static HostedProject loadProject(ProjectLaunch launch) {
        return loadProject(launch, ProjectRendererSession.class.getClassLoader());
    }

    /** Loads and verifies a project through the supplied isolated runtime-artifact class loader. */
    static HostedProject loadProject(ProjectLaunch launch, ClassLoader classLoader) {
        return loadProject(launch, classLoader, new StandardProjectEnvironment(launch.publishedContentRoot()));
    }

    /** Loads and verifies a project through explicit host collaborators for focused tests. */
    static HostedProject loadProject(
            ProjectLaunch launch, ClassLoader classLoader, ProjectRuntimeEnvironment environment) {
        ProjectRuntimeHost host = new ProjectRuntimeHost(launch.engineVersion(), classLoader, environment);
        HostedProject project = host.load(
                launch.projectRoot(), ProjectLaunchRequest.scene(AssetRef.<SceneDefinition>to(launch.sceneAssetId())));
        if (!project.project().identity().id().equals(launch.projectId())) {
            project.close();
            throw new IllegalStateException("Loaded project identity does not match the prepared launch");
        }
        if (!project.world().definition().id().equals(launch.sceneAssetId())) {
            project.close();
            throw new IllegalStateException("Loaded Scene does not match the prepared launch");
        }
        return project;
    }

    @Override
    public void renderFrame() {
        requireOpen();
        long currentFrameNanos = System.nanoTime();
        frames.advance(Duration.ofNanos(Math.max(0L, currentFrameNanos - previousFrameNanos)), ActionSnapshot.empty());
        previousFrameNanos = currentFrameNanos;
        if (spatial.isReadyToRender()) {
            spatial.render(renderer, (float) width / height);
        } else {
            renderer.clear();
        }
        presentation.renderOverlays(renderer);
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
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
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

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Renderer session is closed");
        }
    }

    private static void closeAfterFailure(
            @Nullable HostedProject project, @Nullable Renderer renderer, @Nullable Window window, Throwable failure) {
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
