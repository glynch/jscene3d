/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import java.util.Objects;

/**
 * Immutable options used to create a window.
 *
 * <p>The defaults create a resizable, initially hidden 1280 by 720 window titled {@code
 * JScene3D}, with vertical synchronization enabled and framebuffer multisampling disabled.
 * Unsupported valid framebuffer sample-count requests are negotiated by the platform rather than
 * rejected by this value.
 */
public final class WindowOptions {
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;
    private static final String DEFAULT_TITLE = "JScene3D";
    private static final int DEFAULT_FRAMEBUFFER_SAMPLE_COUNT = 0;
    private static final WindowOptions DEFAULTS = new Builder().build();

    private final int width;
    private final int height;
    private final String title;
    private final VerticalSync verticalSync;
    private final int preferredFramebufferSampleCount;

    private WindowOptions(Builder builder) {
        width = builder.width;
        height = builder.height;
        title = builder.title;
        verticalSync = builder.verticalSync;
        preferredFramebufferSampleCount = builder.preferredFramebufferSampleCount;
    }

    /**
     * Returns the complete default options.
     *
     * @return the shared immutable default value
     */
    public static WindowOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Creates a builder initialized with the documented defaults.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the initial logical width.
     *
     * @return the positive width in screen coordinates
     */
    public int width() {
        return width;
    }

    /**
     * Returns the initial logical height.
     *
     * @return the positive height in screen coordinates
     */
    public int height() {
        return height;
    }

    /**
     * Returns the initial window title.
     *
     * @return the title, which may be empty
     */
    public String title() {
        return title;
    }

    /**
     * Returns the initial vertical-synchronization mode.
     *
     * @return the vertical-synchronization mode
     */
    public VerticalSync verticalSync() {
        return verticalSync;
    }

    /**
     * Returns the preferred sample count for default-framebuffer multisampling.
     *
     * <p>This is a soft creation request. A created window reports the actual platform result
     * through its {@code framebufferSampleCount()} method.
     *
     * @return zero when multisampling is disabled, otherwise the preferred positive sample count
     */
    public int preferredFramebufferSampleCount() {
        return preferredFramebufferSampleCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WindowOptions that)) {
            return false;
        }
        return width == that.width
                && height == that.height
                && preferredFramebufferSampleCount == that.preferredFramebufferSampleCount
                && title.equals(that.title)
                && verticalSync == that.verticalSync;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, title, verticalSync, preferredFramebufferSampleCount);
    }

    @Override
    public String toString() {
        return "WindowOptions[width="
                + width
                + ", height="
                + height
                + ", title="
                + title
                + ", verticalSync="
                + verticalSync
                + ", preferredFramebufferSampleCount="
                + preferredFramebufferSampleCount
                + ']';
    }

    /** Builds immutable {@link WindowOptions} values. */
    public static final class Builder {
        private int width = DEFAULT_WIDTH;
        private int height = DEFAULT_HEIGHT;
        private String title = DEFAULT_TITLE;
        private VerticalSync verticalSync = VerticalSync.ENABLED;
        private int preferredFramebufferSampleCount = DEFAULT_FRAMEBUFFER_SAMPLE_COUNT;

        private Builder() {}

        /**
         * Sets the initial logical size atomically.
         *
         * @param width the positive width in screen coordinates
         * @param height the positive height in screen coordinates
         * @return this builder
         * @throws IllegalArgumentException if either dimension is not positive
         */
        public Builder size(int width, int height) {
            requirePositive(width, "width");
            requirePositive(height, "height");
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets the initial title.
         *
         * @param title the title, which may be empty
         * @return this builder
         * @throws NullPointerException if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains a null character
         */
        public Builder title(String title) {
            Objects.requireNonNull(title, "title");
            if (title.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("title must not contain a null character");
            }
            this.title = title;
            return this;
        }

        /**
         * Sets the initial vertical-synchronization mode.
         *
         * @param verticalSync the vertical-synchronization mode
         * @return this builder
         * @throws NullPointerException if {@code verticalSync} is {@code null}
         */
        public Builder verticalSync(VerticalSync verticalSync) {
            this.verticalSync = Objects.requireNonNull(verticalSync, "verticalSync");
            return this;
        }

        /**
         * Sets the preferred sample count for default-framebuffer multisampling.
         *
         * <p>Zero disables multisampling. A positive value is a soft request that the platform may
         * satisfy with a different actual sample count.
         *
         * @param preferredFramebufferSampleCount zero or a positive preferred sample count
         * @return this builder
         * @throws IllegalArgumentException if the sample count is negative
         */
        public Builder preferredFramebufferSampleCount(int preferredFramebufferSampleCount) {
            if (preferredFramebufferSampleCount < 0) {
                throw new IllegalArgumentException(
                        "preferredFramebufferSampleCount must not be negative: " + preferredFramebufferSampleCount);
            }
            this.preferredFramebufferSampleCount = preferredFramebufferSampleCount;
            return this;
        }

        /**
         * Builds an immutable value from the current builder state.
         *
         * @return the new options value
         */
        public WindowOptions build() {
            return new WindowOptions(this);
        }

        private static void requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive: " + value);
            }
        }
    }
}
