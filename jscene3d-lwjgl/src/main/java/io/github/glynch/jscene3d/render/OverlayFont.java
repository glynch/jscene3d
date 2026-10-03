/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.stb.STBTruetype.stbtt_BakeFontBitmap;

import io.github.glynch.jscene3d.math.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;

/** Bundled TrueType glyph atlases and logical metrics for renderer-owned overlay text. */
final class OverlayFont {
    private static final String RESOURCE = "/io/github/glynch/jscene3d/render/font/Inter.ttf";
    private static final int BASIC_LATIN_FIRST = 0x0020;
    private static final int BASIC_LATIN_COUNT = 0x007e - BASIC_LATIN_FIRST + 1;
    private static final int LATIN_ONE_FIRST = 0x00a0;
    private static final int LATIN_ONE_COUNT = 0x00ff - LATIN_ONE_FIRST + 1;
    private static final int GENERAL_PUNCTUATION_FIRST = 0x2000;
    private static final int GENERAL_PUNCTUATION_COUNT = 0x206f - GENERAL_PUNCTUATION_FIRST + 1;
    private static final int FALLBACK_CHARACTER = '?';
    private static final int ATLAS_WIDTH = 1_024;
    private static final int ATLAS_HEIGHT = 1_024;
    private static final float BAKED_SIZE = 48.0f;

    private final List<GlyphRange> glyphRanges;
    private final Glyph fallbackGlyph;

    /** Retains copied glyph metrics and immutable alpha-mask atlases. */
    private OverlayFont(List<GlyphRange> glyphRanges) {
        this.glyphRanges = List.copyOf(glyphRanges);
        fallbackGlyph = glyph(FALLBACK_CHARACTER, false);
    }

    /** Loads the bundled renderer font and releases all temporary native structures. */
    static OverlayFont load() {
        ByteBuffer fontData = loadResource();
        return new OverlayFont(List.of(
                bakeRange(fontData, BASIC_LATIN_FIRST, BASIC_LATIN_COUNT),
                bakeRange(fontData, LATIN_ONE_FIRST, LATIN_ONE_COUNT),
                bakeRange(fontData, GENERAL_PUNCTUATION_FIRST, GENERAL_PUNCTUATION_COUNT)));
    }

    /** Appends one aligned, top-origin line through the generic alpha-mask primitive. */
    void text(OverlayCanvas canvas, TextRun run) {
        float scale = run.size() / BAKED_SIZE;
        float cursor = alignedOrigin(run.anchorX(), width(run.text(), run.size()), run.alignment());
        float baseline = run.top() + run.size();
        int index = 0;
        while (index < run.text().length()) {
            int codePoint = run.text().codePointAt(index);
            Glyph glyph = glyph(codePoint);
            float width = glyph.width() * scale;
            float height = glyph.height() * scale;
            if (width > 0.0f && height > 0.0f) {
                canvas.alphaMask(
                        glyph.region(),
                        cursor + glyph.offsetX() * scale,
                        baseline + glyph.offsetY() * scale,
                        width,
                        height,
                        run.color(),
                        run.alpha());
            }
            cursor += glyph.advance() * scale;
            index += Character.charCount(codePoint);
        }
    }

    /** Returns the logical advance width of one line at the requested size. */
    float width(String text, float size) {
        float advance = 0.0f;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            advance += glyph(codePoint).advance();
            index += Character.charCount(codePoint);
        }
        return advance * size / BAKED_SIZE;
    }

    /** Converts an alignment anchor to a left-origin cursor. */
    private static float alignedOrigin(float anchorX, float width, OverlayTextAlignment alignment) {
        return switch (alignment) {
            case LEFT -> anchorX;
            case CENTER -> anchorX - width * 0.5f;
            case RIGHT -> anchorX - width;
        };
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
        throw new IllegalStateException("Bundled overlay font does not contain the fallback glyph");
    }

    /** Bakes and copies one contiguous Unicode range into an immutable alpha-mask atlas. */
    private static GlyphRange bakeRange(ByteBuffer fontData, int firstCodePoint, int characterCount) {
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);
        try (STBTTBakedChar.Buffer bakedCharacters = STBTTBakedChar.malloc(characterCount)) {
            int result = stbtt_BakeFontBitmap(
                    fontData, BAKED_SIZE, bitmap, ATLAS_WIDTH, ATLAS_HEIGHT, firstCodePoint, bakedCharacters);
            if (result <= 0) {
                String rangeStart = Integer.toHexString(firstCodePoint).toUpperCase(Locale.ROOT);
                throw new IllegalStateException("Bundled Inter range does not fit the overlay atlas: U+" + rangeStart);
            }
            OverlayImage atlas = createAtlas(bitmap);
            Glyph[] glyphs = new Glyph[characterCount];
            for (int index = 0; index < glyphs.length; index++) {
                STBTTBakedChar baked = bakedCharacters.get(index);
                int minimumX = Short.toUnsignedInt(baked.x0());
                int minimumY = Short.toUnsignedInt(baked.y0());
                int maximumX = Short.toUnsignedInt(baked.x1());
                int maximumY = Short.toUnsignedInt(baked.y1());
                glyphs[index] = new Glyph(
                        atlas.region(
                                minimumX / (float) ATLAS_WIDTH,
                                minimumY / (float) ATLAS_HEIGHT,
                                maximumX / (float) ATLAS_WIDTH,
                                maximumY / (float) ATLAS_HEIGHT),
                        (float) maximumX - minimumX,
                        (float) maximumY - minimumY,
                        baked.xoff(),
                        baked.yoff(),
                        baked.xadvance());
            }
            return new GlyphRange(firstCodePoint, glyphs);
        }
    }

    /** Copies a temporary direct bitmap into an immutable overlay image. */
    private static OverlayImage createAtlas(ByteBuffer bitmap) {
        byte[] pixels = new byte[bitmap.capacity()];
        bitmap.get(0, pixels);
        return OverlayImage.alphaMask(ATLAS_WIDTH, ATLAS_HEIGHT, pixels);
    }

    /** Reads the complete bundled TrueType resource into direct storage. */
    private static ByteBuffer loadResource() {
        try (InputStream input = OverlayFont.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled overlay font: " + RESOURCE);
            }
            byte[] bytes = input.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).flip();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load bundled overlay font: " + RESOURCE, exception);
        }
    }

    /** Validated immutable text line passed from the public canvas boundary. */
    record TextRun(
            String text,
            float anchorX,
            float top,
            float size,
            Color color,
            float alpha,
            OverlayTextAlignment alignment) {}

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
