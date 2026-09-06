/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import java.util.Objects;

/** World-owned mesh-renderer component backed by one hidden JScene3D mesh. */
final class Object3dMeshRenderer implements MeshRenderer3d, ComponentLifecycleCallbacks {
    private final Object3dSpatialAdapter adapter;
    private final Entity owner;
    private final Mesh3dResource mesh;
    private final Material3dResource material;
    private final Mesh renderable;

    private boolean visible;
    private boolean active;
    private boolean closed;

    /** Creates an initially lifecycle-hidden mesh retaining shared resources. */
    Object3dMeshRenderer(
            Object3dSpatialAdapter adapter,
            Entity owner,
            Mesh3dResource mesh,
            Material3dResource material,
            boolean visible) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.mesh = Objects.requireNonNull(mesh, "mesh");
        this.material = Objects.requireNonNull(material, "material");
        renderable = new Mesh(mesh.geometry(), material.material());
        this.visible = visible;
        renderable.setVisible(false);
    }

    @Override
    public Mesh3dResource mesh() {
        requireOpen();
        return mesh;
    }

    @Override
    public Material3dResource material() {
        requireOpen();
        return material;
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
        adapter.release(this);
    }

    /** Returns the hidden backend mesh. */
    Mesh renderable() {
        return renderable;
    }

    /** Returns the owning live entity. */
    Entity owner() {
        return owner;
    }

    /** Applies local and entity lifecycle visibility to the hidden mesh. */
    private void synchronizeVisibility() {
        renderable.setVisible(active && visible);
    }

    /** Rejects state access after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("MeshRenderer3d is closed");
        }
    }
}
