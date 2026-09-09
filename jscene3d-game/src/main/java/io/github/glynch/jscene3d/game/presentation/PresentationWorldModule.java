/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.Renderer;

/** World-scoped host seam for ordered screen overlays and listener-relative sound effects. */
public interface PresentationWorldModule extends WorldModule {
    /**
     * Registers one overlay in deterministic insertion order.
     *
     * @param overlay presentation callback invoked on the host render thread
     * @return removable registration owned by the calling component
     */
    OverlayRegistration registerOverlay(Overlay overlay);

    /**
     * Creates one independently retriggerable listener-relative sound.
     *
     * @param audio immutable imported PCM resource
     * @param category volume category
     * @return component-owned playback handle
     */
    LocalSound createLocalSound(PcmAudioResource audio, AudioCategory category);

    /**
     * Renders every currently registered overlay over the existing framebuffer.
     *
     * @param renderer active host renderer
     */
    void renderOverlays(Renderer renderer);
}
