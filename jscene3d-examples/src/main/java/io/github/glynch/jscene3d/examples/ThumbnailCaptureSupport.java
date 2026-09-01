/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.render.OverlayImage;
import io.github.glynch.jscene3d.render.OverlayImageWriter;
import io.github.glynch.jscene3d.render.Renderer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Generates catalogue thumbnails from the actual hosted example implementations. */
public final class ThumbnailCaptureSupport {
    private static final int WIDTH = 760;
    private static final int HEIGHT = 356;

    /** Prevents instantiation of this developer utility. */
    private ThumbnailCaptureSupport() {
        throw new AssertionError("ThumbnailCaptureSupport cannot be instantiated");
    }

    /**
     * Renders and captures every catalogue example to the supplied directory.
     *
     * @param destinationDirectory existing or new output directory
     * @throws NullPointerException if {@code destinationDirectory} is {@code null}
     * @throws IllegalStateException if the directory cannot be created or a PNG cannot be written
     */
    public static void capture(Path destinationDirectory) {
        capture(destinationDirectory, List.of());
    }

    /**
     * Renders and captures selected catalogue examples to the supplied directory.
     *
     * <p>An empty identifier list selects every catalogue entry. Otherwise, entries are captured
     * once in catalogue order, regardless of the order in which identifiers were supplied.
     *
     * @param destinationDirectory existing or new output directory
     * @param exampleIds catalogue identifiers to capture, or an empty list for every example
     * @throws NullPointerException if either argument or an identifier is {@code null}
     * @throws IllegalArgumentException if an identifier is unknown
     * @throws IllegalStateException if the directory cannot be created or a PNG cannot be written
     */
    public static void capture(Path destinationDirectory, List<String> exampleIds) {
        Path validDirectory = Objects.requireNonNull(destinationDirectory, "destinationDirectory");
        List<ExampleCatalogEntry> entries = selectEntries(exampleIds);
        try {
            Files.createDirectories(validDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create thumbnail directory: " + validDirectory, exception);
        }
        WindowOptions options = WindowOptions.builder()
                .size(WIDTH, HEIGHT)
                .title("JScene3D Thumbnail Capture")
                .preferredFramebufferSampleCount(4)
                .build();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window)) {
            ExampleContext context = new ExampleContext(window, renderer);
            for (ExampleCatalogEntry entry : entries) {
                capture(entry, context, validDirectory);
            }
        }
    }

    /** Selects requested entries in stable catalogue order without initializing OpenGL. */
    static List<ExampleCatalogEntry> selectEntries(List<String> exampleIds) {
        List<String> requestedIds = List.copyOf(Objects.requireNonNull(exampleIds, "exampleIds"));
        List<ExampleCatalogEntry> catalog = ExampleCatalog.entries();
        if (requestedIds.isEmpty()) {
            return catalog;
        }
        List<ExampleCatalogEntry> selected = new ArrayList<>();
        for (ExampleCatalogEntry entry : catalog) {
            if (requestedIds.contains(entry.id())) {
                selected.add(entry);
            }
        }
        if (selected.size() != requestedIds.stream().distinct().count()) {
            throw unknownIdentifiers(requestedIds, catalog);
        }
        return List.copyOf(selected);
    }

    /** Creates a clear failure naming unknown requested and available catalogue identifiers. */
    private static IllegalArgumentException unknownIdentifiers(
            List<String> requestedIds, List<ExampleCatalogEntry> catalog) {
        List<String> availableIds =
                catalog.stream().map(ExampleCatalogEntry::id).toList();
        List<String> unknownIds = requestedIds.stream()
                .distinct()
                .filter(id -> !availableIds.contains(id))
                .toList();
        return new IllegalArgumentException(
                "Unknown example catalog ID(s): " + unknownIds + ". Available IDs: " + availableIds);
    }

    /** Captures one settled frame and closes its independently owned resources. */
    private static void capture(ExampleCatalogEntry entry, ExampleContext context, Path directory) {
        try (HostedExample example = entry.factory().create(context)) {
            example.resize();
            for (int frame = 0; frame < 3; frame++) {
                example.update(new ExampleFrame(1.0f / 60.0f, true));
                context.applyRendererViewport();
                example.renderThumbnail();
            }
            OverlayImage image = context.renderer().captureViewport();
            OverlayImageWriter.writePng(directory.resolve(entry.id() + ".png"), image);
        }
    }
}
