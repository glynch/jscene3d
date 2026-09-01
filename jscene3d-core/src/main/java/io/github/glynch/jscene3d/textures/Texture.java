/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * Renderer-independent RGBA8 image and sampler description.
 *
 * <p>A texture owns a defensive copy of its pixels and retains them until terminal closure so the
 * same description can be realized by multiple renderers. Pixels are row-major beginning with the
 * top row, matching conventional disk-image storage; the renderer resolves the texture-coordinate
 * orientation. Offset, repeat, rotation, and rotation-center values define a cached UV transform
 * without changing geometry data. Texture instances are mutable, application-owned, shareable,
 * and not thread-safe. They never contain an OpenGL identifier.
 */
public final class Texture implements AutoCloseable {
    private static final TexturePixelFormat PIXEL_FORMAT = TexturePixelFormat.RGBA8;

    private int width;
    private int height;
    private byte[] pixels;
    private TextureColorSpace colorSpace;
    private TextureFilter minificationFilter = TextureFilter.LINEAR_MIPMAP_LINEAR;
    private TextureFilter magnificationFilter = TextureFilter.LINEAR;
    private TextureWrap horizontalWrap = TextureWrap.CLAMP_TO_EDGE;
    private TextureWrap verticalWrap = TextureWrap.CLAMP_TO_EDGE;
    private TextureCoordinateOrigin coordinateOrigin = TextureCoordinateOrigin.BOTTOM_LEFT;
    private MipmapMode mipmapMode = MipmapMode.GENERATE;
    private final Matrix3f transformMatrix = new Matrix3f();
    private float offsetU;
    private float offsetV;
    private float repeatU = 1.0f;
    private float repeatV = 1.0f;
    private float rotation;
    private float centerU;
    private float centerV;
    private long version;
    private long imageVersion;
    private long samplerVersion;
    private long transformVersion;
    private boolean closed;

    /** Creates a texture after validating and copying its initial image. */
    private Texture(int width, int height, byte[] pixels, TextureColorSpace colorSpace) {
        this.colorSpace = colorSpace;
        this.width = Preconditions.requirePositive(width, "width");
        this.height = Preconditions.requirePositive(height, "height");
        this.pixels = copyAndValidatePixels(this.width, this.height, pixels);
    }

    /** Creates a texture after validating and copying remaining buffer bytes. */
    private Texture(int width, int height, ByteBuffer pixels, TextureColorSpace colorSpace) {
        this.colorSpace = colorSpace;
        this.width = Preconditions.requirePositive(width, "width");
        this.height = Preconditions.requirePositive(height, "height");
        this.pixels = copyAndValidatePixels(this.width, this.height, pixels);
    }

    /**
     * Creates an sRGB base-color texture from RGBA8 pixels.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first RGBA8 data, defensively copied
     * @return new application-owned texture
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions or pixel-array length are invalid
     */
    public static Texture baseColor(int width, int height, byte[] pixels) {
        return new Texture(width, height, pixels, TextureColorSpace.SRGB);
    }

    /**
     * Creates an sRGB base-color texture from the remaining RGBA8 buffer bytes.
     *
     * <p>The buffer's position and limit are not changed.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first RGBA8 data, defensively copied
     * @return new application-owned texture
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions or remaining pixel count are invalid
     */
    public static Texture baseColor(int width, int height, ByteBuffer pixels) {
        return new Texture(width, height, pixels, TextureColorSpace.SRGB);
    }

    /**
     * Creates a linear data texture from RGBA8 pixels.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first RGBA8 data, defensively copied
     * @return new application-owned texture
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions or pixel-array length are invalid
     */
    public static Texture data(int width, int height, byte[] pixels) {
        return new Texture(width, height, pixels, TextureColorSpace.LINEAR);
    }

    /**
     * Creates a linear data texture from the remaining RGBA8 buffer bytes.
     *
     * <p>The buffer's position and limit are not changed.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first RGBA8 data, defensively copied
     * @return new application-owned texture
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions or remaining pixel count are invalid
     */
    public static Texture data(int width, int height, ByteBuffer pixels) {
        return new Texture(width, height, pixels, TextureColorSpace.LINEAR);
    }

    /**
     * Returns the image width in pixels.
     *
     * @return positive image width
     * @throws IllegalStateException if this texture is closed
     */
    public int width() {
        requireOpen();
        return width;
    }

    /**
     * Returns the image height in pixels.
     *
     * @return positive image height
     * @throws IllegalStateException if this texture is closed
     */
    public int height() {
        requireOpen();
        return height;
    }

    /**
     * Returns the fixed pixel format.
     *
     * @return {@link TexturePixelFormat#RGBA8}
     * @throws IllegalStateException if this texture is closed
     */
    public TexturePixelFormat pixelFormat() {
        requireOpen();
        return PIXEL_FORMAT;
    }

    /**
     * Returns the number of bytes in the retained base image.
     *
     * @return {@code width * height * 4}
     * @throws IllegalStateException if this texture is closed
     */
    public int pixelByteCount() {
        requireOpen();
        return pixels.length;
    }

    /**
     * Copies the complete top-row-first image into a destination buffer.
     *
     * <p>The copy begins at the current position and advances it by {@link #pixelByteCount()}.
     *
     * @param destination writable buffer with sufficient remaining capacity
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalArgumentException if the buffer has insufficient remaining capacity
     * @throws IllegalStateException if this texture is closed
     */
    public void copyPixelsTo(ByteBuffer destination) {
        requireOpen();
        ByteBuffer validDestination = Objects.requireNonNull(destination, "destination");
        if (validDestination.remaining() < pixels.length) {
            throw new IllegalArgumentException("destination requires at least " + pixels.length + " remaining bytes: "
                    + validDestination.remaining());
        }
        validDestination.put(pixels);
    }

    /**
     * Replaces the base image after defensively copying it.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels top-row-first RGBA8 data
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if dimensions or pixel-array length are invalid
     * @throws IllegalStateException if this texture is closed
     */
    public void setImage(int width, int height, byte[] pixels) {
        requireOpen();
        int validWidth = Preconditions.requirePositive(width, "width");
        int validHeight = Preconditions.requirePositive(height, "height");
        byte[] validPixels = copyAndValidatePixels(validWidth, validHeight, pixels);
        this.width = validWidth;
        this.height = validHeight;
        this.pixels = validPixels;
        markImageChanged();
    }

    /**
     * Returns the interpretation of the image's color channels.
     *
     * @return current color-space interpretation
     * @throws IllegalStateException if this texture is closed
     */
    public TextureColorSpace colorSpace() {
        requireOpen();
        return colorSpace;
    }

    /**
     * Changes the image color-space interpretation.
     *
     * @param colorSpace new color-space interpretation
     * @throws NullPointerException if {@code colorSpace} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public void setColorSpace(TextureColorSpace colorSpace) {
        requireOpen();
        TextureColorSpace validColorSpace = Objects.requireNonNull(colorSpace, "colorSpace");
        if (this.colorSpace != validColorSpace) {
            this.colorSpace = validColorSpace;
            markImageChanged();
        }
    }

    /**
     * Returns the minification filter.
     *
     * @return current minification filter
     * @throws IllegalStateException if this texture is closed
     */
    public TextureFilter minificationFilter() {
        requireOpen();
        return minificationFilter;
    }

    /**
     * Changes the minification filter.
     *
     * @param filter new minification filter
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IllegalArgumentException if it requires mipmaps while mipmaps are disabled
     * @throws IllegalStateException if this texture is closed
     */
    public void setMinificationFilter(TextureFilter filter) {
        requireOpen();
        TextureFilter validFilter = Objects.requireNonNull(filter, "filter");
        requireCompatible(validFilter, mipmapMode);
        if (minificationFilter != validFilter) {
            minificationFilter = validFilter;
            markSamplerChanged();
        }
    }

    /**
     * Returns the magnification filter.
     *
     * @return current non-mipmap magnification filter
     * @throws IllegalStateException if this texture is closed
     */
    public TextureFilter magnificationFilter() {
        requireOpen();
        return magnificationFilter;
    }

    /**
     * Changes the magnification filter.
     *
     * @param filter {@link TextureFilter#NEAREST} or {@link TextureFilter#LINEAR}
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IllegalArgumentException if {@code filter} uses mipmaps
     * @throws IllegalStateException if this texture is closed
     */
    public void setMagnificationFilter(TextureFilter filter) {
        requireOpen();
        TextureFilter validFilter = Objects.requireNonNull(filter, "filter");
        if (validFilter.usesMipmaps()) {
            throw new IllegalArgumentException("magnificationFilter cannot use mipmaps: " + validFilter);
        }
        if (magnificationFilter != validFilter) {
            magnificationFilter = validFilter;
            markSamplerChanged();
        }
    }

    /**
     * Returns the horizontal texture-coordinate wrap mode.
     *
     * @return current horizontal wrap mode
     * @throws IllegalStateException if this texture is closed
     */
    public TextureWrap horizontalWrap() {
        requireOpen();
        return horizontalWrap;
    }

    /**
     * Changes the horizontal texture-coordinate wrap mode.
     *
     * @param wrap new horizontal wrap mode
     * @throws NullPointerException if {@code wrap} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public void setHorizontalWrap(TextureWrap wrap) {
        requireOpen();
        TextureWrap validWrap = Objects.requireNonNull(wrap, "wrap");
        if (horizontalWrap != validWrap) {
            horizontalWrap = validWrap;
            markSamplerChanged();
        }
    }

    /**
     * Returns the vertical texture-coordinate wrap mode.
     *
     * @return current vertical wrap mode
     * @throws IllegalStateException if this texture is closed
     */
    public TextureWrap verticalWrap() {
        requireOpen();
        return verticalWrap;
    }

    /**
     * Changes the vertical texture-coordinate wrap mode.
     *
     * @param wrap new vertical wrap mode
     * @throws NullPointerException if {@code wrap} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public void setVerticalWrap(TextureWrap wrap) {
        requireOpen();
        TextureWrap validWrap = Objects.requireNonNull(wrap, "wrap");
        if (verticalWrap != validWrap) {
            verticalWrap = validWrap;
            markSamplerChanged();
        }
    }

    /**
     * Returns whether renderers generate mipmaps.
     *
     * @return current mipmap policy
     * @throws IllegalStateException if this texture is closed
     */
    public MipmapMode mipmapMode() {
        requireOpen();
        return mipmapMode;
    }

    /**
     * Changes whether renderers generate mipmaps.
     *
     * @param mode new mipmap policy
     * @throws NullPointerException if {@code mode} is {@code null}
     * @throws IllegalArgumentException if disabling mipmaps conflicts with the minification filter
     * @throws IllegalStateException if this texture is closed
     */
    public void setMipmapMode(MipmapMode mode) {
        requireOpen();
        MipmapMode validMode = Objects.requireNonNull(mode, "mode");
        requireCompatible(minificationFilter, validMode);
        if (mipmapMode != validMode) {
            mipmapMode = validMode;
            markSamplerChanged();
        }
    }

    /**
     * Returns the horizontal texture-coordinate offset.
     *
     * @return horizontal offset, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public float offsetU() {
        requireOpen();
        return offsetU;
    }

    /**
     * Returns the vertical texture-coordinate offset.
     *
     * @return vertical offset, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public float offsetV() {
        requireOpen();
        return offsetV;
    }

    /**
     * Copies the current texture-coordinate offset into caller-owned storage.
     *
     * @param destination vector receiving the horizontal and vertical offsets
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public Vector2f offset(Vector2f destination) {
        requireOpen();
        return Objects.requireNonNull(destination, "destination").set(offsetU, offsetV);
    }

    /**
     * Sets the texture-coordinate offset.
     *
     * @param u horizontal offset in texture-coordinate units
     * @param v vertical offset in texture-coordinate units
     * @throws IllegalArgumentException if either value is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setOffset(float u, float v) {
        requireOpen();
        float validU = Preconditions.requireFinite(u, "u");
        float validV = Preconditions.requireFinite(v, "v");
        if (offsetU != validU || offsetV != validV) {
            offsetU = validU;
            offsetV = validV;
            updateTransformMatrix();
            markTransformChanged();
        }
    }

    /**
     * Copies an existing value into the texture-coordinate offset.
     *
     * @param offset horizontal and vertical offsets
     * @throws NullPointerException if {@code offset} is {@code null}
     * @throws IllegalArgumentException if either component is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setOffset(Vector2fc offset) {
        Vector2fc validOffset = Preconditions.requireFinite(offset, "offset");
        setOffset(validOffset.x(), validOffset.y());
    }

    /**
     * Returns the horizontal texture-coordinate repeat factor.
     *
     * @return horizontal repeat factor, initially one
     * @throws IllegalStateException if this texture is closed
     */
    public float repeatU() {
        requireOpen();
        return repeatU;
    }

    /**
     * Returns the vertical texture-coordinate repeat factor.
     *
     * @return vertical repeat factor, initially one
     * @throws IllegalStateException if this texture is closed
     */
    public float repeatV() {
        requireOpen();
        return repeatV;
    }

    /**
     * Copies the current texture-coordinate repeat factors into caller-owned storage.
     *
     * @param destination vector receiving the horizontal and vertical repeat factors
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public Vector2f repeat(Vector2f destination) {
        requireOpen();
        return Objects.requireNonNull(destination, "destination").set(repeatU, repeatV);
    }

    /**
     * Sets the texture-coordinate repeat factors.
     *
     * <p>Values beyond one tile only repeat when the corresponding wrap mode is {@link
     * TextureWrap#REPEAT} or {@link TextureWrap#MIRRORED_REPEAT}. Negative and zero factors are
     * valid.
     *
     * @param u horizontal repeat factor
     * @param v vertical repeat factor
     * @throws IllegalArgumentException if either value is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setRepeat(float u, float v) {
        requireOpen();
        float validU = Preconditions.requireFinite(u, "u");
        float validV = Preconditions.requireFinite(v, "v");
        if (repeatU != validU || repeatV != validV) {
            repeatU = validU;
            repeatV = validV;
            updateTransformMatrix();
            markTransformChanged();
        }
    }

    /**
     * Copies an existing value into the texture-coordinate repeat factors.
     *
     * @param repeat horizontal and vertical repeat factors
     * @throws NullPointerException if {@code repeat} is {@code null}
     * @throws IllegalArgumentException if either component is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setRepeat(Vector2fc repeat) {
        Vector2fc validRepeat = Preconditions.requireFinite(repeat, "repeat");
        setRepeat(validRepeat.x(), validRepeat.y());
    }

    /**
     * Returns the texture-coordinate rotation in radians.
     *
     * @return counter-clockwise rotation, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public float rotation() {
        requireOpen();
        return rotation;
    }

    /**
     * Sets the counter-clockwise texture-coordinate rotation in radians.
     *
     * @param rotation rotation in radians
     * @throws IllegalArgumentException if {@code rotation} is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setRotation(float rotation) {
        requireOpen();
        float validRotation = Preconditions.requireFinite(rotation, "rotation");
        if (this.rotation != validRotation) {
            this.rotation = validRotation;
            updateTransformMatrix();
            markTransformChanged();
        }
    }

    /**
     * Returns the horizontal coordinate of the rotation center.
     *
     * @return horizontal rotation center, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public float centerU() {
        requireOpen();
        return centerU;
    }

    /**
     * Returns the vertical coordinate of the rotation center.
     *
     * @return vertical rotation center, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public float centerV() {
        requireOpen();
        return centerV;
    }

    /**
     * Copies the current rotation center into caller-owned storage.
     *
     * @param destination vector receiving the horizontal and vertical center coordinates
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public Vector2f center(Vector2f destination) {
        requireOpen();
        return Objects.requireNonNull(destination, "destination").set(centerU, centerV);
    }

    /**
     * Sets the center around which texture coordinates rotate.
     *
     * @param u horizontal center coordinate
     * @param v vertical center coordinate
     * @throws IllegalArgumentException if either value is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setCenter(float u, float v) {
        requireOpen();
        float validU = Preconditions.requireFinite(u, "u");
        float validV = Preconditions.requireFinite(v, "v");
        if (centerU != validU || centerV != validV) {
            centerU = validU;
            centerV = validV;
            updateTransformMatrix();
            markTransformChanged();
        }
    }

    /**
     * Copies an existing value into the texture-coordinate rotation center.
     *
     * @param center horizontal and vertical center coordinates
     * @throws NullPointerException if {@code center} is {@code null}
     * @throws IllegalArgumentException if either component is not finite
     * @throws IllegalStateException if this texture is closed
     */
    public void setCenter(Vector2fc center) {
        Vector2fc validCenter = Preconditions.requireFinite(center, "center");
        setCenter(validCenter.x(), validCenter.y());
    }

    /**
     * Returns the image corner represented by texture coordinate {@code (0, 0)}.
     *
     * @return current coordinate origin, initially {@link TextureCoordinateOrigin#BOTTOM_LEFT}
     * @throws IllegalStateException if this texture is closed
     */
    public TextureCoordinateOrigin coordinateOrigin() {
        requireOpen();
        return coordinateOrigin;
    }

    /**
     * Changes the image corner represented by texture coordinate {@code (0, 0)}.
     *
     * <p>This changes only built-in material sampling and does not upload the image again. Custom
     * shader materials remain responsible for applying their chosen coordinate convention.
     *
     * @param coordinateOrigin new coordinate origin
     * @throws NullPointerException if {@code coordinateOrigin} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public void setCoordinateOrigin(TextureCoordinateOrigin coordinateOrigin) {
        requireOpen();
        TextureCoordinateOrigin validOrigin = Objects.requireNonNull(coordinateOrigin, "coordinateOrigin");
        if (this.coordinateOrigin != validOrigin) {
            this.coordinateOrigin = validOrigin;
            markTransformChanged();
        }
    }

    /**
     * Copies the cached homogeneous texture-coordinate transform into caller-owned storage.
     *
     * <p>The matrix applies repeat and counter-clockwise rotation around the configured center,
     * followed by offset. The renderer applies its image-orientation conversion separately.
     *
     * @param destination matrix receiving the current transform
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if this texture is closed
     */
    public Matrix3f transformMatrix(Matrix3f destination) {
        requireOpen();
        return Objects.requireNonNull(destination, "destination").set(transformMatrix);
    }

    /**
     * Returns the version of all image, sampler, and texture-coordinate transform changes.
     *
     * @return monotonically increasing overall version, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public long version() {
        requireOpen();
        return version;
    }

    /**
     * Returns the version of changes that require image upload.
     *
     * @return monotonically increasing image version, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public long imageVersion() {
        requireOpen();
        return imageVersion;
    }

    /**
     * Returns the version of sampler-state changes.
     *
     * @return monotonically increasing sampler version, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public long samplerVersion() {
        requireOpen();
        return samplerVersion;
    }

    /**
     * Returns the version of texture-coordinate transform changes.
     *
     * @return monotonically increasing transform version, initially zero
     * @throws IllegalStateException if this texture is closed
     */
    public long transformVersion() {
        requireOpen();
        return transformVersion;
    }

    /**
     * Returns whether terminal closure has occurred.
     *
     * @return {@code true} after the first call to {@link #close()}
     */
    public boolean isClosed() {
        return closed;
    }

    /** Permanently closes this texture and releases its retained CPU pixels. Repeated closure is a no-op. */
    @Override
    public void close() {
        if (!closed) {
            pixels = new byte[0];
            closed = true;
        }
    }

    /** Validates the required RGBA8 image size and returns a defensive copy. */
    private static byte[] copyAndValidatePixels(int width, int height, byte[] pixels) {
        byte[] validPixels = Objects.requireNonNull(pixels, "pixels");
        int expectedLength = Preconditions.requireArrayLength(
                (long) width * height, TexturePixelFormat.RGBA8.bytesPerPixel(), "pixels");
        if (validPixels.length != expectedLength) {
            throw new IllegalArgumentException("pixels length must be " + expectedLength + " for " + width + "x"
                    + height + " RGBA8: " + validPixels.length);
        }
        return Arrays.copyOf(validPixels, validPixels.length);
    }

    /** Validates remaining RGBA8 buffer bytes and returns a defensive Java-array copy. */
    private static byte[] copyAndValidatePixels(int width, int height, ByteBuffer pixels) {
        ByteBuffer validPixels = Objects.requireNonNull(pixels, "pixels");
        int expectedLength = Preconditions.requireArrayLength(
                (long) width * height, TexturePixelFormat.RGBA8.bytesPerPixel(), "pixels");
        if (validPixels.remaining() != expectedLength) {
            throw new IllegalArgumentException("pixels must have " + expectedLength + " remaining bytes for " + width
                    + "x" + height + " RGBA8: " + validPixels.remaining());
        }
        byte[] copy = new byte[expectedLength];
        validPixels.duplicate().get(copy);
        return copy;
    }

    /** Rejects a mipmap filter when mipmap generation is disabled. */
    private static void requireCompatible(TextureFilter filter, MipmapMode mode) {
        if (mode == MipmapMode.NONE && filter.usesMipmaps()) {
            throw new IllegalArgumentException("minificationFilter " + filter + " requires mipmaps");
        }
    }

    /** Records an image-visible change. */
    private void markImageChanged() {
        imageVersion++;
        version++;
    }

    /** Records a sampler-visible change. */
    private void markSamplerChanged() {
        samplerVersion++;
        version++;
    }

    /** Recomputes the cached homogeneous UV transform without allocating. */
    private void updateTransformMatrix() {
        float cosine = (float) Math.cos(rotation);
        float sine = (float) Math.sin(rotation);
        transformMatrix.set(
                repeatU * cosine,
                -repeatV * sine,
                0.0f,
                repeatU * sine,
                repeatV * cosine,
                0.0f,
                -repeatU * (cosine * centerU + sine * centerV) + centerU + offsetU,
                -repeatV * (-sine * centerU + cosine * centerV) + centerV + offsetV,
                1.0f);
    }

    /** Records a texture-coordinate transform change. */
    private void markTransformChanged() {
        transformVersion++;
        version++;
    }

    /** Rejects access after terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Texture is closed");
        }
    }
}
