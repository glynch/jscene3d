/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;

/**
 * Renderer-independent surface appearance and render-state description.
 *
 * <p>Materials are mutable, shareable, and not thread-safe. Their version changes only when an
 * upload-visible property changes. Closure is terminal and does not close meshes that share the
 * material, but those meshes cannot subsequently render with the closed material.
 */
public sealed class Material implements AutoCloseable permits BasicMaterial, LambertMaterial, ShaderMaterial {
    private boolean visible = true;
    private float opacity = 1.0f;
    private boolean transparent;
    private MaterialSide side;
    private boolean depthTestEnabled = true;
    private boolean depthWriteEnabled = true;
    private long version;
    private boolean closed;

    /** Creates an open material with front-face rendering selected. */
    protected Material() {
        side = MaterialSide.FRONT;
    }

    /**
     * Returns whether surfaces using this material participate in rendering.
     *
     * @return {@code true} by default
     * @throws IllegalStateException if this material is closed
     */
    public final boolean visible() {
        requireOpen();
        return visible;
    }

    /**
     * Changes whether surfaces using this material participate in rendering.
     *
     * @param visible whether the material is visible
     * @throws IllegalStateException if this material is closed
     */
    public final void setVisible(boolean visible) {
        requireOpen();
        if (this.visible != visible) {
            this.visible = visible;
            version++;
        }
    }

    /**
     * Returns the surface opacity.
     *
     * @return a value between zero and one, initially one
     * @throws IllegalStateException if this material is closed
     */
    public final float opacity() {
        requireOpen();
        return opacity;
    }

    /**
     * Changes the surface opacity without implicitly enabling transparency.
     *
     * @param opacity finite value between zero and one
     * @throws IllegalArgumentException if {@code opacity} is outside its valid range
     * @throws IllegalStateException if this material is closed
     */
    public final void setOpacity(float opacity) {
        requireOpen();
        float validOpacity = Preconditions.requireInRange(opacity, 0.0f, 1.0f, "opacity");
        if (this.opacity != validOpacity) {
            this.opacity = validOpacity;
            version++;
        }
    }

    /**
     * Returns whether opacity participates in transparent rendering.
     *
     * @return {@code false} by default
     * @throws IllegalStateException if this material is closed
     */
    public final boolean transparent() {
        requireOpen();
        return transparent;
    }

    /**
     * Changes whether opacity participates in transparent rendering.
     *
     * @param transparent whether transparent rendering is enabled
     * @throws IllegalStateException if this material is closed
     */
    public final void setTransparent(boolean transparent) {
        requireOpen();
        if (this.transparent != transparent) {
            this.transparent = transparent;
            version++;
        }
    }

    /**
     * Returns which triangle orientations are rendered.
     *
     * @return the selected material side, initially {@link MaterialSide#FRONT}
     * @throws IllegalStateException if this material is closed
     */
    public final MaterialSide side() {
        requireOpen();
        return side;
    }

    /**
     * Changes which triangle orientations are rendered.
     *
     * @param side selected material side
     * @throws NullPointerException if {@code side} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public final void setSide(MaterialSide side) {
        requireOpen();
        MaterialSide validSide = Objects.requireNonNull(side, "side");
        if (this.side != validSide) {
            this.side = validSide;
            version++;
        }
    }

    /**
     * Returns whether depth testing is enabled.
     *
     * @return {@code true} by default
     * @throws IllegalStateException if this material is closed
     */
    public final boolean depthTestEnabled() {
        requireOpen();
        return depthTestEnabled;
    }

    /**
     * Changes whether depth testing is enabled.
     *
     * @param enabled whether to test fragment depth
     * @throws IllegalStateException if this material is closed
     */
    public final void setDepthTestEnabled(boolean enabled) {
        requireOpen();
        if (depthTestEnabled != enabled) {
            depthTestEnabled = enabled;
            version++;
        }
    }

    /**
     * Returns whether rendered fragments write depth.
     *
     * @return {@code true} by default
     * @throws IllegalStateException if this material is closed
     */
    public final boolean depthWriteEnabled() {
        requireOpen();
        return depthWriteEnabled;
    }

    /**
     * Changes whether rendered fragments write depth.
     *
     * @param enabled whether to write fragment depth
     * @throws IllegalStateException if this material is closed
     */
    public final void setDepthWriteEnabled(boolean enabled) {
        requireOpen();
        if (depthWriteEnabled != enabled) {
            depthWriteEnabled = enabled;
            version++;
        }
    }

    /**
     * Returns the upload-visible material version.
     *
     * @return the version, initially zero
     * @throws IllegalStateException if this material is closed
     */
    public final long version() {
        requireOpen();
        return version;
    }

    /**
     * Returns whether terminal closure has occurred.
     *
     * @return {@code true} after the first call to {@link #close()}
     */
    public final boolean isClosed() {
        return closed;
    }

    /** Permanently closes this material. Repeated closure is a no-op. */
    @Override
    public final void close() {
        closed = true;
    }

    /**
     * Records an upload-visible change made by a concrete material.
     *
     * @throws IllegalStateException if this material is closed
     */
    protected final void markChanged() {
        requireOpen();
        version++;
    }

    /**
     * Requires this material to remain open.
     *
     * @throws IllegalStateException if this material is closed
     */
    protected final void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Material is closed");
        }
    }
}
