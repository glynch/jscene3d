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
import io.github.glynch.jscene3d.project.runtime.HostedProject;
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

    private final ProjectRuntimeHost projectHost;
    private final DesktopApplicationState applicationState;

    /** Creates a runner using the standard project environment.
     *
     * @param engineVersion running semantic engine version
     * @param classLoader application extension and provider class loader
     * @param publishedImports read-only cache of published import generations
     */
    public DesktopProjectRunner(String engineVersion, ClassLoader classLoader, Path publishedImports) {
        applicationState = new DesktopApplicationState();
        projectHost = new ProjectRuntimeHost(
                Objects.requireNonNull(engineVersion, "engineVersion"),
                Objects.requireNonNull(classLoader, "classLoader"),
                new StandardProjectEnvironment(publishedImports, applicationState::createControl));
    }

    /** Runs the manifest-selected startup world until its window requests closure.
     *
     * @param projectRoot project directory containing {@code project.json}
     */
    public void run(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot");
        try (DesktopProjectSession session = new DesktopProjectSession(projectHost, applicationState, root)) {
            session.start();
            runLoaded(session);
        }
    }

    /** Owns native resources while one already composed project is active. */
    private static void runLoaded(DesktopProjectSession session) {
        String title = session.current().project.project().identity().name();
        try (Window window = Window.create(title);
                Renderer renderer = Renderer.create(window);
                GamepadState gamepad = new GamepadState(PRIMARY_GAMEPAD_SLOT)) {
            runLoop(session, window, renderer, gamepad);
        }
    }

    /** Activates and advances the project until the platform requests closure. */
    private static void runLoop(DesktopProjectSession session, Window window, Renderer renderer, GamepadState gamepad) {
        window.show();
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
            if (!session.applyPendingCommand()) {
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
        window.swapBuffers();
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

        private @Nullable RunningWorld current;
        private @Nullable RunningWorld menu;
        private @Nullable RunningWorld gameplay;
        private boolean startupMenu;

        private DesktopProjectSession(ProjectRuntimeHost host, DesktopApplicationState application, Path projectRoot) {
            this.host = Objects.requireNonNull(host, "host");
            this.application = Objects.requireNonNull(application, "application");
            this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        }

        /** Loads and activates the manifest-selected startup world. */
        private void start() {
            application.setResumeAvailable(false);
            HostedProject loaded = host.load(projectRoot);
            startupMenu = loaded.project().runtime().startupScene().isPresent();
            RunningWorld started = new RunningWorld(loaded, !startupMenu);
            if (startupMenu) {
                menu = started;
            } else {
                gameplay = started;
            }
            current = started;
        }

        /** Returns the currently presented world after startup. */
        private RunningWorld current() {
            if (current == null) {
                throw new IllegalStateException("desktop project session has not started");
            }
            return current;
        }

        /** Applies at most one world-requested transition between completed host frames. */
        private boolean applyPendingCommand() {
            return application.takeRequest().map(this::apply).orElse(true);
        }

        /** Applies one host-owned application transition. */
        private boolean apply(ApplicationCommand command) {
            return switch (command) {
                case SHOW_MENU -> showMenu();
                case NEW_GAME -> newGame();
                case RESUME -> resume();
                case QUIT -> false;
            };
        }

        /** Opens a fresh menu world while retaining and no longer advancing gameplay. */
        private boolean showMenu() {
            if (!startupMenu || gameplay == null || current != gameplay) {
                return true;
            }
            application.setResumeAvailable(true);
            menu = new RunningWorld(host.load(projectRoot), false);
            current = menu;
            return true;
        }

        /** Replaces all previous gameplay state with a fresh entry world. */
        private boolean newGame() {
            closeMenu();
            closeGameplay();
            application.setResumeAvailable(false);
            gameplay = new RunningWorld(host.loadEntry(projectRoot), true);
            current = gameplay;
            return true;
        }

        /** Closes the menu and returns to the retained gameplay world when available. */
        private boolean resume() {
            if (gameplay == null || current != menu) {
                return true;
            }
            closeMenu();
            application.setResumeAvailable(false);
            current = gameplay;
            return true;
        }

        @Override
        public void close() {
            current = null;
            closeMenu();
            closeGameplay();
        }

        /** Closes and forgets the current menu world. */
        private void closeMenu() {
            if (menu != null) {
                menu.close();
                menu = null;
            }
        }

        /** Closes and forgets the current gameplay world. */
        private void closeGameplay() {
            if (gameplay != null) {
                gameplay.close();
                gameplay = null;
            }
        }
    }
}
