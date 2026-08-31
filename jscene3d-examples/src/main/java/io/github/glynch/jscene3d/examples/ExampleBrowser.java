/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.gui.GalleryItem;
import io.github.glynch.jscene3d.gui.GalleryPanel;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.List;
import java.util.Objects;

/** Searchable native gallery that runs every catalogue example in one shared renderer window. */
public final class ExampleBrowser {
    private static final float MAXIMUM_FRAME_SECONDS = 0.1f;

    /** Prevents instantiation of this example-browser entry point. */
    private ExampleBrowser() {
        throw new AssertionError("ExampleBrowser cannot be instantiated");
    }

    /**
     * Opens the native searchable example browser.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        WindowOptions options = WindowOptions.builder()
                .size(1440, 900)
                .title("JScene3D Examples")
                .preferredFramebufferSampleCount(4)
                .build();
        List<ExampleDefinition> definitions = ExampleCatalog.definitions();
        List<GalleryItem> galleryItems =
                definitions.stream().map(ExampleDefinition::galleryItem).toList();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window)) {
            ExampleContext context = new ExampleContext(window, renderer);
            context.setSidebarWidth(GalleryPanel.WIDTH);
            GalleryPanel gallery = new GalleryPanel(window, "JScene3D", galleryItems);
            try (BrowserSession session = new BrowserSession(context, definitions, gallery)) {
                window.show();
                run(window, context, gallery, renderer, session);
            }
        }
    }

    /** Runs the event, example, and overlay loop until the native window closes. */
    private static void run(
            Window window, ExampleContext context, GalleryPanel gallery, Renderer renderer, BrowserSession session) {
        long previousNanos = System.nanoTime();
        while (!window.shouldClose()) {
            Window.pollEvents();
            if (window.input().wasKeyPressed(Key.ESCAPE)) {
                window.requestClose();
            }
            if (window.framebufferSizeChanged()) {
                context.refreshDimensions();
                session.resize();
            }
            gallery.update();
            session.select(gallery.selectedItem().id());
            long nowNanos = System.nanoTime();
            float elapsedSeconds =
                    Math.clamp((nowNanos - previousNanos) / 1_000_000_000.0f, 0.0f, MAXIMUM_FRAME_SECONDS);
            previousNanos = nowNanos;
            boolean pointerCaptured = gallery.capturesPointer() || !context.containsPointer();
            session.update(new ExampleFrame(elapsedSeconds, pointerCaptured));
            context.applyRendererViewport();
            session.render();
            renderer.render(gallery);
            window.swapBuffers();
        }
    }

    /** Owns the currently selected live example and replaces it atomically on selection. */
    private static final class BrowserSession implements AutoCloseable {
        private final ExampleContext context;
        private final List<ExampleDefinition> definitions;

        private String selectedId;
        private HostedExample example;

        /** Creates and sizes the first catalogue example. */
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
                try {
                    created.close();
                } catch (RuntimeException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        }

        /** Forwards content-area dimension changes. */
        private void resize() {
            example.resize();
        }

        /** Advances the current example. */
        private void update(ExampleFrame frame) {
            example.update(frame);
        }

        /** Draws the current example into the assigned renderer viewport. */
        private void render() {
            example.render();
        }

        /** Closes the current example. */
        @Override
        public void close() {
            example.close();
        }

        /** Finds one catalogue definition by its stable identifier. */
        private ExampleDefinition definition(String id) {
            for (ExampleDefinition candidate : definitions) {
                if (candidate.id().equals(id)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown example identifier: " + id);
        }
    }
}
