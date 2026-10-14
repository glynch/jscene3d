/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import java.util.Objects;

/** Shared world ownership and lifecycle for hidden engine camera objects. */
abstract class Object3dCameraComponent<C extends Camera> implements ComponentLifecycleCallbacks, AutoCloseable {
    private final Object3dSpatialAdapter adapter;
    private final Entity owner;
    private final C camera;
    private final boolean primary;
    private final String componentName;

    private boolean active;
    private boolean closed;

    /** Captures common camera ownership without attaching backend state to the authored entity model. */
    Object3dCameraComponent(
            Object3dSpatialAdapter adapter, Entity owner, C camera, boolean primary, String componentName) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.primary = primary;
        this.componentName = Objects.requireNonNull(componentName, "componentName");
    }

    /** Returns whether authored configuration selects this camera as primary. */
    public final boolean isPrimary() {
        requireOpen();
        return primary;
    }

    @Override
    public final void onActivated() {
        requireOpen();
        active = true;
    }

    @Override
    public final void onDeactivated() {
        requireOpen();
        active = false;
    }

    /** Returns whether terminal cleanup has occurred. */
    public final boolean isClosed() {
        return closed;
    }

    @Override
    public final void close() {
        if (closed) {
            return;
        }
        closed = true;
        active = false;
        adapter.release(this);
    }

    /** Returns the hidden backend camera. */
    final C camera() {
        return camera;
    }

    /** Returns the owning live entity. */
    final Entity owner() {
        return owner;
    }

    /** Returns whether entity lifecycle currently permits camera selection. */
    final boolean isActive() {
        return active && !closed;
    }

    /** Applies viewport-dependent projection state before rendering. */
    void prepareForRender(float viewportAspectRatio) {
        // Orthographic authored bounds do not depend on viewport aspect ratio.
    }

    /** Rejects projection access after terminal cleanup. */
    final void requireOpen() {
        if (closed) {
            throw new IllegalStateException(componentName + " is closed");
        }
    }
}
