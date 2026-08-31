/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import io.github.glynch.jscene3d.textures.Texture;
import java.nio.file.Path;

/**
 * Loads officially supported disk images into renderer-independent texture descriptions.
 *
 * <p>Loading is synchronous on the calling thread; this component creates no background threads.
 */
public final class TextureLoader {
    /** Prevents instantiation of this stateless loader. */
    private TextureLoader() {
        throw new AssertionError("TextureLoader cannot be instantiated");
    }

    /**
     * Loads a PNG or JPEG as an sRGB base-color texture.
     *
     * <p>STB decoding is completed and its native memory is freed before this method returns. The
     * returned texture owns one Java copy in top-row-first RGBA8 order.
     *
     * @param source PNG or JPEG path
     * @return new application-owned base-color texture
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws TextureLoadException if the file cannot be read, has an unsupported signature, or
     *     cannot be decoded
     */
    public static Texture load(Path source) {
        return ImageDecoder.decode(source, Texture::baseColor);
    }
}
