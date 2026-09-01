/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.math.Color;
import java.util.Arrays;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Renders many instances of one shared triangle geometry and material as one batch.
 *
 * <p>Each instance transform is local to this object's own world transform. Capacity is fixed at
 * construction, while {@link #count()} may select any leading subset without reallocating. Matrix
 * and color setters copy their inputs and automatically version changed data for renderer uploads.
 * Instances are mutable and are not thread-safe.
 *
 * <p>Main- and shadow-pass callbacks are invoked once for the complete batch, not once per
 * instance. Transparent instances share one batch-level render order and therefore are not sorted
 * against one another.
 */
public final class InstancedMesh extends Mesh {
    /** Number of scalar components in one four-by-four transform. */
    public static final int MATRIX_COMPONENTS = 16;

    /** Number of scalar components in one linear RGB color. */
    public static final int COLOR_COMPONENTS = 3;

    private final int capacity;
    private final float[] matrices;
    private final float[] matrixValueScratch;
    private final Matrix4f matrixScratch;
    private final Vector3f centerScratch;
    private final Vector3f scaleScratch;

    private @Nullable float[] colors;
    private @Nullable BoundingSphere cachedBoundingSphere;
    private @Nullable BufferGeometry cachedBoundsGeometry;
    private long cachedBoundsMatrixVersion = -1L;
    private long matrixVersion;
    private long colorVersion;
    private int cachedBoundsCount = -1;
    private int count;

    /**
     * Creates a batch with identity transforms and the supplied fixed capacity.
     *
     * @param geometry open shared triangle geometry
     * @param material open shared built-in mesh material
     * @param capacity positive maximum number of instances
     * @throws NullPointerException if a resource is {@code null}
     * @throws IllegalArgumentException if a resource is closed or capacity is not positive
     */
    public InstancedMesh(BufferGeometry geometry, Material material, int capacity) {
        super(geometry, material);
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        count = capacity;
        matrices = new float[Math.multiplyExact(capacity, MATRIX_COMPONENTS)];
        matrixValueScratch = new float[MATRIX_COMPONENTS];
        matrixScratch = new Matrix4f();
        centerScratch = new Vector3f();
        scaleScratch = new Vector3f();
        for (int index = 0; index < capacity; index++) {
            writeIdentity(index);
        }
    }

    /**
     * Returns the fixed allocation capacity.
     *
     * @return positive maximum instance count
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns how many leading instances participate in rendering, bounds, and raycasting.
     *
     * @return active instance count from zero through {@link #capacity()}
     */
    public int count() {
        return count;
    }

    /**
     * Selects how many leading instances participate without reallocating storage.
     *
     * @param count active instance count from zero through {@link #capacity()}
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    public void setCount(int count) {
        if (count < 0 || count > capacity) {
            throw new IllegalArgumentException("count must be in [0, " + capacity + "]: " + count);
        }
        if (this.count != count) {
            this.count = count;
            cachedBoundsCount = -1;
        }
    }

    /**
     * Copies one finite, affine, invertible local instance transform.
     *
     * @param index zero-based instance index within capacity
     * @param matrix transform to copy
     * @throws IndexOutOfBoundsException if the index is outside capacity
     * @throws NullPointerException if the matrix is {@code null}
     * @throws IllegalArgumentException if the matrix is non-finite, non-affine, or singular
     */
    public void setMatrixAt(int index, Matrix4fc matrix) {
        Objects.checkIndex(index, capacity);
        Matrix4fc validMatrix = Objects.requireNonNull(matrix, "matrix");
        if (!validMatrix.isFinite()) {
            throw new IllegalArgumentException("instance matrix must be finite");
        }
        if (!validMatrix.isAffine()) {
            throw new IllegalArgumentException("instance matrix must be affine");
        }
        float determinant = validMatrix.determinant3x3();
        if (!Float.isFinite(determinant) || determinant == 0.0f) {
            throw new IllegalArgumentException("instance matrix must be invertible");
        }
        int offset = index * MATRIX_COMPONENTS;
        validMatrix.get(matrixScratch).get(matrixValueScratch);
        if (Arrays.mismatch(matrices, offset, offset + MATRIX_COMPONENTS, matrixValueScratch, 0, MATRIX_COMPONENTS)
                < 0) {
            return;
        }
        System.arraycopy(matrixValueScratch, 0, matrices, offset, MATRIX_COMPONENTS);
        matrixVersion++;
        cachedBoundsMatrixVersion = -1L;
    }

    /**
     * Copies one local instance transform into caller-owned storage.
     *
     * @param index zero-based instance index within capacity
     * @param destination matrix receiving the transform
     * @return {@code destination}
     * @throws IndexOutOfBoundsException if the index is outside capacity
     * @throws NullPointerException if the destination is {@code null}
     */
    public Matrix4f matrixAt(int index, Matrix4f destination) {
        Objects.checkIndex(index, capacity);
        return Objects.requireNonNull(destination, "destination").set(matrices, index * MATRIX_COMPONENTS);
    }

    /**
     * Assigns one optional per-instance color, creating white defaults on first use.
     *
     * @param index zero-based instance index within capacity
     * @param color immutable linear color to copy
     * @throws IndexOutOfBoundsException if the index is outside capacity
     * @throws NullPointerException if the color is {@code null}
     */
    public void setColorAt(int index, Color color) {
        Objects.checkIndex(index, capacity);
        Color validColor = Objects.requireNonNull(color, "color");
        if (colors == null) {
            colors = new float[Math.multiplyExact(capacity, COLOR_COMPONENTS)];
            Arrays.fill(colors, 1.0f);
        }
        int offset = index * COLOR_COMPONENTS;
        if (colors[offset] != validColor.red()
                || colors[offset + 1] != validColor.green()
                || colors[offset + 2] != validColor.blue()) {
            colors[offset] = validColor.red();
            colors[offset + 1] = validColor.green();
            colors[offset + 2] = validColor.blue();
            colorVersion++;
        }
    }

    /**
     * Returns whether a per-instance color buffer is active.
     *
     * @return whether at least one color has been assigned
     */
    public boolean hasInstanceColors() {
        return colors != null;
    }

    /**
     * Copies one assigned instance color.
     *
     * @param index zero-based instance index within capacity
     * @return immutable copied linear color
     * @throws IndexOutOfBoundsException if the index is outside capacity
     * @throws IllegalStateException if instance colors are not active
     */
    public Color colorAt(int index) {
        Objects.checkIndex(index, capacity);
        float[] activeColors = colors;
        if (activeColors == null) {
            throw new IllegalStateException("Instanced mesh has no instance colors");
        }
        int offset = index * COLOR_COMPONENTS;
        return Color.linear(activeColors[offset], activeColors[offset + 1], activeColors[offset + 2]);
    }

    /** Removes the optional color buffer so every instance resolves to white. */
    public void clearInstanceColors() {
        if (colors != null) {
            colors = null;
            colorVersion++;
        }
    }

    /**
     * Returns the monotonic transform-data version used by renderer resources.
     *
     * @return transform version
     */
    public long matrixVersion() {
        return matrixVersion;
    }

    /**
     * Returns the monotonic color-data version used by renderer resources.
     *
     * @return color version
     */
    public long colorVersion() {
        return colorVersion;
    }

    /**
     * Copies all capacity-sized transform data into renderer or application storage.
     *
     * @param destination array of exactly {@code capacity * 16} floats
     * @throws NullPointerException if the destination is {@code null}
     * @throws IllegalArgumentException if its length differs
     */
    public void copyMatricesTo(float[] destination) {
        requireLength(destination, matrices.length, "matrix destination");
        System.arraycopy(matrices, 0, destination, 0, matrices.length);
    }

    /**
     * Copies all capacity-sized color data into renderer or application storage.
     *
     * @param destination array of exactly {@code capacity * 3} floats
     * @throws NullPointerException if the destination is {@code null}
     * @throws IllegalArgumentException if its length differs
     * @throws IllegalStateException if instance colors are not active
     */
    public void copyColorsTo(float[] destination) {
        float[] activeColors = colors;
        if (activeColors == null) {
            throw new IllegalStateException("Instanced mesh has no instance colors");
        }
        requireLength(destination, activeColors.length, "color destination");
        System.arraycopy(activeColors, 0, destination, 0, activeColors.length);
    }

    /**
     * Returns a cached local-space sphere enclosing every active transformed geometry instance.
     *
     * @return aggregate local-space bounds, or {@code null} when no instances are active
     */
    public @Nullable BoundingSphere boundingSphere() {
        if (count == 0) {
            return null;
        }
        BufferGeometry geometry = geometry();
        if (cachedBoundingSphere == null
                || cachedBoundsGeometry != geometry
                || cachedBoundsCount != count
                || cachedBoundsMatrixVersion != matrixVersion) {
            cachedBoundingSphere = computeBoundingSphere(geometry);
            cachedBoundsGeometry = geometry;
            cachedBoundsCount = count;
            cachedBoundsMatrixVersion = matrixVersion;
        }
        return cachedBoundingSphere;
    }

    /** Computes conservative aggregate bounds from each transformed geometry sphere. */
    private BoundingSphere computeBoundingSphere(BufferGeometry geometry) {
        BoundingSphere geometrySphere = geometry.boundingSphere();
        if (geometrySphere == null) {
            geometrySphere = geometry.computeBoundingSphere();
        }
        float minimumX = Float.POSITIVE_INFINITY;
        float minimumY = Float.POSITIVE_INFINITY;
        float minimumZ = Float.POSITIVE_INFINITY;
        float maximumX = Float.NEGATIVE_INFINITY;
        float maximumY = Float.NEGATIVE_INFINITY;
        float maximumZ = Float.NEGATIVE_INFINITY;
        for (int index = 0; index < count; index++) {
            matrixAt(index, matrixScratch);
            matrixScratch.transformPosition(geometrySphere.center(), centerScratch);
            matrixScratch.getScale(scaleScratch);
            float radius = geometrySphere.radius() * maximumAbsoluteComponent(scaleScratch);
            minimumX = Math.min(minimumX, centerScratch.x - radius);
            minimumY = Math.min(minimumY, centerScratch.y - radius);
            minimumZ = Math.min(minimumZ, centerScratch.z - radius);
            maximumX = Math.max(maximumX, centerScratch.x + radius);
            maximumY = Math.max(maximumY, centerScratch.y + radius);
            maximumZ = Math.max(maximumZ, centerScratch.z + radius);
        }
        float centerX = (minimumX + maximumX) * 0.5f;
        float centerY = (minimumY + maximumY) * 0.5f;
        float centerZ = (minimumZ + maximumZ) * 0.5f;
        float radius = 0.0f;
        for (int index = 0; index < count; index++) {
            matrixAt(index, matrixScratch);
            matrixScratch.transformPosition(geometrySphere.center(), centerScratch);
            matrixScratch.getScale(scaleScratch);
            float instanceRadius = geometrySphere.radius() * maximumAbsoluteComponent(scaleScratch);
            float centerDistance = centerScratch.distance(centerX, centerY, centerZ);
            radius = Math.max(radius, centerDistance + instanceRadius);
        }
        return new BoundingSphere(centerX, centerY, centerZ, radius);
    }

    /** Writes one identity matrix directly during construction. */
    private void writeIdentity(int index) {
        matrixScratch.identity().get(matrices, index * MATRIX_COMPONENTS);
    }

    /** Returns the largest absolute vector component. */
    private static float maximumAbsoluteComponent(Vector3f value) {
        return Math.max(Math.max(Math.abs(value.x), Math.abs(value.y)), Math.abs(value.z));
    }

    /** Validates one exact-length caller-owned copy destination. */
    private static void requireLength(float[] destination, int expected, String label) {
        float[] validDestination = Objects.requireNonNull(destination, label);
        if (validDestination.length != expected) {
            throw new IllegalArgumentException(label + " length must be " + expected + ": " + validDestination.length);
        }
    }
}
