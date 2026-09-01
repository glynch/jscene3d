/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/** Copied and validated scalar storage shared by typed animation-track evaluators. */
final class KeyframeData {
    private static final int CUBIC_GROUPS_PER_KEY = 3;

    private final float[] times;
    private final float[] values;
    private final int components;
    private final int groupsPerKey;

    /** Copies arrays after validating their shape and finite, non-decreasing times. */
    KeyframeData(float[] times, float[] values, int components, Interpolation interpolation) {
        Objects.requireNonNull(interpolation, "interpolation");
        this.times = Objects.requireNonNull(times, "times").clone();
        this.values = Objects.requireNonNull(values, "values").clone();
        this.components = Preconditions.requirePositive(components, "components");
        groupsPerKey = interpolation == Interpolation.CUBIC_SPLINE ? CUBIC_GROUPS_PER_KEY : 1;
        validate();
    }

    /** Validates storage dimensions, finite values, and a non-negative time domain. */
    private void validate() {
        if (times.length == 0) {
            throw new IllegalArgumentException("times must contain at least one keyframe");
        }
        int expectedValues = Preconditions.requireArrayLength((long) times.length * groupsPerKey, components, "values");
        if (values.length != expectedValues) {
            throw new IllegalArgumentException("values length must be " + expectedValues + " for " + times.length
                    + " keyframes: " + values.length);
        }
        float previous = -1.0f;
        for (int index = 0; index < times.length; index++) {
            float time = Preconditions.requireNonNegative(times[index], "times[" + index + "]");
            if (index > 0 && time < previous) {
                throw new IllegalArgumentException("times must not decrease at index " + index + ": " + time);
            }
            previous = time;
        }
        for (int index = 0; index < values.length; index++) {
            Preconditions.requireFinite(values[index], "values[" + index + "]");
        }
    }

    /** Returns the final time value. */
    float duration() {
        return times[times.length - 1];
    }

    /** Returns the number of stored keyframes. */
    int keyCount() {
        return times.length;
    }

    /** Returns one key time. */
    float time(int keyIndex) {
        return times[keyIndex];
    }

    /** Returns the lower key index for a clamped sample time. */
    int lowerKey(float sampleTime) {
        if (sampleTime < times[0] || times.length == 1) {
            return 0;
        }
        int last = times.length - 1;
        if (sampleTime >= times[last]) {
            return last;
        }
        int low = 0;
        int high = last;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (sampleTime < times[middle]) {
                high = middle;
            } else {
                low = middle;
            }
        }
        return low;
    }

    /** Returns normalized progress from one key to its successor. */
    float progress(int lowerKey, float sampleTime) {
        float lowerTime = times[lowerKey];
        return (sampleTime - lowerTime) / (times[lowerKey + 1] - lowerTime);
    }

    /** Returns one ordinary or cubic value component. */
    float value(int keyIndex, int component) {
        return values[groupOffset(keyIndex, groupsPerKey == CUBIC_GROUPS_PER_KEY ? 1 : 0) + component];
    }

    /** Returns one cubic incoming-tangent component. */
    float incomingTangent(int keyIndex, int component) {
        return values[groupOffset(keyIndex, 0) + component];
    }

    /** Returns one cubic outgoing-tangent component. */
    float outgoingTangent(int keyIndex, int component) {
        return values[groupOffset(keyIndex, 2) + component];
    }

    /** Returns the scalar offset of one per-key value group. */
    private int groupOffset(int keyIndex, int groupIndex) {
        return (keyIndex * groupsPerKey + groupIndex) * components;
    }
}
