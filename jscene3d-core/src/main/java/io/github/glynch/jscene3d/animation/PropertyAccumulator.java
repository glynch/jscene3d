/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Arrays;
import org.joml.Vector3fc;

/** Mixer-owned weighted accumulator for one object identity and animated property. */
final class PropertyAccumulator {
    private final Object3D target;
    private final AnimatedProperty property;
    private final int components;
    private final float[] base;
    private final float[] weighted;
    private final float[] reference;

    private float totalWeight;
    private boolean baseCaptured;
    private boolean referenceCaptured;

    /** Retains the controlled target and property without capturing mutable scene state. */
    PropertyAccumulator(Object3D target, AnimatedProperty property, int components) {
        this.target = target;
        this.property = property;
        this.components = components;
        base = new float[components];
        weighted = new float[components];
        reference = new float[components];
    }

    /** Returns the component count established by the first registered track. */
    int components() {
        return components;
    }

    /** Clears contributions while retaining the base pose captured by an active blend. */
    void beginEvaluation() {
        Arrays.fill(weighted, 0.0f);
        totalWeight = 0.0f;
        referenceCaptured = false;
    }

    /** Adds one positive weighted sample, aligning equivalent quaternion signs when required. */
    void accumulate(float[] sample, float weight) {
        if (!baseCaptured) {
            captureBase();
        }
        float signedWeight = property == TransformProperty.ROTATION ? alignedWeight(sample, weight) : weight;
        for (int component = 0; component < components; component++) {
            weighted[component] = Math.fma(sample[component], signedWeight, weighted[component]);
        }
        totalWeight += weight;
    }

    /** Applies the blended result once, or restores and releases an inactive base pose. */
    void apply() {
        if (totalWeight == 0.0f) {
            restoreInactiveBase();
        } else if (property == TransformProperty.ROTATION) {
            applyQuaternion();
        } else if (property instanceof TransformProperty) {
            applyVector();
        } else {
            applyMorphInfluences();
        }
    }

    /** Captures the property value that contributes whenever action weights total less than one. */
    private void captureBase() {
        if (property == MorphProperty.INFLUENCES) {
            ((Mesh) target).copyMorphTargetInfluencesTo(base);
        } else if (property == TransformProperty.POSITION) {
            copyVector(target.position(), base);
        } else if (property == TransformProperty.ROTATION) {
            base[0] = target.quaternion().x();
            base[1] = target.quaternion().y();
            base[2] = target.quaternion().z();
            base[3] = target.quaternion().w();
        } else {
            copyVector(target.scale(), base);
        }
        baseCaptured = true;
    }

    /** Copies one read-only vector without retaining its live view. */
    private static void copyVector(Vector3fc source, float[] destination) {
        destination[0] = source.x();
        destination[1] = source.y();
        destination[2] = source.z();
    }

    /** Aligns quaternion samples to the first contribution so opposite signs cannot cancel. */
    private float alignedWeight(float[] sample, float weight) {
        if (!referenceCaptured) {
            System.arraycopy(sample, 0, reference, 0, 4);
            referenceCaptured = true;
            return weight;
        }
        return dot(sample, reference) < 0.0f ? -weight : weight;
    }

    /** Applies a base-completed blend below unit weight or a normalized blend above unit weight. */
    private void applyVector() {
        completeLinearBlend();
        if (property == TransformProperty.POSITION) {
            target.setPosition(weighted[0], weighted[1], weighted[2]);
        } else {
            target.setScale(weighted[0], weighted[1], weighted[2]);
        }
    }

    /** Applies component-wise blended morph influences. */
    private void applyMorphInfluences() {
        completeLinearBlend();
        ((Mesh) target).setMorphTargetInfluences(weighted);
    }

    /** Completes a linear blend with its base below unit weight or normalizes above it. */
    private void completeLinearBlend() {
        if (totalWeight < 1.0f) {
            float baseWeight = 1.0f - totalWeight;
            for (int component = 0; component < components; component++) {
                weighted[component] = Math.fma(base[component], baseWeight, weighted[component]);
            }
        } else if (totalWeight > 1.0f) {
            for (int component = 0; component < components; component++) {
                weighted[component] /= totalWeight;
            }
        }
    }

    /** Completes quaternion weight with the base pose, then normalizes the accumulated result. */
    private void applyQuaternion() {
        if (totalWeight < 1.0f) {
            float baseWeight = 1.0f - totalWeight;
            if (dot(base, reference) < 0.0f) {
                baseWeight = -baseWeight;
            }
            for (int component = 0; component < 4; component++) {
                weighted[component] = Math.fma(base[component], baseWeight, weighted[component]);
            }
        }
        normalize(weighted);
        target.setQuaternion(weighted[0], weighted[1], weighted[2], weighted[3]);
    }

    /** Restores the base once no action contributes, allowing later playback to capture a new base. */
    private void restoreInactiveBase() {
        if (!baseCaptured) {
            return;
        }
        if (property == MorphProperty.INFLUENCES) {
            ((Mesh) target).setMorphTargetInfluences(base);
        } else if (property == TransformProperty.POSITION) {
            target.setPosition(base[0], base[1], base[2]);
        } else if (property == TransformProperty.ROTATION) {
            target.setQuaternion(base[0], base[1], base[2], base[3]);
        } else {
            target.setScale(base[0], base[1], base[2]);
        }
        baseCaptured = false;
    }

    /** Returns the four-component dot product of two quaternion arrays. */
    private static float dot(float[] first, float[] second) {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2] + first[3] * second[3];
    }

    /** Normalizes a non-zero quaternion accumulation without avoidable overflow or underflow. */
    private static void normalize(float[] quaternion) {
        float largest = Math.max(
                Math.max(Math.abs(quaternion[0]), Math.abs(quaternion[1])),
                Math.max(Math.abs(quaternion[2]), Math.abs(quaternion[3])));
        if (largest == 0.0f) {
            throw new IllegalStateException("weighted quaternion blend produced a zero-length result");
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
