/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

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

/** Captures representative frames from any explicit example suite. */
public final class ThumbnailCaptureSupport {
    private static final int WIDTH = 760;
    private static final int HEIGHT = 356;

    /** Prevents instantiation of this developer utility. */
    private ThumbnailCaptureSupport() {
        throw new AssertionError("ThumbnailCaptureSupport cannot be instantiated");
    }

    /**
     * Captures every example in the supplied suite.
     *
     * @param suite suite whose examples will be captured
     * @param destinationDirectory directory that receives the PNG files
     */
    public static void capture(ExampleSuite suite, Path destinationDirectory) {
        capture(suite, destinationDirectory, List.of());
    }

    /**
     * Captures selected examples, or the complete suite when no identifiers are supplied.
     *
     * <p>Selected examples retain suite order and are captured once even when an identifier is
     * repeated.
     *
     * @param suite suite whose examples will be captured
     * @param destinationDirectory directory that receives the PNG files
     * @param exampleIds stable identifiers to capture, or an empty list to capture the suite
     */
    public static void capture(ExampleSuite suite, Path destinationDirectory, List<String> exampleIds) {
        ExampleSuite validSuite = Objects.requireNonNull(suite, "suite");
        Path validDirectory = Objects.requireNonNull(destinationDirectory, "destinationDirectory");
        List<ExampleDefinition> definitions = selectDefinitions(validSuite, exampleIds);
        createDirectories(validDirectory);
        WindowOptions options = WindowOptions.builder()
                .size(WIDTH, HEIGHT)
                .title(validSuite.windowTitle() + " Thumbnail Capture")
                .preferredFramebufferSampleCount(4)
                .build();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window)) {
            ExampleContext context = new ExampleContext(window, renderer);
            for (ExampleDefinition definition : definitions) {
                capture(definition, context, validDirectory);
            }
        }
    }

    /** Selects requested definitions in stable suite order without initializing OpenGL. */
    static List<ExampleDefinition> selectDefinitions(ExampleSuite suite, List<String> exampleIds) {
        ExampleSuite validSuite = Objects.requireNonNull(suite, "suite");
        List<String> requestedIds = List.copyOf(Objects.requireNonNull(exampleIds, "exampleIds"));
        List<ExampleDefinition> definitions = validSuite.definitions();
        if (requestedIds.isEmpty()) {
            return definitions;
        }
        List<ExampleDefinition> selected = new ArrayList<>();
        for (ExampleDefinition definition : definitions) {
            if (requestedIds.contains(definition.id())) {
                selected.add(definition);
            }
        }
        if (selected.size() != requestedIds.stream().distinct().count()) {
            throw unknownIdentifiers(requestedIds, definitions);
        }
        return List.copyOf(selected);
    }

    /** Creates a clear failure naming unknown requested and available identifiers. */
    private static IllegalArgumentException unknownIdentifiers(
            List<String> requestedIds, List<ExampleDefinition> definitions) {
        List<String> availableIds =
                definitions.stream().map(ExampleDefinition::id).toList();
        List<String> unknownIds = requestedIds.stream()
                .distinct()
                .filter(id -> !availableIds.contains(id))
                .toList();
        return new IllegalArgumentException(
                "Unknown example catalog ID(s): " + unknownIds + ". Available IDs: " + availableIds);
    }

    /** Creates the output directory or fails with its exact path. */
    private static void createDirectories(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create thumbnail directory: " + directory, exception);
        }
    }

    /** Captures one settled frame and closes its independently owned resources. */
    private static void capture(ExampleDefinition definition, ExampleContext context, Path directory) {
        try (HostedExample example = definition.factory().create(context)) {
            example.resize();
            for (int frame = 0; frame < 3; frame++) {
                example.update(new ExampleFrame(1.0F / 60.0F, true, true));
                context.applyRendererViewport();
                example.renderThumbnail();
            }
            OverlayImage image = context.renderer().captureViewport();
            OverlayImageWriter.writePng(directory.resolve(definition.id() + ".png"), image);
        }
    }
}
