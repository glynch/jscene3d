/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Immutable color stored in JScene3D's linear-sRGB working space. */
public final class Color {
    /** Linear representation of sRGB black. */
    public static final Color BLACK = srgb(0x000000);

    /** Linear representation of sRGB white. */
    public static final Color WHITE = srgb(0xffffff);

    /** Linear representation of sRGB red. */
    public static final Color RED = srgb(0xff0000);

    /** Linear representation of sRGB green. */
    public static final Color GREEN = srgb(0x00ff00);

    /** Linear representation of sRGB blue. */
    public static final Color BLUE = srgb(0x0000ff);

    /** Linear representation of sRGB yellow. */
    public static final Color YELLOW = srgb(0xffff00);

    /** Linear representation of sRGB cyan. */
    public static final Color CYAN = srgb(0x00ffff);

    /** Linear representation of sRGB magenta. */
    public static final Color MAGENTA = srgb(0xff00ff);

    /** Linear representation of sRGB gray {@code #808080}. */
    public static final Color GRAY = srgb(0x808080);

    // The uppercase constant and lowercase component names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    private final float red;

    // The uppercase constant and lowercase component names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    private final float green;

    // The uppercase constant and lowercase component names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    private final float blue;

    private Color(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Creates a color from channels already expressed in linear sRGB.
     *
     * @param red linear red channel in the inclusive range {@code [0, 1]}
     * @param green linear green channel in the inclusive range {@code [0, 1]}
     * @param blue linear blue channel in the inclusive range {@code [0, 1]}
     * @return the immutable color
     * @throws IllegalArgumentException if any channel is non-finite or outside {@code [0, 1]}
     */
    public static Color linear(float red, float green, float blue) {
        float validRed = Preconditions.requireInRange(red, 0.0f, 1.0f, "red");
        float validGreen = Preconditions.requireInRange(green, 0.0f, 1.0f, "green");
        float validBlue = Preconditions.requireInRange(blue, 0.0f, 1.0f, "blue");
        return new Color(validRed, validGreen, validBlue);
    }

    /**
     * Creates a color by converting normalized sRGB-encoded channels to linear sRGB.
     *
     * @param red sRGB-encoded red channel in the inclusive range {@code [0, 1]}
     * @param green sRGB-encoded green channel in the inclusive range {@code [0, 1]}
     * @param blue sRGB-encoded blue channel in the inclusive range {@code [0, 1]}
     * @return the immutable linear color
     * @throws IllegalArgumentException if any channel is non-finite or outside {@code [0, 1]}
     */
    public static Color srgb(float red, float green, float blue) {
        float validRed = Preconditions.requireInRange(red, 0.0f, 1.0f, "red");
        float validGreen = Preconditions.requireInRange(green, 0.0f, 1.0f, "green");
        float validBlue = Preconditions.requireInRange(blue, 0.0f, 1.0f, "blue");
        return new Color(
                ColorConversions.srgbToLinear(validRed),
                ColorConversions.srgbToLinear(validGreen),
                ColorConversions.srgbToLinear(validBlue));
    }

    /**
     * Creates a color from a packed sRGB {@code 0xRRGGBB} value.
     *
     * @param rgb packed sRGB value in the inclusive range {@code [0x000000, 0xffffff]}
     * @return the immutable linear color
     * @throws IllegalArgumentException if {@code rgb} contains bits outside {@code 0xRRGGBB}
     */
    public static Color srgb(int rgb) {
        int validRgb = Preconditions.requireInRange(rgb, 0x000000, 0xffffff, "rgb");
        float red = ((validRgb >>> 16) & 0xff) / 255.0f;
        float green = ((validRgb >>> 8) & 0xff) / 255.0f;
        float blue = (validRgb & 0xff) / 255.0f;
        return srgb(red, green, blue);
    }

    /**
     * Returns the linear red channel.
     *
     * @return red in the inclusive range {@code [0, 1]}
     */
    // The uppercase constant and lowercase accessor names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    public float red() {
        return red;
    }

    /**
     * Returns the linear green channel.
     *
     * @return green in the inclusive range {@code [0, 1]}
     */
    // The uppercase constant and lowercase accessor names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    public float green() {
        return green;
    }

    /**
     * Returns the linear blue channel.
     *
     * @return blue in the inclusive range {@code [0, 1]}
     */
    // The uppercase constant and lowercase accessor names are intentionally distinct Java conventions.
    @SuppressWarnings("java:S1845")
    public float blue() {
        return blue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Color color)) {
            return false;
        }
        return Float.compare(red, color.red) == 0
                && Float.compare(green, color.green) == 0
                && Float.compare(blue, color.blue) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(red);
        result = 31 * result + Float.hashCode(green);
        return 31 * result + Float.hashCode(blue);
    }

    @Override
    public String toString() {
        return "Color[red=" + red + ", green=" + green + ", blue=" + blue + ']';
    }
}
