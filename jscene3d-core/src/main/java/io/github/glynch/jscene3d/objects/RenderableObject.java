/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Base scene object for geometry draws with optional main-pass lifecycle callbacks. */
public abstract class RenderableObject extends Object3D {
    private @Nullable RenderCallback beforeRenderCallback;
    private @Nullable RenderCallback afterRenderCallback;

    /** Creates a visible renderable object with no configured callbacks. */
    protected RenderableObject() {
        // Object3D initializes scene state; callback absence requires no additional initialization.
    }

    /**
     * Returns the callback invoked immediately before each selected main-pass draw.
     *
     * @return configured callback, or an empty value
     */
    public final Optional<RenderCallback> beforeRenderCallback() {
        return Optional.ofNullable(beforeRenderCallback);
    }

    /**
     * Replaces the callback invoked immediately before each selected main-pass draw.
     *
     * <p>The callback runs after visibility, culling, and render ordering have been resolved.
     * Material and shader-uniform changes can affect the selected draw. Changes to hierarchy,
     * visibility, transforms, geometry binding, material binding, or render order take effect when
     * a later frame builds its render list.
     *
     * @param callback callback to retain
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public final void setBeforeRenderCallback(RenderCallback callback) {
        beforeRenderCallback = Objects.requireNonNull(callback, "callback");
    }

    /** Removes the main-pass callback without invoking it. */
    public final void clearBeforeRenderCallback() {
        beforeRenderCallback = null;
    }

    /**
     * Returns the callback invoked immediately after each successful main-pass draw.
     *
     * @return configured callback, or an empty value
     */
    public final Optional<RenderCallback> afterRenderCallback() {
        return Optional.ofNullable(afterRenderCallback);
    }

    /**
     * Replaces the callback invoked immediately after each successful main-pass draw.
     *
     * @param callback callback to retain
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public final void setAfterRenderCallback(RenderCallback callback) {
        afterRenderCallback = Objects.requireNonNull(callback, "callback");
    }

    /** Removes the main-pass callback without invoking it. */
    public final void clearAfterRenderCallback() {
        afterRenderCallback = null;
    }
}
