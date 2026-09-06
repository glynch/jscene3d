/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** World-owned directional-light component backed by one hidden JScene3D light. */
final class Object3dDirectionalLight implements DirectionalLight3d, ComponentLifecycleCallbacks {
    private final Object3dSpatialAdapter adapter;
    private final Entity owner;
    private final DirectionalLight light;
    private final Vector3f target;

    private boolean closed;

    /** Creates an initially lifecycle-hidden light. */
    Object3dDirectionalLight(
            Object3dSpatialAdapter adapter, Entity owner, Color color, float intensity, Vector3fc target) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.target = new Vector3f(Objects.requireNonNull(target, "target"));
        light = new DirectionalLight(Objects.requireNonNull(color, "color"), intensity);
        light.setPosition(0.0F, 0.0F, 0.0F);
        light.setTarget(this.target);
        light.setVisible(false);
    }

    @Override
    public Color color() {
        requireOpen();
        return light.color();
    }

    @Override
    public float intensity() {
        requireOpen();
        return light.intensity();
    }

    @Override
    public Vector3fc target() {
        requireOpen();
        return target;
    }

    @Override
    public void setColor(Color color) {
        requireOpen();
        light.setColor(Objects.requireNonNull(color, "color"));
    }

    @Override
    public void setIntensity(float intensity) {
        requireOpen();
        light.setIntensity(intensity);
    }

    @Override
    public void setTarget(float x, float y, float z) {
        requireOpen();
        light.setTarget(x, y, z);
        target.set(x, y, z);
    }

    @Override
    public void onActivated() {
        requireOpen();
        light.setVisible(true);
    }

    @Override
    public void onDeactivated() {
        requireOpen();
        light.setVisible(false);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        adapter.release(this);
    }

    /** Returns the hidden backend light. */
    DirectionalLight light() {
        return light;
    }

    /** Returns the owning live entity. */
    Entity owner() {
        return owner;
    }

    /** Rejects state access after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("DirectionalLight3d is closed");
        }
    }
}
