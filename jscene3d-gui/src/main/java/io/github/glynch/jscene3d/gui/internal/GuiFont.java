/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui.internal;

import static org.lwjgl.stb.STBTruetype.stbtt_BakeFontBitmap;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;

/** Baked Inter atlases and retained Java glyph metrics for dependency-free GUI text. */
public final class GuiFont {
    private static final String RESOURCE = "/io/github/glynch/jscene3d/gui/font/Inter.ttf";
    private static final int ASCII_FIRST_CHARACTER = 32;
    private static final int ASCII_CHARACTER_COUNT = 95;
    private static final int PUNCTUATION_FIRST_CHARACTER = 0x2010;
    private static final int PUNCTUATION_CHARACTER_COUNT = 0x2026 - PUNCTUATION_FIRST_CHARACTER + 1;
    private static final int FALLBACK_CHARACTER = '?';
    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 512;
    private static final float BAKED_SIZE = 28.0f;
    private static final GuiFont DEFAULT = load();

    private final List<GlyphRange> glyphRanges;
    private final Glyph fallbackGlyph;

    /** Retains copied glyph metrics and reusable atlas regions. */
    private GuiFont(List<GlyphRange> glyphRanges) {
        this.glyphRanges = List.copyOf(glyphRanges);
        this.fallbackGlyph = glyph(FALLBACK_CHARACTER, false);
    }

    /**
     * Returns the shared bundled Inter font.
     *
     * @return shared GUI font
     */
    public static GuiFont defaultFont() {
        return DEFAULT;
    }

    /**
     * Appends one line of TrueType text with a top-aligned origin.
     *
     * @param canvas destination drawing boundary
     * @param x left coordinate
     * @param y top coordinate
     * @param text text to append
     * @param size logical font size
     * @param color text color
     */
    public void text(GuiCanvas canvas, float x, float y, String text, float size, Color color) {
        float scale = size / BAKED_SIZE;
        float cursor = x;
        float baseline = y + size;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            Glyph glyph = glyph(codePoint);
            float width = glyph.width * scale;
            float height = glyph.height * scale;
            if (width > 0.0f && height > 0.0f) {
                canvas.alphaMask(
                        glyph.region,
                        cursor + glyph.offsetX * scale,
                        baseline + glyph.offsetY * scale,
                        width,
                        height,
                        color,
                        1.0f);
            }
            cursor += glyph.advance * scale;
            index += Character.charCount(codePoint);
        }
    }

    /**
     * Returns the logical width of one line at the requested size.
     *
     * @param text text to measure
     * @param size logical font size
     * @return logical text width
     */
    public float width(String text, float size) {
        float advance = 0.0f;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            advance += glyph(codePoint).advance;
            index += Character.charCount(codePoint);
        }
        return advance * size / BAKED_SIZE;
    }

    /** Returns a supported glyph or the question-mark fallback. */
    private Glyph glyph(int codePoint) {
        return glyph(codePoint, true);
    }

    /** Resolves a glyph from the baked ranges, optionally using the fallback. */
    private Glyph glyph(int codePoint, boolean useFallback) {
        for (GlyphRange range : glyphRanges) {
            if (range.contains(codePoint)) {
                return range.glyph(codePoint);
            }
        }
        if (useFallback) {
            return fallbackGlyph;
        }
        throw new IllegalStateException("Bundled GUI font does not contain the fallback glyph");
    }

    /** Loads, bakes, and copies the bundled font without retaining native allocations. */
    private static GuiFont load() {
        ByteBuffer fontData = loadResource();
        return new GuiFont(List.of(
                bakeRange(fontData, ASCII_FIRST_CHARACTER, ASCII_CHARACTER_COUNT),
                bakeRange(fontData, PUNCTUATION_FIRST_CHARACTER, PUNCTUATION_CHARACTER_COUNT)));
    }

    /** Bakes and copies one contiguous Unicode range into a retained atlas. */
    private static GlyphRange bakeRange(ByteBuffer fontData, int firstCodePoint, int characterCount) {
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);
        try (STBTTBakedChar.Buffer bakedCharacters = STBTTBakedChar.malloc(characterCount)) {
            int result = stbtt_BakeFontBitmap(
                    fontData, BAKED_SIZE, bitmap, ATLAS_WIDTH, ATLAS_HEIGHT, firstCodePoint, bakedCharacters);
            if (result <= 0) {
                String rangeStart = Integer.toHexString(firstCodePoint).toUpperCase(Locale.ROOT);
                throw new IllegalStateException("Bundled Inter font range does not fit the GUI atlas: U+" + rangeStart);
            }
            BakedGlyph[] bakedGlyphs = new BakedGlyph[characterCount];
            for (int index = 0; index < bakedGlyphs.length; index++) {
                STBTTBakedChar baked = bakedCharacters.get(index);
                bakedGlyphs[index] = new BakedGlyph(
                        Short.toUnsignedInt(baked.x0()),
                        Short.toUnsignedInt(baked.y0()),
                        Short.toUnsignedInt(baked.x1()),
                        Short.toUnsignedInt(baked.y1()),
                        baked.xoff(),
                        baked.yoff(),
                        baked.xadvance());
            }
            byte[] pixels = new byte[bitmap.capacity()];
            bitmap.get(0, pixels);
            OverlayImage atlas = OverlayImage.alphaMask(ATLAS_WIDTH, ATLAS_HEIGHT, pixels);
            Glyph[] glyphs = new Glyph[bakedGlyphs.length];
            for (int index = 0; index < glyphs.length; index++) {
                BakedGlyph baked = bakedGlyphs[index];
                glyphs[index] = new Glyph(
                        atlas.region(
                                baked.minimumX / (float) ATLAS_WIDTH,
                                baked.minimumY / (float) ATLAS_HEIGHT,
                                baked.maximumX / (float) ATLAS_WIDTH,
                                baked.maximumY / (float) ATLAS_HEIGHT),
                        (float) baked.maximumX - baked.minimumX,
                        (float) baked.maximumY - baked.minimumY,
                        baked.offsetX,
                        baked.offsetY,
                        baked.advance);
            }
            return new GlyphRange(firstCodePoint, glyphs);
        }
    }

    /** Reads the complete bundled TrueType resource into direct storage. */
    private static ByteBuffer loadResource() {
        try (InputStream input = GuiFont.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled GUI font: " + RESOURCE);
            }
            byte[] bytes = input.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).flip();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load bundled GUI font: " + RESOURCE, exception);
        }
    }

    /** Temporary baked atlas coordinates and baseline-relative metrics. */
    private record BakedGlyph(
            int minimumX, int minimumY, int maximumX, int maximumY, float offsetX, float offsetY, float advance) {}

    /** One glyph's reusable atlas region and baseline-relative metrics. */
    private record Glyph(
            OverlayImage.Region region, float width, float height, float offsetX, float offsetY, float advance) {}

    /** One contiguous baked Unicode range. */
    private static final class GlyphRange {
        private final int firstCodePoint;
        private final Glyph[] glyphs;

        /** Retains the first code point and its sequential glyphs. */
        private GlyphRange(int firstCodePoint, Glyph[] glyphs) {
            this.firstCodePoint = firstCodePoint;
            this.glyphs = glyphs;
        }

        /** Returns whether this range contains the code point. */
        private boolean contains(int codePoint) {
            int index = codePoint - firstCodePoint;
            return index >= 0 && index < glyphs.length;
        }

        /** Returns the glyph for a code point known to be in this range. */
        private Glyph glyph(int codePoint) {
            return glyphs[codePoint - firstCodePoint];
        }
    }
}
