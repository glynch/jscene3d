/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.Objects;

/** Shared immutable-use screen image resource independent of 3D materials and textures. */
public final class OverlayImageResource implements AutoCloseable {
    private final OverlayImage image;
    private boolean closed;

    private OverlayImageResource(OverlayImage image) {
        this.image = Objects.requireNonNull(image, "image");
    }

    /**
     * Creates a resource owning the supplied immutable overlay image.
     *
     * @param image immutable image to own
     * @return owning resource
     */
    public static OverlayImageResource owning(OverlayImage image) {
        return new OverlayImageResource(image);
    }

    /**
     * Returns the immutable image while this resource is open.
     *
     * @return immutable overlay image
     */
    public OverlayImage image() {
        if (closed) {
            throw new IllegalStateException("overlay image resource is closed");
        }
        return image;
    }

    /**
     * Returns whether terminal resource closure has completed.
     *
     * @return {@code true} after closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Marks this immutable heap resource closed. */
    @Override
    public void close() {
        closed = true;
    }
}
