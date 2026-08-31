/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.render.OverlayImage;

/** Creates compact full-colour catalogue artwork without runtime file-system dependencies. */
final class ExampleThumbnailFactory {
    private static final int WIDTH = 304;
    private static final int HEIGHT = 142;

    /** Prevents instantiation of this stateless factory. */
    private ExampleThumbnailFactory() {
        throw new AssertionError("ExampleThumbnailFactory cannot be instantiated");
    }

    /** Creates a dark gradient thumbnail with a deterministic scene-like motif. */
    static OverlayImage create(String id, int accentRgb) {
        byte[] pixels = new byte[WIDTH * HEIGHT * 4];
        int red = accentRgb >>> 16 & 0xff;
        int green = accentRgb >>> 8 & 0xff;
        int blue = accentRgb & 0xff;
        int variant = Math.floorMod(id.hashCode(), 4);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                float horizontal = x / (float) (WIDTH - 1);
                float vertical = y / (float) (HEIGHT - 1);
                float glow = motif(variant, horizontal, vertical);
                int offset = (y * WIDTH + x) * 4;
                pixels[offset] = component(5.0f + red * glow);
                pixels[offset + 1] = component(8.0f + green * glow);
                pixels[offset + 2] = component(16.0f + blue * glow);
                pixels[offset + 3] = (byte) 0xff;
            }
        }
        return OverlayImage.srgbRgba(WIDTH, HEIGHT, pixels);
    }

    /** Returns one soft geometric motif in the normalized thumbnail plane. */
    private static float motif(int variant, float x, float y) {
        float vignette = Math.max(0.0f, 1.0f - 0.72f * distance(x, y, 0.5f, 0.5f));
        float shape =
                switch (variant) {
                    case 0 -> Math.max(0.0f, 1.0f - distance(x, y, 0.5f, 0.52f) * 3.4f);
                    case 1 -> Math.max(0.0f, 1.0f - Math.abs(y - (0.25f + x * 0.55f)) * 7.0f);
                    case 2 -> Math.max(0.0f, 1.0f - Math.abs(distance(x, y, 0.5f, 0.52f) - 0.27f) * 10.0f);
                    default -> Math.max(0.0f, 1.0f - Math.max(Math.abs(x - 0.5f), Math.abs(y - 0.52f)) * 3.5f);
                };
        return Math.min(0.18f * vignette + 0.82f * shape, 1.0f);
    }

    /** Returns Euclidean distance with horizontal compensation for the wide image. */
    private static float distance(float x, float y, float centerX, float centerY) {
        float horizontal = (x - centerX) * 1.75f;
        float vertical = y - centerY;
        return (float) Math.sqrt(horizontal * horizontal + vertical * vertical);
    }

    /** Converts a clamped floating-point colour component to unsigned storage. */
    private static byte component(float value) {
        return (byte) Math.clamp(Math.round(value), 0, 255);
    }
}
