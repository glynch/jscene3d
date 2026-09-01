/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Renderer-independent linear HDR equirectangular environment image.
 *
 * <p>An environment map owns one defensive copy of top-row-first RGB floating-point pixels. It is
 * application-owned, shareable between scenes and renderers, immutable while open, and not
 * thread-safe. Renderer-specific irradiance and reflection maps are derived lazily and remain
 * internal to each renderer.
 */
public final class EnvironmentMap implements AutoCloseable {
    private static final int COMPONENT_COUNT = 3;

    private final int width;
    private final int height;

    private float[] pixels;
    private boolean closed;

    /** Validates dimensions and retains a defensive RGB copy. */
    private EnvironmentMap(int width, int height, float[] pixels) {
        this.width = Preconditions.requirePositive(width, "width");
        this.height = Preconditions.requirePositive(height, "height");
        this.pixels = copyAndValidatePixels(this.width, this.height, pixels);
    }

    /** Validates dimensions and retains a copy of remaining RGB components. */
    private EnvironmentMap(int width, int height, FloatBuffer pixels) {
        this.width = Preconditions.requirePositive(width, "width");
        this.height = Preconditions.requirePositive(height, "height");
        this.pixels = copyAndValidatePixels(this.width, this.height, pixels);
    }

    /**
     * Creates a linear HDR equirectangular environment from RGB pixels.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first linear RGB values, defensively copied
     * @return new application-owned environment map
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions, pixel count, or a component is invalid
     */
    public static EnvironmentMap equirectangular(int width, int height, float[] pixels) {
        return new EnvironmentMap(width, height, pixels);
    }

    /**
     * Creates a linear HDR equirectangular environment from remaining RGB buffer components.
     *
     * <p>The buffer's position and limit are not changed.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first linear RGB values, defensively copied
     * @return new application-owned environment map
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions, remaining pixel count, or a component is
     *     invalid
     */
    public static EnvironmentMap equirectangular(int width, int height, FloatBuffer pixels) {
        return new EnvironmentMap(width, height, pixels);
    }

    /**
     * Returns the equirectangular image width.
     *
     * @return positive image width
     * @throws IllegalStateException if this environment map is closed
     */
    public int width() {
        requireOpen();
        return width;
    }

    /**
     * Returns the equirectangular image height.
     *
     * @return positive image height
     * @throws IllegalStateException if this environment map is closed
     */
    public int height() {
        requireOpen();
        return height;
    }

    /**
     * Returns the retained RGB component count.
     *
     * @return {@code width * height * 3}
     * @throws IllegalStateException if this environment map is closed
     */
    public int pixelComponentCount() {
        requireOpen();
        return pixels.length;
    }

    /**
     * Copies all retained RGB components into a destination buffer.
     *
     * <p>The copy begins at the current position and advances it by
     * {@link #pixelComponentCount()}.
     *
     * @param destination writable buffer with sufficient remaining capacity
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalArgumentException if the buffer has insufficient space
     * @throws IllegalStateException if this environment map is closed
     */
    public void copyPixelsTo(FloatBuffer destination) {
        requireOpen();
        FloatBuffer validDestination = Objects.requireNonNull(destination, "destination");
        if (validDestination.remaining() < pixels.length) {
            throw new IllegalArgumentException("destination requires at least "
                    + pixels.length
                    + " remaining floats: "
                    + validDestination.remaining());
        }
        validDestination.put(pixels);
    }

    /**
     * Returns whether terminal closure has completed.
     *
     * @return {@code true} after closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases the retained CPU image terminally without affecting renderer-owned realizations. */
    @Override
    public void close() {
        if (!closed) {
            pixels = new float[0];
            closed = true;
        }
    }

    /** Copies and validates an exactly sized finite, non-negative RGB image. */
    private static float[] copyAndValidatePixels(int width, int height, float[] pixels) {
        float[] copy = Arrays.copyOf(Objects.requireNonNull(pixels, "pixels"), pixels.length);
        long requiredLength = (long) width * height * COMPONENT_COUNT;
        if (requiredLength > Integer.MAX_VALUE || copy.length != requiredLength) {
            throw new IllegalArgumentException(
                    "pixels must contain width * height * 3 components: " + copy.length + " != " + requiredLength);
        }
        for (int index = 0; index < copy.length; index++) {
            float component = copy[index];
            if (!Float.isFinite(component) || component < 0.0f) {
                throw new IllegalArgumentException(
                        "pixels[" + index + "] must be finite and non-negative: " + component);
            }
        }
        return copy;
    }

    /** Copies and validates an exactly sized RGB buffer without changing its position. */
    private static float[] copyAndValidatePixels(int width, int height, FloatBuffer pixels) {
        FloatBuffer source = Objects.requireNonNull(pixels, "pixels").duplicate();
        long requiredLength = (long) width * height * COMPONENT_COUNT;
        if (requiredLength > Integer.MAX_VALUE || source.remaining() != requiredLength) {
            throw new IllegalArgumentException("pixels must contain width * height * 3 remaining components: "
                    + source.remaining()
                    + " != "
                    + requiredLength);
        }
        float[] copy = new float[(int) requiredLength];
        source.get(copy);
        for (int index = 0; index < copy.length; index++) {
            float component = copy[index];
            if (!Float.isFinite(component) || component < 0.0f) {
                throw new IllegalArgumentException(
                        "pixels[" + index + "] must be finite and non-negative: " + component);
            }
        }
        return copy;
    }

    /** Requires this application-owned description to remain open. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("EnvironmentMap is closed");
        }
    }
}
