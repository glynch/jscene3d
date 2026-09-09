/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.Renderer;
import org.joml.Vector3fc;

/** World-scoped host seam for ordered screen overlays and local or world-positioned sound effects. */
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
     * Creates one independently retriggerable world-positioned sound.
     *
     * @param audio immutable imported mono PCM resource
     * @param category volume category
     * @param attenuation authored world-distance attenuation
     * @return component-owned positional playback handle
     */
    PositionalSound createPositionalSound(
            PcmAudioResource audio, AudioCategory category, PositionalSoundAttenuation attenuation);

    /**
     * Updates the listener pose used by world-positioned sounds.
     *
     * @param position listener world position
     * @param forward non-zero world direction faced by the listener
     * @param up non-zero world direction above the listener
     */
    void setListenerTransform(Vector3fc position, Vector3fc forward, Vector3fc up);

    /**
     * Renders every currently registered overlay over the existing framebuffer.
     *
     * @param renderer active host renderer
     */
    void renderOverlays(Renderer renderer);
}
