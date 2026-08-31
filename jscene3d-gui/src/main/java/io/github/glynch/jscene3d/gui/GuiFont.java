/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.lwjgl.stb.STBTruetype.stbtt_BakeFontBitmap;

import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;

/** Baked Inter atlas and retained Java glyph metrics for dependency-free GUI text. */
final class GuiFont {
    private static final String RESOURCE = "/io/github/glynch/jscene3d/gui/font/Inter.ttf";
    private static final int FIRST_CHARACTER = 32;
    private static final int CHARACTER_COUNT = 95;
    private static final int FALLBACK_CHARACTER = '?';
    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 512;
    private static final float BAKED_SIZE = 28.0f;
    private static final GuiFont DEFAULT = load();

    private final Glyph[] glyphs;

    /** Retains copied glyph metrics and reusable atlas regions. */
    private GuiFont(Glyph[] glyphs) {
        this.glyphs = glyphs;
    }

    /** Returns the shared bundled Inter font. */
    static GuiFont defaultFont() {
        return DEFAULT;
    }

    /** Appends one line of TrueType text with a top-aligned origin. */
    void text(GuiCanvas canvas, float x, float y, String text, float size, Color color) {
        float scale = size / BAKED_SIZE;
        float cursor = x;
        float baseline = y + size;
        for (int index = 0; index < text.length(); index++) {
            Glyph glyph = glyph(text.charAt(index));
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
        }
    }

    /** Returns the logical width of one line at the requested size. */
    float width(String text, float size) {
        float advance = 0.0f;
        for (int index = 0; index < text.length(); index++) {
            advance += glyph(text.charAt(index)).advance;
        }
        return advance * size / BAKED_SIZE;
    }

    /** Returns a supported glyph or the question-mark fallback. */
    private Glyph glyph(char character) {
        int index = character - FIRST_CHARACTER;
        if (index < 0 || index >= glyphs.length) {
            index = FALLBACK_CHARACTER - FIRST_CHARACTER;
        }
        return glyphs[index];
    }

    /** Loads, bakes, and copies the bundled font without retaining native allocations. */
    private static GuiFont load() {
        ByteBuffer fontData = loadResource();
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);
        try (STBTTBakedChar.Buffer bakedCharacters = STBTTBakedChar.malloc(CHARACTER_COUNT)) {
            int result = stbtt_BakeFontBitmap(
                    fontData, BAKED_SIZE, bitmap, ATLAS_WIDTH, ATLAS_HEIGHT, FIRST_CHARACTER, bakedCharacters);
            if (result <= 0) {
                throw new IllegalStateException("Bundled Inter font does not fit the GUI atlas");
            }
            BakedGlyph[] bakedGlyphs = new BakedGlyph[CHARACTER_COUNT];
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
            return new GuiFont(glyphs);
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
}
