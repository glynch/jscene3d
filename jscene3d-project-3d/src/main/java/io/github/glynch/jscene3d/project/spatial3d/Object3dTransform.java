/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.runtime.Entity;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** World-owned transform component backed by one hidden JScene3D spatial object. */
final class Object3dTransform implements Transform3d {
    /** Adapter registration owner. */
    private final Object3dSpatialAdapter adapter;

    /** Live entity owning this component. */
    private final Entity owner;

    /** Hidden spatial object providing automatic matrix maintenance. */
    private final Object3D node;

    /** Terminal component state. */
    private boolean closed;

    /** Stores one complete adapter registration. */
    Object3dTransform(Object3dSpatialAdapter adapter, Entity owner, Object3D node) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.node = Objects.requireNonNull(node, "node");
    }

    @Override
    public Vector3fc position() {
        requireOpen();
        return node.position();
    }

    @Override
    public Quaternionfc orientation() {
        requireOpen();
        return node.quaternion();
    }

    @Override
    public Vector3fc scale() {
        requireOpen();
        return node.scale();
    }

    @Override
    public void setPosition(float x, float y, float z) {
        requireOpen();
        node.setPosition(x, y, z);
    }

    @Override
    public void setOrientation(float x, float y, float z, float w) {
        requireOpen();
        node.setQuaternion(x, y, z, w);
    }

    @Override
    public void setScale(float x, float y, float z) {
        requireOpen();
        node.setScale(x, y, z);
    }

    @Override
    public Matrix4fc localMatrix() {
        requireOpen();
        return node.matrix();
    }

    @Override
    public Matrix4fc worldMatrix() {
        requireOpen();
        return node.matrixWorld();
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

    /** Returns the entity used as the adapter's identity key. */
    Entity owner() {
        return owner;
    }

    /** Returns the hidden node used only inside this adapter implementation. */
    Object3D node() {
        return node;
    }

    /** Rejects component access after world-owned cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Transform3d is closed");
        }
    }
}
