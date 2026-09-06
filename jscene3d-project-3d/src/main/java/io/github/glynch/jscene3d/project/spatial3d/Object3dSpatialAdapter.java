/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Standard in-memory adapter mapping live entity transforms onto an internal JScene3D object hierarchy. */
@SuppressWarnings("ReferenceEquality")
final class Object3dSpatialAdapter implements Spatial3dWorldModule {
    /** Hidden spatial root retained for later presentation adapters in this artifact. */
    private final Scene root = new Scene();

    /** Live transforms indexed by entity reference identity. */
    private final Map<Entity, Object3dTransform> byEntity = new IdentityHashMap<>();

    /** Registration order used for deterministic reverse cleanup. */
    private final List<Object3dTransform> registrations = new ArrayList<>();

    /** World identity captured by the first registered transform. */
    private @Nullable World world;

    /** Terminal adapter state. */
    private boolean closed;

    @Override
    public Transform3d createTransform(Entity owner, Vector3fc position, Quaternionfc orientation, Vector3fc scale) {
        requireOpen();
        Entity validOwner = Objects.requireNonNull(owner, "owner");
        World ownerWorld = validOwner.world();
        requireWorld(ownerWorld);
        if (byEntity.containsKey(validOwner)) {
            throw new IllegalArgumentException("entity already has a registered Transform3d: " + validOwner.id());
        }
        Object3D node = new Object3D();
        node.setPosition(Objects.requireNonNull(position, "position"));
        node.setQuaternion(Objects.requireNonNull(orientation, "orientation"));
        node.setScale(Objects.requireNonNull(scale, "scale"));
        Object3dTransform transform = new Object3dTransform(this, validOwner, node);
        parentNode(validOwner).add(node);
        byEntity.put(validOwner, transform);
        registrations.add(transform);
        return transform;
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
        List<Object3dTransform> remaining = List.copyOf(registrations);
        for (int index = remaining.size() - 1; index >= 0; index--) {
            remaining.get(index).close();
        }
        root.clear();
    }

    /** Removes one terminal transform registration without requiring this adapter to remain open. */
    void release(Object3dTransform transform) {
        Object3dTransform validTransform = Objects.requireNonNull(transform, "transform");
        Object3dTransform removed = byEntity.remove(validTransform.owner());
        if (removed != validTransform) {
            throw new IllegalStateException("transform registration does not belong to this adapter");
        }
        registrations.remove(validTransform);
        validTransform.node().detach();
    }

    /** Returns the compatible direct-parent node or the hidden spatial root. */
    private Object3D parentNode(Entity owner) {
        return owner.parent().map(byEntity::get).map(Object3dTransform::node).orElse(root);
    }

    /** Captures one world identity and rejects accidental adapter reuse across worlds. */
    private void requireWorld(World ownerWorld) {
        if (world == null) {
            world = ownerWorld;
        } else if (world != ownerWorld) {
            throw new IllegalArgumentException("spatial adapter cannot be shared by multiple worlds");
        }
    }

    /** Rejects registration after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("spatial adapter is closed");
        }
    }
}
