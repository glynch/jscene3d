/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/**
 * Renderer-independent surface appearance and render-state description.
 *
 * <p>Materials are mutable, shareable, and not thread-safe. Their version changes only when an
 * upload-visible property changes. Closure is terminal and does not close meshes that share the
 * material, but those meshes cannot subsequently render with the closed material.
 */
public abstract sealed class Material implements AutoCloseable
        permits BasicMaterial,
                LambertMaterial,
                LineBasicMaterial,
                NormalMaterial,
                PhongMaterial,
                ShaderMaterial,
                StandardMaterial {
    private static final float DEFAULT_ALPHA_CUTOFF = 0.5f;

    private boolean visible = true;
    private float opacity = 1.0f;
    private AlphaMode alphaMode;
    private float alphaCutoff;
    private MaterialSide side;
    private boolean depthTestEnabled = true;
    private boolean depthWriteEnabled = true;
    private DepthFunction depthFunction;
    private long version;
    private boolean closed;

    /** Creates an open material with front-face rendering selected. */
    protected Material() {
        alphaMode = AlphaMode.OPAQUE;
        alphaCutoff = DEFAULT_ALPHA_CUTOFF;
        side = MaterialSide.FRONT;
        depthFunction = DepthFunction.LESS_OR_EQUAL;
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
        return alphaMode() == AlphaMode.BLEND;
    }

    /**
     * Changes whether opacity participates in transparent rendering.
     *
     * @param transparent whether transparent rendering is enabled
     * @throws IllegalStateException if this material is closed
     */
    public final void setTransparent(boolean transparent) {
        setAlphaMode(transparent ? AlphaMode.BLEND : AlphaMode.OPAQUE);
    }

    /**
     * Returns how resolved fragment alpha participates in rendering.
     *
     * @return {@link AlphaMode#OPAQUE} by default
     * @throws IllegalStateException if this material is closed
     */
    public final AlphaMode alphaMode() {
        requireOpen();
        return alphaMode;
    }

    /**
     * Changes how resolved fragment alpha participates in rendering.
     *
     * @param alphaMode alpha treatment to apply
     * @throws NullPointerException if {@code alphaMode} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public final void setAlphaMode(AlphaMode alphaMode) {
        requireOpen();
        AlphaMode validAlphaMode = Objects.requireNonNull(alphaMode, "alphaMode");
        if (this.alphaMode != validAlphaMode) {
            this.alphaMode = validAlphaMode;
            version++;
        }
    }

    /**
     * Returns the alpha threshold used by masked rendering.
     *
     * @return value in the inclusive range {@code [0, 1]}, initially {@code 0.5}
     * @throws IllegalStateException if this material is closed
     */
    public final float alphaCutoff() {
        requireOpen();
        return alphaCutoff;
    }

    /**
     * Changes the alpha threshold used by {@link AlphaMode#MASK}.
     *
     * @param alphaCutoff finite value in the inclusive range {@code [0, 1]}
     * @throws IllegalArgumentException if the value is non-finite or outside its valid range
     * @throws IllegalStateException if this material is closed
     */
    public final void setAlphaCutoff(float alphaCutoff) {
        requireOpen();
        float validAlphaCutoff = Preconditions.requireInRange(alphaCutoff, 0.0f, 1.0f, "alphaCutoff");
        if (this.alphaCutoff != validAlphaCutoff) {
            this.alphaCutoff = validAlphaCutoff;
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
     * Returns the comparison used when depth testing is enabled.
     *
     * @return {@link DepthFunction#LESS_OR_EQUAL} by default
     * @throws IllegalStateException if this material is closed
     */
    public final DepthFunction depthFunction() {
        requireOpen();
        return depthFunction;
    }

    /**
     * Changes the comparison used when depth testing is enabled.
     *
     * @param depthFunction depth comparison to apply
     * @throws NullPointerException if {@code depthFunction} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public final void setDepthFunction(DepthFunction depthFunction) {
        requireOpen();
        DepthFunction validDepthFunction = Objects.requireNonNull(depthFunction, "depthFunction");
        if (this.depthFunction != validDepthFunction) {
            this.depthFunction = validDepthFunction;
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
