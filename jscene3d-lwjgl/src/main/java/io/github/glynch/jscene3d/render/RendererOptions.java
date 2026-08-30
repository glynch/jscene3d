/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.Color;
import java.util.Objects;

/** Immutable options used to create a renderer. */
public final class RendererOptions {
    private static final RendererOptions DEFAULTS = new Builder().build();

    private final boolean automaticClear;
    private final Color clearColor;
    private final float clearAlpha;

    private RendererOptions(Builder builder) {
        automaticClear = builder.automaticClear;
        clearColor = builder.clearColor;
        clearAlpha = builder.clearAlpha;
    }

    /** Returns the shared default options. */
    public static RendererOptions defaults() {
        return DEFAULTS;
    }

    /** Creates a builder initialized with the documented defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns whether each frame clears its color and depth buffers automatically. */
    public boolean automaticClear() {
        return automaticClear;
    }

    /** Returns the default linear-sRGB clear color. */
    public Color clearColor() {
        return clearColor;
    }

    /** Returns the clear alpha in the inclusive range {@code [0, 1]}. */
    public float clearAlpha() {
        return clearAlpha;
    }

    /** Builds immutable {@link RendererOptions} values. */
    public static final class Builder {
        private boolean automaticClear = true;
        private Color clearColor = Color.BLACK;
        private float clearAlpha = 1.0f;

        private Builder() {}

        /** Selects whether every rendered frame clears color and depth automatically. */
        public Builder automaticClear(boolean automaticClear) {
            this.automaticClear = automaticClear;
            return this;
        }

        /** Sets the default linear-sRGB clear color. */
        public Builder clearColor(Color clearColor) {
            this.clearColor = Objects.requireNonNull(clearColor, "clearColor");
            return this;
        }

        /** Sets the clear alpha in the inclusive range {@code [0, 1]}. */
        public Builder clearAlpha(float clearAlpha) {
            this.clearAlpha = Preconditions.requireUnitInterval(clearAlpha, "clearAlpha");
            return this;
        }

        /** Builds the immutable options value. */
        public RendererOptions build() {
            return new RendererOptions(this);
        }
    }
}
