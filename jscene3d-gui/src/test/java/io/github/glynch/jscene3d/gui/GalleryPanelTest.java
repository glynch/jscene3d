/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GalleryPanelTest {
    private static final OverlayImage THUMBNAIL =
            OverlayImage.srgbRgba(1, 1, new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});

    @Test
    void filtersByMetadataAndSupportsUnicodeAwareBackspace() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());
        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, true, 0.0, false, ""), 1000, 720);

        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, false, 0.0, false, "light"), 1000, 720);

        assertThat(panel.query()).isEqualTo("light");
        assertThat(panel.matchingItemCount()).isEqualTo(1);

        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, false, 0.0, false, "🚀"), 1000, 720);
        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, false, 0.0, true, ""), 1000, 720);

        assertThat(panel.query()).isEqualTo("light");
    }

    @Test
    void selectsVisibleCardsAndScrollsThroughResults() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());

        panel.update(new GalleryPanel.GalleryInput(40.0, 380.0, true, 0.0, false, ""), 1000, 720);
        assertThat(panel.selectedItem().id()).isEqualTo("lights");

        boolean changed = panel.update(new GalleryPanel.GalleryInput(40.0, 350.0, false, -1.0, false, ""), 1000, 400);

        assertThat(changed).isTrue();
        assertThat(panel.capturesPointer()).isTrue();
    }

    @Test
    void paintsSidebarCardsAndThumbnails() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        panel.paint(canvas, 1000, 720);

        assertThat(canvas.rectangleCount()).isGreaterThan(1);
        assertThat(canvas.roundedRectangleCount()).isGreaterThan(1);
        assertThat(canvas.imageCount()).isGreaterThan(0);
        assertThat(canvas.alphaMaskCount()).isGreaterThan(0);
    }

    @Test
    void rejectsEmptyItemLists() {
        assertThatIllegalArgumentException().isThrownBy(() -> new GalleryPanel("JScene3D", List.of()));
    }

    @Test
    void clearsSearchAndPaintsNoResultState() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();
        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, true, 0.0, false, ""), 1000, 720);
        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, false, 0.0, false, "no-match"), 1000, 720);

        panel.paint(canvas, 1000, 720);

        assertThat(panel.matchingItemCount()).isZero();
        assertThat(canvas.alphaMaskCount()).isGreaterThan(0);

        panel.update(new GalleryPanel.GalleryInput(305.0, 75.0, true, 0.0, false, ""), 1000, 720);
        assertThat(panel.query()).isEmpty();
        assertThat(panel.matchingItemCount()).isEqualTo(3);
    }

    @Test
    void scrollsInBothDirectionsAndReleasesFocusOutsideSidebar() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());
        panel.update(new GalleryPanel.GalleryInput(20.0, 75.0, true, 0.0, false, ""), 1000, 400);
        panel.update(new GalleryPanel.GalleryInput(40.0, 350.0, false, -1.0, false, ""), 1000, 400);
        panel.update(new GalleryPanel.GalleryInput(40.0, 350.0, false, 1.0, false, ""), 1000, 400);

        assertThat(panel.update(new GalleryPanel.GalleryInput(500.0, 350.0, true, 0.0, false, ""), 1000, 400))
                .isTrue();
        assertThat(panel.isSearchFocused()).isFalse();
        assertThat(panel.capturesPointer()).isFalse();
    }

    @Test
    void validatesDetachedOperationsAndPaintDimensions() {
        GalleryPanel panel = new GalleryPanel("JScene3D", items());

        assertThatIllegalStateException().isThrownBy(panel::update);
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(new RecordingGuiCanvas(), 0, 720));
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(new RecordingGuiCanvas(), 1000, 0));
    }

    /** Creates three searchable gallery fixtures in display order. */
    private static List<GalleryItem> items() {
        return List.of(
                new GalleryItem(
                        "materials", "Materials", "Features", "Material comparison", List.of("phong"), THUMBNAIL),
                new GalleryItem("lights", "Lighting", "Features", "Light comparison", List.of("spot"), THUMBNAIL),
                new GalleryItem("solar", "Solar System", "Showcases", "Planet viewer", List.of("texture"), THUMBNAIL));
    }
}
