/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.ProjectLoadProgressReporter;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.OverlayImage;
import io.github.glynch.jscene3d.render.Renderer;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Project-owned launch artwork and truthful synchronous loading feedback. */
final class DesktopSplashScreen {
    private static final Color TRACK_COLOR = Color.srgb(0x252b30);
    private static final Color PROGRESS_COLOR = Color.srgb(0xef762f);

    private final String projectName;
    private final Optional<OverlayImage> background;
    private final Optional<OverlayImage> title;
    private final Optional<OverlayImage> studioLogo;
    private final List<OverlayImage> poweredByBadges;
    private final Duration minimumDuration;
    private ProjectLoadProgressReporter.Phase phase = ProjectLoadProgressReporter.Phase.MANIFEST;

    /** Loads configured launch images before any runtime world is composed. */
    private DesktopSplashScreen(String projectName, Optional<GameProject.SplashConfiguration> configuration) {
        this.projectName = Objects.requireNonNull(projectName, "projectName");
        background =
                configuration.map(GameProject.SplashConfiguration::background).map(OverlayImageLoader::load);
        title = configuration.map(GameProject.SplashConfiguration::title).map(OverlayImageLoader::load);
        studioLogo = configuration
                .flatMap(GameProject.SplashConfiguration::studioLogo)
                .map(OverlayImageLoader::load);
        poweredByBadges = configuration.stream()
                .flatMap(splash -> splash.poweredByBadges().stream())
                .map(OverlayImageLoader::load)
                .toList();
        minimumDuration = configuration
                .map(GameProject.SplashConfiguration::minimumDuration)
                .orElse(Duration.ZERO);
    }

    /** Creates launch presentation for one validated project. */
    static DesktopSplashScreen load(GameProject project) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        return new DesktopSplashScreen(
                validProject.identity().name(), validProject.launch().splash());
    }

    /** Returns the configured minimum time this launch screen must remain visible. */
    Duration minimumDuration() {
        return minimumDuration;
    }

    /** Repaints one real load phase and keeps the native window responsive. */
    void present(ProjectLoadProgressReporter.Phase current, Renderer renderer, Window window) {
        phase = Objects.requireNonNull(current, "current");
        Window.pollEvents();
        window.setTitle(projectName + " — " + phase.description());
        if (window.framebufferWidth() > 0 && window.framebufferHeight() > 0) {
            renderer.clear();
            renderer.render(this::paint);
            window.swapBuffers();
        }
    }

    /** Paints launch artwork and a determinate phase bar in logical coordinates. */
    private void paint(OverlayCanvas canvas, int width, int height) {
        background.ifPresent(image -> canvas.image(image.fullRegion(), 0.0F, 0.0F, width, height, Color.WHITE, 1.0F));
        title.ifPresent(image -> drawCentered(canvas, image, width, width * 0.56F, height * 0.09F));
        studioLogo.ifPresent(image -> drawFitted(canvas, image, width * 0.04F, height * 0.05F, width * 0.18F));
        drawBadges(canvas, width, height);
        float trackWidth = width * 0.42F;
        float trackX = (width - trackWidth) * 0.5F;
        float trackY = height - Math.max(24.0F, height * 0.04F);
        canvas.roundedRectangle(trackX, trackY, trackWidth, 8.0F, 4.0F, TRACK_COLOR, 0.95F);
        canvas.roundedRectangle(trackX, trackY, trackWidth * phase.fraction(), 8.0F, 4.0F, PROGRESS_COLOR, 1.0F);
    }

    /** Draws configured middleware badges above the loading bar. */
    private void drawBadges(OverlayCanvas canvas, int width, int height) {
        float y = height * 0.77F;
        for (OverlayImage badge : poweredByBadges) {
            float drawnWidth = Math.min(width * 0.27F, badge.width());
            drawCentered(canvas, badge, width, drawnWidth, y);
            y += drawnWidth * badge.height() / badge.width() + 10.0F;
        }
    }

    /** Draws one image centered horizontally at its preserved aspect ratio. */
    private static void drawCentered(
            OverlayCanvas canvas, OverlayImage image, float viewportWidth, float width, float y) {
        float height = width * image.height() / image.width();
        canvas.image(image.fullRegion(), (viewportWidth - width) * 0.5F, y, width, height, Color.WHITE, 1.0F);
    }

    /** Draws one image at its preserved aspect ratio. */
    private static void drawFitted(OverlayCanvas canvas, OverlayImage image, float x, float y, float width) {
        float height = width * image.height() / image.width();
        canvas.image(image.fullRegion(), x, y, width, height, Color.WHITE, 1.0F);
    }
}
