/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Displays a mutable non-negative integer using ten descriptor-selected bitmap glyphs. */
final class BitmapNumber implements ScreenNumber, ScreenContent {
    private final List<OverlayImage> digits;
    private final Optional<OverlayImage> suffix;
    private final Alignment alignment;
    private int value;

    BitmapNumber(
            List<OverlayImageResource> digits, Optional<OverlayImageResource> suffix, int value, String alignment) {
        List<OverlayImageResource> validDigits = List.copyOf(Objects.requireNonNull(digits, "digits"));
        if (validDigits.size() != 10) {
            throw new IllegalArgumentException("digits must contain exactly ten overlay images");
        }
        this.digits = validDigits.stream().map(OverlayImageResource::image).toList();
        this.suffix = Objects.requireNonNull(suffix, "suffix").map(OverlayImageResource::image);
        this.alignment =
                Alignment.valueOf(Objects.requireNonNull(alignment, "alignment").toUpperCase(Locale.ROOT));
        setValue(value);
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public void setValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("screen number value must be non-negative: " + value);
        }
        this.value = value;
    }

    @Override
    public void paint(ScreenPainter painter, ScreenRegion.Bounds bounds, float scale) {
        List<OverlayImage> glyphs = glyphs();
        float contentWidth = 0.0F;
        float contentHeight = 0.0F;
        for (OverlayImage glyph : glyphs) {
            contentWidth += glyph.width() * scale;
            contentHeight = Math.max(contentHeight, glyph.height() * scale);
        }
        float x = alignment == Alignment.LEFT ? bounds.x() : bounds.x() + bounds.width() - contentWidth;
        for (OverlayImage glyph : glyphs) {
            float width = glyph.width() * scale;
            float height = glyph.height() * scale;
            painter.image(glyph, x, bounds.y() + (bounds.height() - height) * 0.5F, width, height);
            x += width;
        }
    }

    private List<OverlayImage> glyphs() {
        String text = Integer.toString(value);
        List<OverlayImage> result =
                new ArrayList<>(text.length() + suffix.map(ignored -> 1).orElse(0));
        for (int index = 0; index < text.length(); index++) {
            result.add(digits.get(text.charAt(index) - '0'));
        }
        suffix.ifPresent(result::add);
        return result;
    }

    private enum Alignment {
        LEFT,
        RIGHT
    }
}
