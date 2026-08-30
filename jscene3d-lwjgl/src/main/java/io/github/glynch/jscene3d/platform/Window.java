/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.lwjgl.glfw.GLFW.GLFW_ALPHA_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_BLUE_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_DOUBLEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_GREEN_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RED_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_SRGB_CAPABLE;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Owns a native window and its unshared OpenGL context.
 *
 * <p>A window is initially hidden. Native operations and closure must run on the thread that
 * created the window. Closure is terminal and idempotent.
 */
public final class Window implements AutoCloseable {
    private static final List<Window> OPEN_WINDOWS = new ArrayList<>();

    private final long handle;
    private final GLCapabilities capabilities;
    private final InputState input = new InputState();

    private int width;
    private int height;
    private String title;
    private int framebufferWidth;
    private int framebufferHeight;
    private final int framebufferSampleCount;
    private VerticalSync verticalSync;

    private boolean visible;
    private boolean framebufferSizeChanged;
    private boolean closed;

    private Window(long handle, GLCapabilities capabilities, WindowOptions options) {
        this.handle = handle;
        this.capabilities = capabilities;
        title = options.title();
        verticalSync = options.verticalSync();

        int[] logicalWidth = new int[1];
        int[] logicalHeight = new int[1];
        GLFW.glfwGetWindowSize(handle, logicalWidth, logicalHeight);
        width = logicalWidth[0];
        height = logicalHeight[0];

        int[] pixelWidth = new int[1];
        int[] pixelHeight = new int[1];
        GLFW.glfwGetFramebufferSize(handle, pixelWidth, pixelHeight);
        framebufferWidth = pixelWidth[0];
        framebufferHeight = pixelHeight[0];
        framebufferSampleCount = GL11.glGetInteger(GL13.GL_SAMPLES);
    }

    /**
     * Creates an initially hidden window and OpenGL 3.3 Core context.
     *
     * @param options immutable creation options
     * @return the created window
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IllegalStateException if GLFW or the requested OpenGL context cannot be initialized
     */
    public static Window create(WindowOptions options) {
        Objects.requireNonNull(options, "options");
        GlfwRuntime.acquire();

        long handle = NULL;
        try {
            GlfwRuntime.clearLastError();
            configureWindowHints(options);
            handle = GLFW.glfwCreateWindow(options.width(), options.height(), options.title(), NULL, NULL);
            if (handle == NULL) {
                throw new IllegalStateException(GlfwRuntime.failureMessage("Could not create window"));
            }

            GLFW.glfwMakeContextCurrent(handle);
            GLCapabilities capabilities = GL.createCapabilities();
            if (!capabilities.OpenGL33) {
                throw new IllegalStateException("The created context does not support OpenGL 3.3");
            }
            GLFW.glfwSwapInterval(toSwapInterval(options.verticalSync()));

            Window window = new Window(handle, capabilities, options);
            window.installCallbacks();
            OPEN_WINDOWS.add(window);
            return window;
        } catch (RuntimeException exception) {
            if (handle != NULL) {
                releaseNativeWindow(handle);
            }
            GlfwRuntime.release();
            throw exception;
        }
    }

    /**
     * Creates an initially hidden window using the default vertical-synchronization and
     * multisampling options.
     *
     * @param width the positive logical width in screen coordinates
     * @param height the positive logical height in screen coordinates
     * @param title the title, which may be empty
     * @return the created window
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if a dimension is not positive or the title contains a
     *     null character
     * @throws IllegalStateException if GLFW or the requested OpenGL context cannot be initialized
     */
    public static Window create(int width, int height, String title) {
        return create(WindowOptions.builder().size(width, height).title(title).build());
    }

    /**
     * Processes pending platform events for every open JScene3D window.
     *
     * <p>Transient input and window-change state is reset before native callbacks dispatch the
     * latest events. This method does not allocate during steady-state polling.
     *
     * @throws IllegalStateException if there are no open windows or it is called from the wrong
     *     thread
     */
    public static void pollEvents() {
        GlfwRuntime.requireActiveOwnerThread();
        for (int index = 0; index < OPEN_WINDOWS.size(); index++) {
            OPEN_WINDOWS.get(index).beginPoll();
        }
        GLFW.glfwPollEvents();
    }

    /**
     * Returns whether the window is currently visible.
     *
     * @return {@code true} after {@link #show()} and before a subsequent hide
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public boolean isVisible() {
        requireOpenOwnerThread();
        return visible;
    }

    /**
     * Makes the native window visible without changing its context lifetime.
     *
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void show() {
        requireOpenOwnerThread();
        GLFW.glfwShowWindow(handle);
        visible = true;
    }

    /**
     * Hides the native window without changing its context lifetime.
     *
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void hide() {
        requireOpenOwnerThread();
        GLFW.glfwHideWindow(handle);
        visible = false;
    }

    /**
     * Returns whether terminal closure has completed.
     *
     * @return {@code true} after the first successful call to {@link #close()}
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns the current title.
     *
     * @return the title, which may be empty
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public String title() {
        requireOpenOwnerThread();
        return title;
    }

    /**
     * Changes the title immediately.
     *
     * @param title the new title, which may be empty
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} contains a null character
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void setTitle(String title) {
        requireOpenOwnerThread();
        String validTitle = WindowOptions.requireValidTitle(title);
        GLFW.glfwSetWindowTitle(handle, validTitle);
        this.title = validTitle;
    }

    /**
     * Returns the current logical width.
     *
     * @return the width in screen coordinates
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public int width() {
        requireOpenOwnerThread();
        return width;
    }

    /**
     * Returns the current logical height.
     *
     * @return the height in screen coordinates
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public int height() {
        requireOpenOwnerThread();
        return height;
    }

    /**
     * Returns the current framebuffer width.
     *
     * @return the width in framebuffer pixels
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public int framebufferWidth() {
        requireOpenOwnerThread();
        return framebufferWidth;
    }

    /**
     * Returns the current framebuffer height.
     *
     * @return the height in framebuffer pixels
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public int framebufferHeight() {
        requireOpenOwnerThread();
        return framebufferHeight;
    }

    /**
     * Returns the current framebuffer width divided by its height.
     *
     * @return the current framebuffer aspect ratio
     * @throws IllegalStateException if the window is closed, called from the wrong thread, or the
     *     framebuffer has no drawable area
     */
    public float framebufferAspectRatio() {
        requireOpenOwnerThread();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IllegalStateException("Framebuffer has no drawable area");
        }
        return (float) framebufferWidth / (float) framebufferHeight;
    }

    /**
     * Returns whether the framebuffer dimensions changed during the latest event poll.
     *
     * <p>Reading this flag does not consume it.
     *
     * @return {@code true} when the latest poll dispatched a framebuffer-size change
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public boolean framebufferSizeChanged() {
        requireOpenOwnerThread();
        return framebufferSizeChanged;
    }

    /**
     * Returns the actual default-framebuffer sample count.
     *
     * @return zero when multisampling is disabled, otherwise the actual positive sample count
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public int framebufferSampleCount() {
        requireOpenOwnerThread();
        return framebufferSampleCount;
    }

    /**
     * Returns this window's stable, read-only input view.
     *
     * @return the same input view for this window's lifetime
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public InputState input() {
        requireOpenOwnerThread();
        return input;
    }

    /**
     * Returns the current vertical-synchronization mode.
     *
     * @return the current mode
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public VerticalSync verticalSync() {
        requireOpenOwnerThread();
        return verticalSync;
    }

    /**
     * Changes whether subsequent buffer swaps synchronize with the display refresh cycle.
     *
     * @param verticalSync the new mode
     * @throws NullPointerException if {@code verticalSync} is {@code null}
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void setVerticalSync(VerticalSync verticalSync) {
        requireOpenOwnerThread();
        VerticalSync validVerticalSync = Objects.requireNonNull(verticalSync, "verticalSync");
        makeContextCurrent();
        GLFW.glfwSwapInterval(toSwapInterval(validVerticalSync));
        this.verticalSync = validVerticalSync;
    }

    /**
     * Returns whether the application or platform has requested that this window close.
     *
     * <p>A close request does not destroy the window; the application remains responsible for
     * calling {@link #close()}.
     *
     * @return {@code true} when closure has been requested
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public boolean shouldClose() {
        requireOpenOwnerThread();
        return GLFW.glfwWindowShouldClose(handle);
    }

    /**
     * Requests orderly closure without immediately destroying this window.
     *
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void requestClose() {
        requireOpenOwnerThread();
        GLFW.glfwSetWindowShouldClose(handle, true);
    }

    /**
     * Publishes the completed default-framebuffer back buffer.
     *
     * <p>This method makes this window's context current when another JScene3D window was used
     * most recently. Rendering and buffer swapping remain separate operations.
     *
     * @throws IllegalStateException if the window is closed or called from the wrong thread
     */
    public void swapBuffers() {
        requireOpenOwnerThread();
        makeContextCurrent();
        GLFW.glfwSwapBuffers(handle);
    }

    /**
     * Permanently destroys this window and its context.
     *
     * <p>Repeated calls do nothing. The first call must run on the creating thread.
     *
     * @throws IllegalStateException if the first call runs on the wrong thread
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        GlfwRuntime.requireActiveOwnerThread();
        OPEN_WINDOWS.remove(this);
        releaseNativeWindow(handle);
        closed = true;
        GlfwRuntime.release();
    }

    private static void configureWindowHints(WindowOptions options) {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_RED_BITS, 8);
        GLFW.glfwWindowHint(GLFW_GREEN_BITS, 8);
        GLFW.glfwWindowHint(GLFW_BLUE_BITS, 8);
        GLFW.glfwWindowHint(GLFW_ALPHA_BITS, 8);
        GLFW.glfwWindowHint(GLFW_DEPTH_BITS, 24);
        GLFW.glfwWindowHint(GLFW_STENCIL_BITS, 8);
        GLFW.glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_SAMPLES, options.preferredFramebufferSampleCount());
    }

    private static int toSwapInterval(VerticalSync verticalSync) {
        return verticalSync == VerticalSync.ENABLED ? 1 : 0;
    }

    private static void releaseNativeWindow(long handle) {
        Callbacks.glfwFreeCallbacks(handle);
        if (GLFW.glfwGetCurrentContext() == handle) {
            GLFW.glfwMakeContextCurrent(NULL);
            GL.setCapabilities(null);
        }
        GLFW.glfwDestroyWindow(handle);
    }

    private void installCallbacks() {
        double[] pointerX = new double[1];
        double[] pointerY = new double[1];
        GLFW.glfwGetCursorPos(handle, pointerX, pointerY);
        input.initializePointer(pointerX[0], pointerY[0]);

        GLFW.glfwSetWindowSizeCallback(handle, (ignored, newWidth, newHeight) -> {
            width = newWidth;
            height = newHeight;
        });
        GLFW.glfwSetFramebufferSizeCallback(handle, (ignored, newWidth, newHeight) -> {
            framebufferWidth = newWidth;
            framebufferHeight = newHeight;
            framebufferSizeChanged = true;
        });
        GLFW.glfwSetKeyCallback(
                handle,
                (ignored, keyCode, scanCode, action, modifiers) ->
                        input.updateKey(Key.fromPlatformCode(keyCode), action));
        GLFW.glfwSetMouseButtonCallback(
                handle,
                (ignored, buttonCode, action, modifiers) ->
                        input.updateMouseButton(MouseButton.fromPlatformCode(buttonCode), action));
        GLFW.glfwSetCursorPosCallback(handle, (ignored, x, y) -> input.updatePointer(x, y));
        GLFW.glfwSetScrollCallback(handle, (ignored, xOffset, yOffset) -> input.updateScroll(xOffset, yOffset));
        GLFW.glfwSetWindowFocusCallback(handle, (ignored, focused) -> {
            if (!focused) {
                input.releaseHeldButtons();
            }
        });
    }

    private void beginPoll() {
        framebufferSizeChanged = false;
        input.beginPoll();
    }

    private void requireOpenOwnerThread() {
        if (closed) {
            throw new IllegalStateException("Window is closed");
        }
        GlfwRuntime.requireActiveOwnerThread();
    }

    private void makeContextCurrent() {
        if (GLFW.glfwGetCurrentContext() != handle) {
            GLFW.glfwMakeContextCurrent(handle);
        }
        GL.setCapabilities(capabilities);
    }
}
