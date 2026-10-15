/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraIconState.Bounds;
import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

/** Verifies the semantic geometry and appearance of authored-camera overlay icons. */
final class SceneCameraIconRendererTest {
    @Test
    void laysOutARecognizableCameraSilhouetteInsideThePickBounds() {
        Bounds bounds = new Bounds(10.0f, 20.0f, 24.0f, 20.0f);

        SceneCameraIconRenderer.Silhouette silhouette = SceneCameraIconRenderer.silhouette(bounds);

        assertThat(silhouette.body()).isEqualTo(new Bounds(12.0f, 26.0f, 13.0f, 11.0f));
        assertThat(silhouette.viewfinder()).isEqualTo(new Bounds(15.0f, 23.0f, 7.0f, 4.0f));
        assertThat(silhouette.lens())
                .isEqualTo(new SceneCameraIconRenderer.Lens(25.0f, 27.0f, 32.0f, 24.0f, 32.0f, 38.0f));
    }

    @Test
    void usesNeutralAndJScene3DSelectionColoursWithAContrastingOutline() {
        SceneCameraIconRenderer.Appearance unselected = SceneCameraIconRenderer.appearance(false);
        SceneCameraIconRenderer.Appearance selected = SceneCameraIconRenderer.appearance(true);

        assertThat(unselected.fill()).isEqualTo(Color.srgb(0xd7dbe5));
        assertThat(selected.fill()).isEqualTo(Color.srgb(0xb69cff));
        assertThat(unselected.outline()).isEqualTo(Color.srgb(0x161a22));
        assertThat(selected.outline()).isEqualTo(unselected.outline());
    }
}
