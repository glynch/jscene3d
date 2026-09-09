/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.textures.Texture;
import java.util.Objects;

/** Shared immutable-use runtime texture resource owning its renderer-independent image and sampler description. */
public final class Texture3dResource implements AutoCloseable {
    private final Texture texture;
    private boolean closed;

    /** Takes ownership of one validated open texture. */
    private Texture3dResource(Texture texture) {
        this.texture = Objects.requireNonNull(texture, "texture");
        if (texture.isClosed()) {
            throw new IllegalArgumentException("texture must be open");
        }
    }

    /**
     * Takes exclusive ownership of an open texture.
     *
     * @param texture texture whose ownership transfers
     * @return new resource
     */
    public static Texture3dResource owning(Texture texture) {
        return new Texture3dResource(texture);
    }

    /**
     * Returns whether this resource and its owned texture are closed.
     *
     * @return {@code true} after terminal closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Closes the owned texture exactly once. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        texture.close();
    }

    /** Returns the internal texture while this resource is open. */
    Texture texture() {
        if (closed) {
            throw new IllegalStateException("Texture3dResource is closed");
        }
        return texture;
    }
}
