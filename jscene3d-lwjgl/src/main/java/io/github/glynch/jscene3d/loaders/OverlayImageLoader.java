/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import io.github.glynch.jscene3d.render.OverlayImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/** Loads PNG and JPEG files into immutable full-color overlay images. */
public final class OverlayImageLoader {
    /** Prevents instantiation of this stateless loader. */
    private OverlayImageLoader() {
        throw new AssertionError("OverlayImageLoader cannot be instantiated");
    }

    /**
     * Loads a PNG or JPEG as a top-row-first sRGB RGBA8 overlay image.
     *
     * @param source PNG or JPEG path
     * @return immutable full-color overlay image
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws TextureLoadException if the file cannot be read, has an unsupported signature, or
     *     cannot be decoded
     */
    public static OverlayImage load(Path source) {
        return ImageDecoder.decode(source, OverlayImage::srgbRgba);
    }

    /**
     * Loads a PNG or JPEG classpath resource as a top-row-first sRGB RGBA8 overlay image.
     *
     * <p>A name beginning with {@code /} is resolved from the classpath root. Other names are
     * resolved relative to the anchor class's package, following {@link Class#getResourceAsStream}
     * semantics.
     *
     * @param anchor class whose loader and package resolve the resource
     * @param resourceName absolute or package-relative resource name
     * @return immutable full-color overlay image
     * @throws NullPointerException if either argument is {@code null}
     * @throws TextureLoadException if the resource is absent, unreadable, unsupported, or invalid
     */
    public static OverlayImage loadResource(Class<?> anchor, String resourceName) {
        Class<?> validAnchor = Objects.requireNonNull(anchor, "anchor");
        String validResourceName = Objects.requireNonNull(resourceName, "resourceName");
        Path diagnosticSource = Path.of(validResourceName);
        try (InputStream input = validAnchor.getResourceAsStream(validResourceName)) {
            if (input == null) {
                throw new TextureLoadException(
                        diagnosticSource, "Cannot find overlay image resource " + validResourceName);
            }
            return ImageDecoder.decode(diagnosticSource, input.readAllBytes(), OverlayImage::srgbRgba);
        } catch (IOException exception) {
            throw new TextureLoadException(
                    diagnosticSource, "Cannot read overlay image resource " + validResourceName, exception);
        }
    }
}
