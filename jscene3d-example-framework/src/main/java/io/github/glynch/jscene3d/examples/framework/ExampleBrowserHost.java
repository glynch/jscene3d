/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.gui.GalleryItem;
import io.github.glynch.jscene3d.gui.GalleryPanel;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.List;
import java.util.Objects;

/** Hosts any explicit example suite in the shared searchable native gallery. */
public final class ExampleBrowserHost {
    private static final float MAXIMUM_FRAME_SECONDS = 0.1F;

    /** Prevents instantiation of this stateless host. */
    private ExampleBrowserHost() {
        throw new AssertionError("ExampleBrowserHost cannot be instantiated");
    }

    /**
     * Opens the supplied suite and runs it until the native window closes.
     *
     * @param suite suite to host in the native gallery
     */
    public static void launch(ExampleSuite suite) {
        ExampleSuite validSuite = Objects.requireNonNull(suite, "suite");
        WindowOptions options = WindowOptions.builder()
                .size(1440, 900)
                .title(validSuite.windowTitle())
                .preferredFramebufferSampleCount(4)
                .build();
        List<ExampleDefinition> definitions = validSuite.definitions();
        List<GalleryItem> galleryItems = definitions.stream()
                .map(definition -> galleryItem(validSuite, definition))
                .toList();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window)) {
            ExampleContext context = new ExampleContext(window, renderer);
            context.setSidebarWidth(GalleryPanel.WIDTH);
            GalleryPanel gallery = new GalleryPanel(window, validSuite.brandName(), galleryItems);
            try (BrowserSession session = new BrowserSession(context, definitions, gallery)) {
                window.show();
                run(window, context, gallery, renderer, session);
            }
        }
    }

    /** Loads one required suite thumbnail and creates its GUI-facing immutable item. */
    private static GalleryItem galleryItem(ExampleSuite suite, ExampleDefinition definition) {
        return new GalleryItem(
                definition.id(),
                definition.title(),
                definition.category(),
                definition.description(),
                definition.tags(),
                OverlayImageLoader.loadResource(suite.resourceAnchor(), suite.thumbnailResource(definition)),
                definition.attributions());
    }

    /** Runs the event, example, and overlay loop until the native window closes. */
    private static void run(
            Window window, ExampleContext context, GalleryPanel gallery, Renderer renderer, BrowserSession session) {
        long previousNanos = System.nanoTime();
        while (!window.shouldClose()) {
            Window.pollEvents();
            handleEscape(window);
            if (window.framebufferSizeChanged()) {
                context.refreshDimensions();
                session.resize();
            }
            gallery.update();
            session.select(gallery.selectedItem().id());
            long nowNanos = System.nanoTime();
            float elapsedSeconds =
                    Math.clamp((nowNanos - previousNanos) / 1_000_000_000.0F, 0.0F, MAXIMUM_FRAME_SECONDS);
            previousNanos = nowNanos;
            boolean pointerCaptured = gallery.capturesPointer() || !context.containsPointer();
            boolean keyboardCaptured = gallery.capturesKeyboard();
            session.update(new ExampleFrame(elapsedSeconds, pointerCaptured, keyboardCaptured));
            context.applyRendererViewport();
            session.render();
            renderer.render(gallery);
            window.swapBuffers();
        }
    }

    /** Releases pointer capture before treating Escape as a request to close the browser. */
    private static void handleEscape(Window window) {
        if (!window.input().wasKeyPressed(Key.ESCAPE)) {
            return;
        }
        if (window.cursorMode() == CursorMode.DISABLED) {
            window.setCursorMode(CursorMode.NORMAL);
        } else {
            window.requestClose();
        }
    }

    /** Owns the currently selected live example and replaces it atomically on selection. */
    private static final class BrowserSession implements AutoCloseable {
        private final ExampleContext context;
        private final List<ExampleDefinition> definitions;

        private String selectedId;
        private HostedExample example;

        /** Creates and sizes the first suite example. */
        private BrowserSession(ExampleContext context, List<ExampleDefinition> definitions, GalleryPanel gallery) {
            this.context = context;
            this.definitions = definitions;
            ExampleDefinition initial = definition(gallery.selectedItem().id());
            selectedId = initial.id();
            example = create(initial);
        }

        /** Replaces the current example when its selected identifier changes. */
        private void select(String id) {
            if (selectedId.equals(id)) {
                return;
            }
            ExampleDefinition replacementDefinition = definition(id);
            HostedExample replacement = create(replacementDefinition);
            HostedExample previous = example;
            example = replacement;
            selectedId = id;
            previous.close();
        }

        /** Creates and sizes a replacement while guaranteeing cleanup if sizing fails. */
        private HostedExample create(ExampleDefinition definition) {
            HostedExample created =
                    Objects.requireNonNull(definition.factory().create(context), "example factory result");
            try {
                created.resize();
                return created;
            } catch (RuntimeException failure) {
                closeAfterCreationFailure(created, failure);
                throw failure;
            }
        }

        /** Preserves the original creation failure while retaining any cleanup failure. */
        private static void closeAfterCreationFailure(HostedExample created, RuntimeException failure) {
            try {
                created.close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }

        private void resize() {
            example.resize();
        }

        private void update(ExampleFrame frame) {
            example.update(frame);
        }

        private void render() {
            example.render();
        }

        @Override
        public void close() {
            example.close();
        }

        /** Finds one suite definition by its stable identifier. */
        private ExampleDefinition definition(String id) {
            return definitions.stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown example identifier: " + id));
        }
    }
}
