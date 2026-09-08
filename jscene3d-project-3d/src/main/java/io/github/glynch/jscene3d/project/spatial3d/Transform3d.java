/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * Mutable three-dimensional spatial state owned by one live entity.
 *
 * <p>The local transform consists of translation, normalized orientation, and scale composed as {@code T * R * S}.
 * Its world transform is derived automatically from the direct ownership parent's compatible transform. A
 * non-spatial or differently spatial parent starts a new three-dimensional spatial root. This component is confined to
 * its world's logical simulation thread.
 *
 * <p>The owning world controls closure. After closure, only {@link #isClosed()} and repeated {@link #close()} calls are
 * valid.
 */
public interface Transform3d extends AutoCloseable {
    /**
     * Returns the stable live read-only local-position view.
     *
     * @return local position
     * @throws IllegalStateException if this component is closed
     */
    Vector3fc position();

    /**
     * Returns the stable live read-only normalized local-orientation view.
     *
     * @return local orientation
     * @throws IllegalStateException if this component is closed
     */
    Quaternionfc orientation();

    /**
     * Returns the stable live read-only local-scale view.
     *
     * @return local scale
     * @throws IllegalStateException if this component is closed
     */
    Vector3fc scale();

    /**
     * Sets the local position.
     *
     * @param x finite local X coordinate
     * @param y finite local Y coordinate
     * @param z finite local Z coordinate
     * @throws IllegalArgumentException if a coordinate is not finite
     * @throws IllegalStateException if this component is closed
     */
    void setPosition(float x, float y, float z);

    /**
     * Sets and normalizes the local orientation.
     *
     * @param x finite quaternion X component
     * @param y finite quaternion Y component
     * @param z finite quaternion Z component
     * @param w finite quaternion W component
     * @throws IllegalArgumentException if a component is not finite or the quaternion has zero length
     * @throws IllegalStateException if this component is closed
     */
    void setOrientation(float x, float y, float z, float w);

    /**
     * Sets a world-space rigid pose while preserving this transform's local scale.
     *
     * <p>The implementation derives the corresponding local position and orientation from the current spatial
     * parent. This is the write seam for physics authorities whose state is expressed in world space.
     *
     * @param position finite world-space position
     * @param orientation finite non-zero world-space orientation
     * @throws IllegalArgumentException if an input is invalid
     * @throws IllegalStateException if this component is closed
     */
    void setWorldPose(Vector3fc position, Quaternionfc orientation);

    /**
     * Sets the local scale.
     *
     * @param x finite local X scale
     * @param y finite local Y scale
     * @param z finite local Z scale
     * @throws IllegalArgumentException if a component is not finite
     * @throws IllegalStateException if this component is closed
     */
    void setScale(float x, float y, float z);

    /**
     * Returns the stable live read-only local matrix, updating it when necessary.
     *
     * @return current local matrix
     * @throws IllegalStateException if this component is closed
     */
    Matrix4fc localMatrix();

    /**
     * Returns the stable live read-only world matrix, updating its ownership ancestry when necessary.
     *
     * @return current world matrix
     * @throws IllegalStateException if this component is closed
     */
    Matrix4fc worldMatrix();

    /**
     * Returns whether the owning world has permanently released this component.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();

    /** Releases this component from its world-scoped spatial adapter. */
    @Override
    void close();
}
