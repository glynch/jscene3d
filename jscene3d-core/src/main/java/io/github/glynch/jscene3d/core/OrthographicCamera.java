/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import org.joml.Matrix4f;

/**
 * Camera using a right-handed orthographic projection with OpenGL clip-space depth.
 *
 * <p>Projection properties are controlled, validated, and automatically reflected by the matrix
 * accessors. Instances are mutable and are not thread-safe.
 */
public final class OrthographicCamera extends Camera {
    private float left;
    private float right;
    private float top;
    private float bottom;
    private float near;
    private float far;
    private float zoom = 1.0f;

    /**
     * Creates an orthographic camera.
     *
     * @param left left projection bound
     * @param right right projection bound greater than {@code left}
     * @param top top projection bound greater than {@code bottom}
     * @param bottom bottom projection bound
     * @param near non-negative near clipping distance
     * @param far far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if any value is not finite or does not satisfy its stated
     *     relationship
     */
    public OrthographicCamera(float left, float right, float top, float bottom, float near, float far) {
        super();
        float validLeft = Preconditions.requireFinite(left, "left");
        float validRight = Preconditions.requireFinite(right, "right");
        float validTop = Preconditions.requireFinite(top, "top");
        float validBottom = Preconditions.requireFinite(bottom, "bottom");
        validateBounds(validLeft, validRight, validTop, validBottom);
        float validNear = Preconditions.requireNonNegative(near, "near");
        float validFar = Preconditions.requireFinite(far, "far");
        Preconditions.requireLessThan(validNear, "near", validFar, "far");
        this.left = validLeft;
        this.right = validRight;
        this.top = validTop;
        this.bottom = validBottom;
        this.near = validNear;
        this.far = validFar;
    }

    /**
     * Returns the left projection bound.
     *
     * @return the left bound
     */
    public float left() {
        return left;
    }

    /**
     * Returns the right projection bound.
     *
     * @return the right bound
     */
    public float right() {
        return right;
    }

    /**
     * Returns the top projection bound.
     *
     * @return the top bound
     */
    public float top() {
        return top;
    }

    /**
     * Returns the bottom projection bound.
     *
     * @return the bottom bound
     */
    public float bottom() {
        return bottom;
    }

    /**
     * Returns the non-negative near clipping distance.
     *
     * @return the near clipping distance
     */
    public float near() {
        return near;
    }

    /**
     * Returns the far clipping distance.
     *
     * @return the far clipping distance
     */
    public float far() {
        return far;
    }

    /**
     * Returns the projection magnification.
     *
     * @return the positive zoom factor, where {@code 1} preserves the configured bounds
     */
    public float zoom() {
        return zoom;
    }

    /**
     * Atomically sets all projection bounds.
     *
     * @param left left projection bound
     * @param right right projection bound greater than {@code left}
     * @param top top projection bound greater than {@code bottom}
     * @param bottom bottom projection bound
     * @throws IllegalArgumentException if any value is not finite or the required relationships are
     *     not satisfied
     */
    public void setBounds(float left, float right, float top, float bottom) {
        float validLeft = Preconditions.requireFinite(left, "left");
        float validRight = Preconditions.requireFinite(right, "right");
        float validTop = Preconditions.requireFinite(top, "top");
        float validBottom = Preconditions.requireFinite(bottom, "bottom");
        validateBounds(validLeft, validRight, validTop, validBottom);
        if (this.left != validLeft || this.right != validRight || this.top != validTop || this.bottom != validBottom) {
            this.left = validLeft;
            this.right = validRight;
            this.top = validTop;
            this.bottom = validBottom;
            markProjectionChanged();
        }
    }

    /**
     * Atomically sets both clipping distances.
     *
     * @param near non-negative near clipping distance
     * @param far far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if either value is not finite or the required relationship
     *     is not satisfied
     */
    public void setClippingPlanes(float near, float far) {
        float validNear = Preconditions.requireNonNegative(near, "near");
        float validFar = Preconditions.requireFinite(far, "far");
        Preconditions.requireLessThan(validNear, "near", validFar, "far");
        if (this.near != validNear || this.far != validFar) {
            this.near = validNear;
            this.far = validFar;
            markProjectionChanged();
        }
    }

    /**
     * Sets the projection magnification while preserving the center of the configured bounds.
     *
     * @param zoom finite positive zoom factor
     * @throws IllegalArgumentException if {@code zoom} is not finite and positive
     */
    public void setZoom(float zoom) {
        float validZoom = Preconditions.requirePositive(zoom, "zoom");
        if (this.zoom != validZoom) {
            this.zoom = validZoom;
            markProjectionChanged();
        }
    }

    @Override
    void calculateProjectionMatrix(Matrix4f destination) {
        float centerX = (left + right) * 0.5f;
        float centerY = (bottom + top) * 0.5f;
        float halfWidth = (right - left) * 0.5f / zoom;
        float halfHeight = (top - bottom) * 0.5f / zoom;
        destination.setOrtho(
                centerX - halfWidth, centerX + halfWidth, centerY - halfHeight, centerY + halfHeight, near, far);
    }

    private static void validateBounds(float left, float right, float top, float bottom) {
        Preconditions.requireLessThan(left, "left", right, "right");
        Preconditions.requireLessThan(bottom, "bottom", top, "top");
    }
}
