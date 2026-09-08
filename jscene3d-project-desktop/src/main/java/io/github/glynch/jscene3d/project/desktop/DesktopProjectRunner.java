/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.WorldFrameDriver;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.GamepadState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.runtime.HostedProject;
import io.github.glynch.jscene3d.project.runtime.ProjectHost;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeHost;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.render.Renderer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Loads and runs one project in a native desktop window.
 *
 * <p>The runner owns the complete blocking loop, GLFW event polling, the standard gamepad slot,
 * semantic input acquisition, world timing, rendering, and cleanup. It creates no threads and must
 * be invoked on a platform-compatible window thread. On macOS the JVM must start with
 * {@code -XstartOnFirstThread}.
 */
public final class DesktopProjectRunner {
    private static final int PRIMARY_GAMEPAD_SLOT = 0;

    private final ProjectHost projectHost;

    /** Creates a runner using the standard project environment.
     *
     * @param engineVersion running semantic engine version
     * @param classLoader application extension and provider class loader
     * @param publishedImports read-only cache of published import generations
     */
    public DesktopProjectRunner(String engineVersion, ClassLoader classLoader, Path publishedImports) {
        projectHost = new ProjectRuntimeHost(
                Objects.requireNonNull(engineVersion, "engineVersion"),
                Objects.requireNonNull(classLoader, "classLoader"),
                new StandardProjectEnvironment(publishedImports));
    }

    /** Runs the manifest-selected startup world until its window requests closure.
     *
     * @param projectRoot project directory containing {@code project.json}
     */
    public void run(Path projectRoot) {
        HostedProject project = projectHost.load(Objects.requireNonNull(projectRoot, "projectRoot"));
        try {
            runLoaded(project);
        } finally {
            project.close();
        }
    }

    /** Owns native resources while one already composed project is active. */
    private static void runLoaded(HostedProject project) {
        String title = project.project().identity().name();
        try (Window window = Window.create(title);
                Renderer renderer = Renderer.create(window);
                GamepadState gamepad = new GamepadState(PRIMARY_GAMEPAD_SLOT)) {
            runLoop(project, window, renderer, gamepad);
        }
    }

    /** Activates and advances the project until the platform requests closure. */
    private static void runLoop(HostedProject project, Window window, Renderer renderer, GamepadState gamepad) {
        ProjectInput input = requireProjectInput(project);
        Spatial3dWorldModule spatial = project.world().requireModule(Spatial3dWorldModule.class);
        WorldFrameDriver frames = new WorldFrameDriver(project.world(), input);
        DesktopPointerCapture pointerCapture = new DesktopPointerCapture(input.usesRelativePointer());
        project.world().activate();
        window.show();
        long previousFrame = System.nanoTime();
        while (!window.shouldClose()) {
            Window.pollEvents();
            gamepad.poll();
            DesktopPointerCapture.Update pointer = pointerCapture.update(
                    window.isFocused(),
                    window.input().wasKeyPressed(Key.ESCAPE),
                    window.input().wasMouseButtonPressed(MouseButton.LEFT));
            applyPointerTransition(window, pointer);
            ActionSnapshot acquired = input.sample(window.input(), gamepad, pointer.inputCapture());
            long currentFrame = System.nanoTime();
            frames.advance(Duration.ofNanos(Math.max(0L, currentFrame - previousFrame)), acquired);
            previousFrame = currentFrame;
            render(spatial, renderer, window);
        }
    }

    /** Applies a pointer-ownership transition without assigning Escape application-close semantics. */
    private static void applyPointerTransition(Window window, DesktopPointerCapture.Update update) {
        if (update.capturePointer()) {
            window.setCursorMode(CursorMode.DISABLED);
            if (window.isRawMouseMotionSupported()) {
                window.setRawMouseMotionEnabled(true);
            }
        } else if (update.releasePointer()) {
            window.setCursorMode(CursorMode.NORMAL);
        }
    }

    /** Renders and presents one frame when the window has a drawable framebuffer. */
    private static void render(Spatial3dWorldModule spatial, Renderer renderer, Window window) {
        if (window.framebufferWidth() <= 0 || window.framebufferHeight() <= 0) {
            return;
        }
        if (spatial.isReadyToRender()) {
            spatial.render(renderer, window.framebufferAspectRatio());
        } else {
            renderer.clear();
        }
        window.swapBuffers();
    }

    /** Requires the standard environment's host-writable input implementation. */
    private static ProjectInput requireProjectInput(HostedProject project) {
        InputWorldModule input = project.world().requireModule(InputWorldModule.class);
        if (!(input instanceof ProjectInput projectInput)) {
            throw new IllegalStateException("standard desktop environment did not bind ProjectInput");
        }
        return projectInput;
    }
}
