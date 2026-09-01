/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Allocation-free interpolation shared by the typed animation tracks. */
final class KeyframeSampler {
    /** Prevents instantiation of this interpolation utility. */
    private KeyframeSampler() {
        throw new AssertionError("KeyframeSampler cannot be instantiated");
    }

    /** Samples a vector-valued track into caller-owned scalar storage. */
    static void vector(KeyframeData data, Interpolation interpolation, float time, float[] destination) {
        int lower = data.lowerKey(time);
        if (lower == data.keyCount() - 1 || time <= data.time(0)) {
            copyValue(data, lower, destination);
            return;
        }
        float progress = data.progress(lower, time);
        switch (interpolation) {
            case STEP -> copyValue(data, lower, destination);
            case LINEAR -> linear(data, lower, progress, destination);
            case CUBIC_SPLINE -> cubic(data, lower, progress, destination);
        }
    }

    /** Samples a quaternion track, using spherical interpolation for linear keys. */
    static void quaternion(KeyframeData data, Interpolation interpolation, float time, float[] destination) {
        int lower = data.lowerKey(time);
        if (lower == data.keyCount() - 1 || time <= data.time(0)) {
            copyValue(data, lower, destination);
        } else if (interpolation == Interpolation.LINEAR) {
            slerp(data, lower, data.progress(lower, time), destination);
        } else if (interpolation == Interpolation.STEP) {
            copyValue(data, lower, destination);
        } else {
            cubic(data, lower, data.progress(lower, time), destination);
        }
        normalizeQuaternion(destination);
    }

    /** Copies one stored value group. */
    private static void copyValue(KeyframeData data, int key, float[] destination) {
        for (int component = 0; component < destination.length; component++) {
            destination[component] = data.value(key, component);
        }
    }

    /** Interpolates scalar components linearly. */
    private static void linear(KeyframeData data, int lower, float progress, float[] destination) {
        for (int component = 0; component < destination.length; component++) {
            float start = data.value(lower, component);
            destination[component] = Math.fma(data.value(lower + 1, component) - start, progress, start);
        }
    }

    /** Applies glTF-compatible cubic Hermite interpolation. */
    private static void cubic(KeyframeData data, int lower, float progress, float[] destination) {
        float squared = progress * progress;
        float cubed = squared * progress;
        float startBasis = 2.0f * cubed - 3.0f * squared + 1.0f;
        float startTangentBasis = cubed - 2.0f * squared + progress;
        float endBasis = -2.0f * cubed + 3.0f * squared;
        float endTangentBasis = cubed - squared;
        float interval = data.time(lower + 1) - data.time(lower);
        for (int component = 0; component < destination.length; component++) {
            destination[component] = startBasis * data.value(lower, component)
                    + startTangentBasis * interval * data.outgoingTangent(lower, component)
                    + endBasis * data.value(lower + 1, component)
                    + endTangentBasis * interval * data.incomingTangent(lower + 1, component);
        }
    }

    /** Applies shortest-path spherical interpolation between normalized quaternion keys. */
    private static void slerp(KeyframeData data, int lower, float progress, float[] destination) {
        float ax = data.value(lower, 0);
        float ay = data.value(lower, 1);
        float az = data.value(lower, 2);
        float aw = data.value(lower, 3);
        float bx = data.value(lower + 1, 0);
        float by = data.value(lower + 1, 1);
        float bz = data.value(lower + 1, 2);
        float bw = data.value(lower + 1, 3);
        float cosine = ax * bx + ay * by + az * bz + aw * bw;
        if (cosine < 0.0f) {
            cosine = -cosine;
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
        }
        if (cosine > 0.9995f) {
            destination[0] = Math.fma(bx - ax, progress, ax);
            destination[1] = Math.fma(by - ay, progress, ay);
            destination[2] = Math.fma(bz - az, progress, az);
            destination[3] = Math.fma(bw - aw, progress, aw);
            return;
        }
        double angle = Math.acos(Math.clamp(cosine, -1.0f, 1.0f));
        double inverseSine = 1.0 / Math.sin(angle);
        float startWeight = (float) (Math.sin((1.0f - progress) * angle) * inverseSine);
        float endWeight = (float) (Math.sin(progress * angle) * inverseSine);
        destination[0] = startWeight * ax + endWeight * bx;
        destination[1] = startWeight * ay + endWeight * by;
        destination[2] = startWeight * az + endWeight * bz;
        destination[3] = startWeight * aw + endWeight * bw;
    }

    /** Normalizes a sampled quaternion and rejects a degenerate result. */
    private static void normalizeQuaternion(float[] quaternion) {
        float largest = Math.max(
                Math.max(Math.abs(quaternion[0]), Math.abs(quaternion[1])),
                Math.max(Math.abs(quaternion[2]), Math.abs(quaternion[3])));
        if (largest == 0.0f) {
            throw new IllegalArgumentException("sampled quaternion must not have zero length");
        }
        float x = quaternion[0] / largest;
        float y = quaternion[1] / largest;
        float z = quaternion[2] / largest;
        float w = quaternion[3] / largest;
        float inverseLength = (float) (1.0 / Math.sqrt(x * x + y * y + z * z + w * w));
        quaternion[0] = x * inverseLength;
        quaternion[1] = y * inverseLength;
        quaternion[2] = z * inverseLength;
        quaternion[3] = w * inverseLength;
    }
}
