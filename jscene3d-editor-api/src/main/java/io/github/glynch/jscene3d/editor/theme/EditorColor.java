/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import java.util.Locale;

/** Toolkit-independent sRGB color with an eight-bit alpha channel. */
public record EditorColor(int red, int green, int blue, int alpha) {
    /** Validates every channel. */
    public EditorColor {
        requireChannel(red, "red");
        requireChannel(green, "green");
        requireChannel(blue, "blue");
        requireChannel(alpha, "alpha");
    }

    /** Creates an opaque color. */
    public static EditorColor rgb(int red, int green, int blue) {
        return new EditorColor(red, green, blue, 255);
    }

    /** Parses {@code #RRGGBB} or {@code #RRGGBBAA}. */
    public static EditorColor parseHex(String value) {
        if (value == null || value.length() != 7 && value.length() != 9 || value.charAt(0) != '#') {
            throw new IllegalArgumentException("color must use #RRGGBB or #RRGGBBAA");
        }
        try {
            int red = Integer.parseInt(value.substring(1, 3), 16);
            int green = Integer.parseInt(value.substring(3, 5), 16);
            int blue = Integer.parseInt(value.substring(5, 7), 16);
            int alpha = value.length() == 9 ? Integer.parseInt(value.substring(7, 9), 16) : 255;
            return new EditorColor(red, green, blue, alpha);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("color must use #RRGGBB or #RRGGBBAA", exception);
        }
    }

    /** Returns {@code #RRGGBB} for opaque colors and {@code #RRGGBBAA} otherwise. */
    public String toHex() {
        return alpha == 255
                ? String.format(Locale.ROOT, "#%02x%02x%02x", red, green, blue)
                : String.format(Locale.ROOT, "#%02x%02x%02x%02x", red, green, blue, alpha);
    }

    private static void requireChannel(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be between 0 and 255");
        }
    }
}
