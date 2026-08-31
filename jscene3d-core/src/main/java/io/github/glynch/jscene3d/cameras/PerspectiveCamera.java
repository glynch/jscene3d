/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.cameras;

import io.github.glynch.jscene3d.internal.Preconditions;
import org.joml.Matrix4f;

/**
 * Camera using a right-handed perspective projection with OpenGL clip-space depth.
 *
 * <p>The field of view is vertical and expressed in radians. Projection properties are controlled,
 * validated, and automatically reflected by the matrix accessors. Instances are mutable and are
 * not thread-safe.
 */
public final class PerspectiveCamera extends Camera {
    private float fieldOfView;
    private float aspectRatio;
    private float near;
    private float far;

    /**
     * Creates a perspective camera.
     *
     * @param fieldOfView vertical field of view in radians, strictly between zero and pi
     * @param aspectRatio viewport width divided by height
     * @param near positive near clipping distance
     * @param far far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if any value is not finite or does not satisfy its stated
     *     relationship
     */
    public PerspectiveCamera(float fieldOfView, float aspectRatio, float near, float far) {
        super();
        this.fieldOfView = requireFieldOfView(fieldOfView);
        this.aspectRatio = Preconditions.requirePositive(aspectRatio, "aspectRatio");
        float validNear = Preconditions.requirePositive(near, "near");
        float validFar = Preconditions.requireFinite(far, "far");
        Preconditions.requireLessThan(validNear, "near", validFar, "far");
        this.near = validNear;
        this.far = validFar;
    }

    /**
     * Returns the vertical field of view in radians.
     *
     * @return the field of view
     */
    public float fieldOfView() {
        return fieldOfView;
    }

    /**
     * Returns the viewport width-to-height ratio.
     *
     * @return the aspect ratio
     */
    public float aspectRatio() {
        return aspectRatio;
    }

    /**
     * Returns the positive near clipping distance.
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
     * Sets the vertical field of view.
     *
     * @param fieldOfView field of view in radians, strictly between zero and pi
     * @throws IllegalArgumentException if {@code fieldOfView} is not finite or outside its required
     *     range
     */
    public void setFieldOfView(float fieldOfView) {
        float validFieldOfView = requireFieldOfView(fieldOfView);
        if (this.fieldOfView != validFieldOfView) {
            this.fieldOfView = validFieldOfView;
            markProjectionChanged();
        }
    }

    /**
     * Sets the viewport width-to-height ratio.
     *
     * @param aspectRatio positive aspect ratio
     * @throws IllegalArgumentException if {@code aspectRatio} is not finite and positive
     */
    public void setAspectRatio(float aspectRatio) {
        float validAspectRatio = Preconditions.requirePositive(aspectRatio, "aspectRatio");
        if (this.aspectRatio != validAspectRatio) {
            this.aspectRatio = validAspectRatio;
            markProjectionChanged();
        }
    }

    /**
     * Atomically sets both clipping distances.
     *
     * @param near positive near clipping distance
     * @param far far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if either value is not finite or the required relationship
     *     is not satisfied
     */
    public void setClippingPlanes(float near, float far) {
        float validNear = Preconditions.requirePositive(near, "near");
        float validFar = Preconditions.requireFinite(far, "far");
        Preconditions.requireLessThan(validNear, "near", validFar, "far");
        if (this.near != validNear || this.far != validFar) {
            this.near = validNear;
            this.far = validFar;
            markProjectionChanged();
        }
    }

    @Override
    void calculateProjectionMatrix(Matrix4f destination) {
        destination.setPerspective(fieldOfView, aspectRatio, near, far);
    }

    /** Requires a finite field of view strictly between zero and pi radians. */
    private static float requireFieldOfView(float fieldOfView) {
        float validFieldOfView = Preconditions.requirePositive(fieldOfView, "fieldOfView");
        if (validFieldOfView >= Math.PI) {
            throw new IllegalArgumentException("fieldOfView must be less than pi radians: " + validFieldOfView);
        }
        return validFieldOfView;
    }
}
