/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.ToneMapping;
import java.util.Objects;

/** Restores mutable renderer presentation settings when a hosted example closes. */
public final class RendererSettingsScope implements AutoCloseable {
    private final Renderer renderer;
    private final ToneMapping toneMapping;
    private final float exposure;

    /** Captures the current settings without changing them. */
    private RendererSettingsScope(Renderer renderer) {
        this.renderer = renderer;
        toneMapping = renderer.toneMapping();
        exposure = renderer.exposure();
    }

    /**
     * Captures the settings that must survive switching between hosted examples.
     *
     * @param renderer renderer shared by the example host
     * @return scope that restores the captured settings when closed
     * @throws NullPointerException if {@code renderer} is {@code null}
     * @throws IllegalStateException if {@code renderer} is closed
     */
    public static RendererSettingsScope capture(Renderer renderer) {
        return new RendererSettingsScope(Objects.requireNonNull(renderer, "renderer"));
    }

    /** Restores the captured tone-mapping mode and exposure. */
    @Override
    public void close() {
        renderer.setToneMapping(toneMapping);
        renderer.setExposure(exposure);
    }
}
