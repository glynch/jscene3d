/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayImage;
import io.github.glynch.jscene3d.render.Renderer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Exercises generic screen composition without a native renderer or game-specific overlay. */
final class ScreenComponentsTest {
    /** Resolves anchors, pivots, offsets, and scale relative to a parent region. */
    @Test
    void resolvesScreenRegion() {
        ScreenRegion region = new ScreenRegion(
                new ScreenRegion.Point(1.0F, 1.0F),
                new ScreenRegion.Point(1.0F, 1.0F),
                new ScreenRegion.Point(-10.0F, -5.0F),
                new ScreenRegion.Size(40.0F, 20.0F));

        ScreenRegion.Bounds bounds = region.resolve(new ScreenRegion.Bounds(10.0F, 20.0F, 640.0F, 400.0F), 2.0F);

        assertThat(bounds).isEqualTo(new ScreenRegion.Bounds(550.0F, 370.0F, 80.0F, 40.0F));
    }

    /** Draws decimal glyphs and an optional suffix with authored alignment. */
    @Test
    void paintsMutableBitmapNumber() {
        List<OverlayImageResource> digits = digits();
        OverlayImageResource suffix = image(2, 2, (byte) 42);
        BitmapNumber number = new BitmapNumber(digits, Optional.of(suffix), 42, "right");
        List<Draw> draws = new ArrayList<>();

        number.paint(recording(draws), new ScreenRegion.Bounds(100.0F, 50.0F, 20.0F, 10.0F), 2.0F);

        assertThat(number.value()).isEqualTo(42);
        assertThat(draws).extracting(Draw::x).containsExactly(112.0F, 114.0F, 116.0F);
        number.setValue(7);
        assertThat(number.value()).isEqualTo(7);
        assertThatIllegalArgumentException().isThrownBy(() -> number.setValue(-1));
        digits.forEach(OverlayImageResource::close);
        suffix.close();
    }

    /** Fits a generic image exactly to its enclosing region. */
    @Test
    void paintsScreenImage() {
        OverlayImageResource resource = image(2, 3, (byte) 8);
        ScreenImage image = new ScreenImage(resource);
        List<Draw> draws = new ArrayList<>();

        image.paint(recording(draws), new ScreenRegion.Bounds(1.0F, 2.0F, 30.0F, 40.0F), 5.0F);

        assertThat(draws).containsExactly(new Draw(resource.image(), 1.0F, 2.0F, 30.0F, 40.0F));
        resource.close();
    }

    /** Traverses ordinary enabled child entities and unregisters its overlay exactly once. */
    @Test
    void composesEnabledEntityHierarchy() {
        List<OverlayImageResource> digits = digits();
        BitmapNumber number = new BitmapNumber(digits, Optional.empty(), 5, "right");
        ScreenRegion region = new ScreenRegion(
                new ScreenRegion.Point(1.0F, 1.0F),
                new ScreenRegion.Point(1.0F, 1.0F),
                new ScreenRegion.Point(-10.0F, -10.0F),
                new ScreenRegion.Size(40.0F, 20.0F));
        Entity child = entity(
                true,
                List.of(),
                Map.of(
                        GamePresentationDescriptors.screenRegionCapability(), region,
                        GamePresentationDescriptors.screenContentCapability(), number));
        Entity root = entity(true, List.of(child), Map.of());
        RecordingPresentation presentation = new RecordingPresentation();
        ScreenCanvas canvas = new ScreenCanvas(root, presentation, 320.0F, 200.0F);
        List<Draw> draws = new ArrayList<>();

        canvas.paint(recording(draws), 640, 400);

        assertThat(draws).singleElement().satisfies(draw -> {
            assertThat(draw.x()).isEqualTo(618.0F);
            assertThat(draw.y()).isEqualTo(358.0F);
        });
        canvas.close();
        canvas.close();
        assertThat(presentation.overlay).isNull();
        draws.clear();
        canvas.paint(recording(draws), 640, 400);
        assertThat(draws).isEmpty();
        digits.forEach(OverlayImageResource::close);
    }

    /** Rejects invalid screen configuration at the component boundary. */
    @Test
    void rejectsInvalidScreenConfiguration() {
        List<OverlayImageResource> shortDigits = digits().subList(0, 9);
        List<OverlayImageResource> invalidAlignmentDigits = digits();
        Optional<OverlayImageResource> noSuffix = Optional.empty();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScreenRegion(
                        new ScreenRegion.Point(2.0F, 0.0F),
                        new ScreenRegion.Point(0.0F, 0.0F),
                        new ScreenRegion.Point(0.0F, 0.0F),
                        new ScreenRegion.Size(1.0F, 1.0F)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BitmapNumber(shortDigits, noSuffix, 0, "left"))
                .withMessageContaining("exactly ten");
        assertThatThrownBy(() -> new BitmapNumber(invalidAlignmentDigits, noSuffix, 0, "middle"))
                .isInstanceOf(IllegalArgumentException.class);
        shortDigits.forEach(OverlayImageResource::close);
        invalidAlignmentDigits.forEach(OverlayImageResource::close);
    }

    private static ScreenPainter recording(List<Draw> destination) {
        return (image, x, y, width, height) -> destination.add(new Draw(image, x, y, width, height));
    }

    private static List<OverlayImageResource> digits() {
        List<OverlayImageResource> result = new ArrayList<>();
        for (int digit = 0; digit < 10; digit++) {
            result.add(image(1, 2, (byte) digit));
        }
        return List.copyOf(result);
    }

    private static OverlayImageResource image(int width, int height, byte value) {
        byte[] pixels = new byte[width * height * 4];
        Arrays.fill(pixels, value);
        return OverlayImageResource.owning(OverlayImage.srgbRgba(width, height, pixels));
    }

    private static Entity entity(boolean enabled, List<Entity> children, Map<CapabilityId, Object> capabilities) {
        return (Entity) Proxy.newProxyInstance(
                ScreenComponentsTest.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isEnabled" -> enabled;
                    case "children" -> children;
                    case "capability" -> Optional.ofNullable(capabilities.get(arguments[0]));
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private record Draw(OverlayImage image, float x, float y, float width, float height) {}

    private static final class RecordingPresentation implements PresentationWorldModule {
        private @Nullable Overlay overlay;

        @Override
        public OverlayRegistration registerOverlay(Overlay registered) {
            overlay = registered;
            return () -> overlay = null;
        }

        @Override
        public LocalSound createLocalSound(PcmAudioResource audio, AudioCategory category) {
            throw new UnsupportedOperationException("screen composition creates no sound");
        }

        @Override
        public void renderOverlays(Renderer renderer) {
            throw new UnsupportedOperationException("screen composition test has no renderer");
        }

        @Override
        public void close() {
            overlay = null;
        }
    }
}
