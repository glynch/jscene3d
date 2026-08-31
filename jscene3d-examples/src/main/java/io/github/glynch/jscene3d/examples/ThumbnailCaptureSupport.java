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
        Path validDirectory = Objects.requireNonNull(destinationDirectory, "destinationDirectory");
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
            for (ExampleDefinition definition : ExampleCatalog.definitions()) {
                capture(definition, context, validDirectory);
            }
        }
    }

    /** Captures one settled frame and closes its independently owned resources. */
    private static void capture(ExampleDefinition definition, ExampleContext context, Path directory) {
        try (HostedExample example = definition.factory().create(context)) {
            example.resize();
            for (int frame = 0; frame < 3; frame++) {
                example.update(new ExampleFrame(1.0f / 60.0f, true));
                context.applyRendererViewport();
                example.renderThumbnail();
            }
            OverlayImage image = context.renderer().captureViewport();
            OverlayImageWriter.writePng(directory.resolve(definition.id() + ".png"), image);
        }
    }
}
