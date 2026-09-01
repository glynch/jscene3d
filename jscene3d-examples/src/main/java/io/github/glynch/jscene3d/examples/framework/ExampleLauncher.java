/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Objects;

/** Runs hosted examples as ordinary independent native applications. */
public final class ExampleLauncher {
    /** Prevents instantiation of this stateless launcher. */
    private ExampleLauncher() {
        throw new AssertionError("ExampleLauncher cannot be instantiated");
    }

    /**
     * Runs one example until the window is closed or Escape is pressed.
     *
     * @param title native window title
     * @param factory example factory
     */
    public static void launch(String title, ExampleFactory factory) {
        Objects.requireNonNull(title, "title");
        ExampleFactory validFactory = Objects.requireNonNull(factory, "factory");
        try (Window window = Window.create(title);
                Renderer renderer = Renderer.create(window)) {
            ExampleContext context = new ExampleContext(window, renderer);
            try (HostedExample example = validFactory.create(context)) {
                example.resize();
                window.show();
                long previousNanos = System.nanoTime();
                while (!window.shouldClose()) {
                    Window.pollEvents();
                    if (window.input().wasKeyPressed(Key.ESCAPE)) {
                        if (window.cursorMode() == CursorMode.DISABLED) {
                            window.setCursorMode(CursorMode.NORMAL);
                        } else {
                            window.requestClose();
                        }
                    }
                    if (window.framebufferSizeChanged()) {
                        context.refreshDimensions();
                        example.resize();
                    }
                    long nowNanos = System.nanoTime();
                    float elapsedSeconds = Math.max((nowNanos - previousNanos) / 1_000_000_000.0f, 0.0f);
                    previousNanos = nowNanos;
                    example.update(new ExampleFrame(elapsedSeconds, false));
                    context.applyRendererViewport();
                    example.render();
                    window.swapBuffers();
                }
            }
        }
    }
}
