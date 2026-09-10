/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.WorldFrameDriver;
import io.github.glynch.jscene3d.game.application.ApplicationCommand;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.PointerSnapshot;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.game.presentation.PresentationWorldModule;
import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.GamepadState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.HostedProject;
import io.github.glynch.jscene3d.project.runtime.ProjectHostException;
import io.github.glynch.jscene3d.project.runtime.ProjectLaunchRequest;
import io.github.glynch.jscene3d.project.runtime.ProjectLoadProgressReporter;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeHost;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.render.Renderer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Loads and runs one project in a native desktop window.
 *
 * <p>The runner owns the complete blocking loop, GLFW event polling, the standard gamepad slot,
 * semantic input acquisition, world timing, rendering, and cleanup. It creates no threads and must
 * be invoked on a platform-compatible window thread. On macOS the JVM must start with
 * {@code -XstartOnFirstThread}.
 */
public final class DesktopProjectRunner {
    private static final int PRIMARY_GAMEPAD_SLOT = 0;
    private static final InitialSplashPolicy LAUNCH_POLICY = new InitialSplashPolicy();

    private final ProjectRuntimeHost projectHost;
    private final DesktopApplicationState applicationState;
    private final String engineVersion;

    /** Creates a runner using the standard project environment.
     *
     * @param engineVersion running semantic engine version
     * @param classLoader application extension and provider class loader
     * @param publishedImports read-only cache of published import generations
     */
    public DesktopProjectRunner(String engineVersion, ClassLoader classLoader, Path publishedImports) {
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
        applicationState = new DesktopApplicationState();
        projectHost = new ProjectRuntimeHost(
                this.engineVersion,
                Objects.requireNonNull(classLoader, "classLoader"),
                new StandardProjectEnvironment(publishedImports, applicationState::createControl));
    }

    /** Runs the manifest-selected startup world until its window requests closure.
     *
     * @param projectRoot project directory containing {@code project.json}
     */
    public void run(Path projectRoot) {
        run(projectRoot, ProjectLaunchRequest.standard());
    }

    /** Runs one explicitly selected scene and parameter set until its window requests closure.
     *
     * @param projectRoot project directory containing {@code project.json}
     * @param request immutable launch request, including an optional playtest profile
     */
    public void run(Path projectRoot, ProjectLaunchRequest request) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot");
        ProjectLaunchRequest validRequest = Objects.requireNonNull(request, "request");
        GameProject project;
        try {
            project = loadProject(root);
        } catch (RuntimeException failure) {
            runStartupFailure(projectName(root), failure);
            return;
        }
        try (DesktopProjectSession session =
                new DesktopProjectSession(projectHost, applicationState, root, validRequest)) {
            runLoaded(session, project, validRequest);
        }
    }

    /** Owns native resources while startup and one composed project are active. */
    private static void runLoaded(DesktopProjectSession session, GameProject project, ProjectLaunchRequest request) {
        String title = windowTitle(project.identity().name(), request);
        try (Window window = Window.create(title);
                Renderer renderer = Renderer.create(window);
                GamepadState gamepad = new GamepadState(PRIMARY_GAMEPAD_SLOT)) {
            DesktopSplashScreen splash;
            try {
                splash = DesktopSplashScreen.load(project);
            } catch (RuntimeException failure) {
                new DesktopStartupErrorScreen(title, failure).run(renderer, window);
                return;
            }
            boolean presentInitialSplash = !request.isPlaytest()
                    && project.launch().splash().isPresent()
                    && LAUNCH_POLICY.claimInitialSplash();
            long splashShownAt = System.nanoTime();
            window.show();
            try {
                session.start(
                        presentInitialSplash
                                ? phase -> splash.present(phase, renderer, window)
                                : ignored -> Window.pollEvents());
            } catch (RuntimeException failure) {
                new DesktopStartupErrorScreen(title, failure).run(renderer, window);
                return;
            }
            if (presentInitialSplash) {
                awaitMinimumSplashDuration(splash, splashShownAt, renderer, window);
            }
            if (window.shouldClose()) {
                return;
            }
            window.setTitle(title);
            runLoop(session, window, renderer, gamepad);
        }
    }

    /** Adds an unmistakable development-only suffix to named playtest windows. */
    static String windowTitle(String projectName, ProjectLaunchRequest request) {
        return request.profile()
                .map(profile -> projectName + " [PLAYTEST: " + profile + "]")
                .orElse(projectName);
    }

    /** Keeps one startup failure visible even when no validated project could be constructed. */
    private static void runStartupFailure(String projectName, RuntimeException failure) {
        try (Window window = Window.create(projectName);
                Renderer renderer = Renderer.create(window)) {
            new DesktopStartupErrorScreen(projectName, failure).run(renderer, window);
        }
    }

    /** Derives a stable fallback title when manifest validation fails. */
    private static String projectName(Path projectRoot) {
        Path fileName = projectRoot.toAbsolutePath().normalize().getFileName();
        return fileName == null ? "JScene3D project" : fileName.toString();
    }

    /** Keeps polling and presenting a ready splash until its authored minimum duration elapses. */
    private static void awaitMinimumSplashDuration(
            DesktopSplashScreen splash, long shownAt, Renderer renderer, Window window) {
        while (!window.shouldClose()
                && !InitialSplashPolicy.minimumElapsed(shownAt, System.nanoTime(), splash.minimumDuration())) {
            splash.present(ProjectLoadProgressReporter.Phase.READY, renderer, window);
        }
    }

    /** Reads launch presentation before opening the native application shell. */
    private GameProject loadProject(Path projectRoot) {
        ProjectLoadResult result = new ProjectLoader(engineVersion).load(projectRoot);
        return result.project()
                .orElseThrow(() -> new ProjectHostException("project manifest loading failed", result.diagnostics()));
    }

    /** Activates and advances the project until the platform requests closure. */
    private static void runLoop(DesktopProjectSession session, Window window, Renderer renderer, GamepadState gamepad) {
        DesktopLoadingIndicator loading = new DesktopLoadingIndicator(window.title());
        long previousFrame = System.nanoTime();
        while (!window.shouldClose()) {
            RunningWorld current = session.current();
            Window.pollEvents();
            gamepad.poll();
            DesktopPointerCapture.Update pointer = current.pointerCapture.update(
                    window.isFocused(),
                    window.input().wasKeyPressed(Key.ESCAPE),
                    window.input().wasMouseButtonPressed(MouseButton.LEFT));
            applyPointerTransition(window, pointer);
            ActionSnapshot acquired =
                    current.input.sample(window.input(), gamepad, pointer.inputCapture(), pointerSnapshot(window));
            long currentFrame = System.nanoTime();
            current.frames.advance(Duration.ofNanos(Math.max(0L, currentFrame - previousFrame)), acquired);
            previousFrame = currentFrame;
            boolean continueRunning = session.applyPendingCommand(phase -> {
                renderContents(session.current().spatial, session.current().presentation, renderer, window);
                loading.present(phase, renderer, window);
            });
            loading.finish(window);
            if (!continueRunning) {
                break;
            }
            if (current != session.current()) {
                current.pointerCapture.reset();
                releasePointer(window);
                previousFrame = System.nanoTime();
            }
            render(session.current().spatial, session.current().presentation, renderer, window);
        }
    }

    /** Captures logical pointer position and primary-button transitions for screen-space interfaces. */
    private static PointerSnapshot pointerSnapshot(Window window) {
        return new PointerSnapshot(
                window.input().pointerX(),
                window.input().pointerY(),
                window.width(),
                window.height(),
                window.input().isMouseButtonDown(MouseButton.LEFT),
                window.input().wasMouseButtonPressed(MouseButton.LEFT),
                window.input().wasMouseButtonReleased(MouseButton.LEFT));
    }

    /** Returns native pointer ownership to the operating system after leaving gameplay. */
    private static void releasePointer(Window window) {
        window.setCursorMode(CursorMode.NORMAL);
        if (window.isRawMouseMotionSupported()) {
            window.setRawMouseMotionEnabled(false);
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
            releasePointer(window);
        }
    }

    /** Renders and presents one frame when the window has a drawable framebuffer. */
    private static void render(
            Spatial3dWorldModule spatial, PresentationWorldModule presentation, Renderer renderer, Window window) {
        renderContents(spatial, presentation, renderer, window);
        if (window.framebufferWidth() > 0 && window.framebufferHeight() > 0) {
            window.swapBuffers();
        }
    }

    /** Renders one frame without presenting it so a host overlay may be added first. */
    private static void renderContents(
            Spatial3dWorldModule spatial, PresentationWorldModule presentation, Renderer renderer, Window window) {
        if (window.framebufferWidth() <= 0 || window.framebufferHeight() <= 0) {
            return;
        }
        if (spatial.isReadyToRender()) {
            DesktopAudioListenerSynchronizer.synchronize(
                    spatial.activePrimaryCameraTransform(), presentation::setListenerTransform);
            spatial.render(renderer, window.framebufferAspectRatio());
        } else {
            renderer.clear();
        }
        presentation.renderOverlays(renderer);
    }

    /** Activated world and the host-side adapters required by one desktop frame. */
    private static final class RunningWorld implements AutoCloseable {
        private final HostedProject project;
        private final ProjectInput input;
        private final Spatial3dWorldModule spatial;
        private final PresentationWorldModule presentation;
        private final WorldFrameDriver frames;
        private final DesktopPointerCapture pointerCapture;

        private RunningWorld(HostedProject project, boolean gameplay) {
            this.project = Objects.requireNonNull(project, "project");
            input = requireProjectInput(project);
            spatial = project.world().requireModule(Spatial3dWorldModule.class);
            presentation = project.world().requireModule(PresentationWorldModule.class);
            frames = new WorldFrameDriver(project.world(), input);
            pointerCapture = new DesktopPointerCapture(gameplay && input.usesRelativePointer());
            project.world().activate();
        }

        @Override
        public void close() {
            project.close();
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

    /** Owns menu/gameplay world replacement while preserving a paused world for Resume. */
    private static final class DesktopProjectSession implements AutoCloseable {
        private final ProjectRuntimeHost host;
        private final DesktopApplicationState application;
        private final Path projectRoot;
        private final ProjectLaunchRequest launchRequest;

        private @Nullable DesktopSessionLifecycle<RunningWorld> lifecycle;

        private DesktopProjectSession(
                ProjectRuntimeHost host,
                DesktopApplicationState application,
                Path projectRoot,
                ProjectLaunchRequest launchRequest) {
            this.host = Objects.requireNonNull(host, "host");
            this.application = Objects.requireNonNull(application, "application");
            this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
            this.launchRequest = Objects.requireNonNull(launchRequest, "launchRequest");
        }

        /** Loads and activates the manifest-selected startup world. */
        private void start(ProjectLoadProgressReporter progress) {
            application.setResumeAvailable(false);
            HostedProject loaded = host.load(projectRoot, launchRequest, progress);
            boolean startupMenu = !launchRequest.isPlaytest()
                    && loaded.project().runtime().startupScene().isPresent();
            DesktopSessionLifecycle<RunningWorld> started = new DesktopSessionLifecycle<>(startupMenu);
            started.start(new RunningWorld(loaded, !startupMenu));
            lifecycle = started;
        }

        /** Returns the currently presented world after startup. */
        private RunningWorld current() {
            if (lifecycle == null) {
                throw new IllegalStateException("desktop project session has not started");
            }
            return lifecycle.current();
        }

        /** Applies at most one world-requested transition between completed host frames. */
        private boolean applyPendingCommand(ProjectLoadProgressReporter progress) {
            Objects.requireNonNull(progress, "progress");
            return application
                    .takeRequest()
                    .map(command -> apply(command, progress))
                    .orElse(true);
        }

        /** Applies one host-owned application transition. */
        private boolean apply(ApplicationCommand command, ProjectLoadProgressReporter progress) {
            DesktopSessionLifecycle<RunningWorld> active = requireLifecycle();
            boolean continueRunning = active.apply(
                    command,
                    () -> new RunningWorld(host.load(projectRoot), false),
                    () -> new RunningWorld(host.loadEntry(projectRoot, launchRequest, progress), true));
            application.setResumeAvailable(active.canResume());
            return continueRunning;
        }

        @Override
        public void close() {
            if (lifecycle != null) {
                lifecycle.close();
                lifecycle = null;
            }
        }

        /** Returns initialized lifecycle state for one transition. */
        private DesktopSessionLifecycle<RunningWorld> requireLifecycle() {
            if (lifecycle == null) {
                throw new IllegalStateException("desktop project session has not started");
            }
            return lifecycle;
        }
    }
}
