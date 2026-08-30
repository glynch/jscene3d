/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Root scene node with renderer-independent scene settings. */
public final class Scene extends Object3D {
    private @Nullable Color background;

    /** Creates an empty scene with no background override. */
    public Scene() {
        super();
    }

    /**
     * Returns the optional solid background color.
     *
     * @return the background, or {@code null} when the renderer default applies
     */
    public @Nullable Color background() {
        return background;
    }

    /**
     * Sets the solid background color.
     *
     * @param background background color
     * @throws NullPointerException if {@code background} is {@code null}
     */
    public void setBackground(Color background) {
        this.background = Objects.requireNonNull(background, "background");
    }

    /** Clears the background so the renderer's default clear color applies. */
    public void clearBackground() {
        background = null;
    }
}
