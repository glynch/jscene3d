/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Internal facade around the world's dynamic AABB tree. */
public final class WorldIndex {
    private final DynamicAabbTree tree;

    /** Creates an empty spatial index. */
    public WorldIndex() {
        tree = new DynamicAabbTree();
    }

    /**
     * Adds a collider with the supplied pose.
     *
     * @param collider collider to index
     * @param pose collider shape and transform
     */
    public void add(Collider collider, ShapePose pose) {
        tree.add(collider, ShapeBounds.of(pose));
    }

    /**
     * Removes a collider.
     *
     * @param collider collider to remove
     */
    public void remove(Collider collider) {
        tree.remove(collider);
    }

    /**
     * Updates the bounds for a moved collider.
     *
     * @param collider collider to update
     * @param pose replacement shape pose
     */
    public void update(Collider collider, ShapePose pose) {
        tree.update(collider, ShapeBounds.of(pose));
    }

    /**
     * Returns broad-phase overlap candidates.
     *
     * @param pose query shape pose
     * @return mutable candidate list owned by the caller
     */
    public List<Collider> overlapCandidates(ShapePose pose) {
        return tree.query(ShapeBounds.of(pose));
    }

    /**
     * Returns broad-phase ray candidates.
     *
     * @param origin world-space ray origin
     * @param direction normalized ray direction
     * @param maximumDistance maximum world-space distance
     * @return mutable candidate list owned by the caller
     */
    public List<Collider> rayCandidates(Vector3fc origin, Vector3fc direction, float maximumDistance) {
        return tree.queryRay(origin, direction, maximumDistance);
    }

    /**
     * Returns broad-phase sweep candidates.
     *
     * @param pose starting query shape pose
     * @param translation world-space translation
     * @return mutable candidate list owned by the caller
     */
    public List<Collider> sweepCandidates(ShapePose pose, Vector3fc translation) {
        return tree.query(ShapeBounds.swept(pose, new Vector3f(translation)));
    }

    /** Removes every collider from the index. */
    public void clear() {
        tree.clear();
    }
}
