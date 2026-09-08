/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.Billboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/** World-owned billboard-renderer component backed by one hidden JScene3D billboard. */
final class Object3dBillboardRenderer implements BillboardRenderer3d, ComponentLifecycleCallbacks {
    private final Object3dSpatialAdapter adapter;
    private final Entity owner;
    private final Material3dResource material;
    private final Billboard renderable;
    private final Vector2f size;
    private final Vector2f anchor;

    private boolean visible;
    private boolean active;
    private boolean closed;

    /** Creates an initially lifecycle-hidden billboard retaining shared resource identity. */
    Object3dBillboardRenderer(
            Object3dSpatialAdapter adapter,
            Entity owner,
            Material3dResource material,
            Vector2fc size,
            Vector2fc anchor,
            BillboardAlignment alignment,
            boolean visible) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.material = Objects.requireNonNull(material, "material");
        this.size = positive(size, "size");
        this.anchor = finite(anchor, "anchor");
        renderable = new Billboard(material.basicMaterial());
        renderable.setScale(this.size.x, this.size.y, 1.0F);
        renderable.setAnchor(this.anchor);
        renderable.setAlignment(Objects.requireNonNull(alignment, "alignment"));
        this.visible = visible;
        renderable.setVisible(false);
    }

    @Override
    public Material3dResource material() {
        requireOpen();
        return material;
    }

    @Override
    public Vector2fc size() {
        requireOpen();
        return size;
    }

    @Override
    public Vector2fc anchor() {
        requireOpen();
        return anchor;
    }

    @Override
    public BillboardAlignment alignment() {
        requireOpen();
        return renderable.alignment();
    }

    @Override
    public boolean isVisible() {
        requireOpen();
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        requireOpen();
        this.visible = visible;
        synchronizeVisibility();
    }

    @Override
    public void onActivated() {
        requireOpen();
        active = true;
        synchronizeVisibility();
    }

    @Override
    public void onDeactivated() {
        requireOpen();
        active = false;
        synchronizeVisibility();
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
        renderable.setVisible(false);
        try {
            adapter.release(this);
        } finally {
            renderable.close();
        }
    }

    /** Returns the hidden backend billboard. */
    Billboard renderable() {
        return renderable;
    }

    /** Returns the owning live entity. */
    Entity owner() {
        return owner;
    }

    /** Applies local and entity lifecycle visibility to the hidden billboard. */
    private void synchronizeVisibility() {
        renderable.setVisible(active && visible);
    }

    /** Copies one finite vector. */
    private static Vector2f finite(Vector2fc value, String name) {
        Vector2fc valid = Objects.requireNonNull(value, name);
        if (!Float.isFinite(valid.x()) || !Float.isFinite(valid.y())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector2f(valid);
    }

    /** Copies one positive finite vector. */
    private static Vector2f positive(Vector2fc value, String name) {
        Vector2f result = finite(value, name);
        if (result.x <= 0.0F || result.y <= 0.0F) {
            throw new IllegalArgumentException(name + " entries must be positive");
        }
        return result;
    }

    /** Rejects state access after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("BillboardRenderer3d is closed");
        }
    }
}
