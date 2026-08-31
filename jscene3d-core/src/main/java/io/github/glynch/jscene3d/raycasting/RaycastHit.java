/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import io.github.glynch.jscene3d.objects.Mesh;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Immutable result of a ray intersecting one mesh triangle. */
public final class RaycastHit {
    private final Mesh mesh;
    private final float distance;
    private final float pointX;
    private final float pointY;
    private final float pointZ;
    private final int faceIndex;
    private final boolean hasTextureCoordinate;
    private final float textureCoordinateU;
    private final float textureCoordinateV;

    /** Retains one validated intersection while copying its vector values. */
    RaycastHit(Mesh mesh, float distance, Vector3fc point, int faceIndex, @Nullable Vector2fc textureCoordinate) {
        this.mesh = Objects.requireNonNull(mesh, "mesh");
        this.distance = distance;
        Vector3fc validPoint = Objects.requireNonNull(point, "point");
        pointX = validPoint.x();
        pointY = validPoint.y();
        pointZ = validPoint.z();
        this.faceIndex = faceIndex;
        hasTextureCoordinate = textureCoordinate != null;
        textureCoordinateU = textureCoordinate == null ? 0.0f : textureCoordinate.x();
        textureCoordinateV = textureCoordinate == null ? 0.0f : textureCoordinate.y();
    }

    /**
     * Returns the intersected mesh.
     *
     * @return intersected mesh
     */
    public Mesh mesh() {
        return mesh;
    }

    /**
     * Returns the distance from the ray origin to the intersection.
     *
     * @return non-negative world-space distance
     */
    public float distance() {
        return distance;
    }

    /**
     * Copies the world-space intersection point into caller-owned storage.
     *
     * @param destination vector receiving the intersection point
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Vector3f point(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(pointX, pointY, pointZ);
    }

    /**
     * Returns the zero-based triangle index in the geometry element stream.
     *
     * @return intersected face index
     */
    public int faceIndex() {
        return faceIndex;
    }

    /**
     * Returns whether the geometry supplied texture coordinates for this hit.
     *
     * @return whether an interpolated texture coordinate is available
     */
    public boolean hasTextureCoordinate() {
        return hasTextureCoordinate;
    }

    /**
     * Copies the interpolated texture coordinate into caller-owned storage.
     *
     * @param destination vector receiving the coordinate
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if the intersected geometry has no texture coordinates
     */
    public Vector2f textureCoordinate(Vector2f destination) {
        Vector2f validDestination = Objects.requireNonNull(destination, "destination");
        if (!hasTextureCoordinate) {
            throw new IllegalStateException("Raycast hit has no texture coordinate");
        }
        return validDestination.set(textureCoordinateU, textureCoordinateV);
    }
}
