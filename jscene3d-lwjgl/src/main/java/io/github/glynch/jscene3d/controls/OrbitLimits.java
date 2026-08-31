/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI;

/** Package-private validated distance, zoom, and angle limits for orbit state application. */
final class OrbitLimits {
    private float minimumDistance = 0.01f;
    private float maximumDistance = Float.MAX_VALUE;
    private float minimumZoom = 0.01f;
    private float maximumZoom = Float.MAX_VALUE;
    private float minimumPolarAngle;
    private float maximumPolarAngle = PI;
    private float minimumAzimuthAngle = Float.NEGATIVE_INFINITY;
    private float maximumAzimuthAngle = Float.POSITIVE_INFINITY;

    /** Returns the minimum perspective-camera distance. */
    float minimumDistance() {
        return minimumDistance;
    }

    /** Returns the maximum perspective-camera distance. */
    float maximumDistance() {
        return maximumDistance;
    }

    /** Sets the already-validated perspective-camera distance limits. */
    void setDistance(float minimum, float maximum) {
        minimumDistance = minimum;
        maximumDistance = maximum;
    }

    /** Returns the minimum orthographic-camera zoom. */
    float minimumZoom() {
        return minimumZoom;
    }

    /** Returns the maximum orthographic-camera zoom. */
    float maximumZoom() {
        return maximumZoom;
    }

    /** Sets the already-validated orthographic-camera zoom limits. */
    void setZoom(float minimum, float maximum) {
        minimumZoom = minimum;
        maximumZoom = maximum;
    }

    /** Returns the minimum polar angle. */
    float minimumPolarAngle() {
        return minimumPolarAngle;
    }

    /** Returns the maximum polar angle. */
    float maximumPolarAngle() {
        return maximumPolarAngle;
    }

    /** Sets the already-validated polar-angle limits. */
    void setPolarAngle(float minimum, float maximum) {
        minimumPolarAngle = minimum;
        maximumPolarAngle = maximum;
    }

    /** Returns the minimum azimuth angle. */
    float minimumAzimuthAngle() {
        return minimumAzimuthAngle;
    }

    /** Returns the maximum azimuth angle. */
    float maximumAzimuthAngle() {
        return maximumAzimuthAngle;
    }

    /** Sets the already-validated azimuth-angle limits. */
    void setAzimuthAngle(float minimum, float maximum) {
        minimumAzimuthAngle = minimum;
        maximumAzimuthAngle = maximum;
    }
}
