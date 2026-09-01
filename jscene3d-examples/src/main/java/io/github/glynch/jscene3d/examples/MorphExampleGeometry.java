/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import java.util.Objects;

/** Builds the shared three-target sphere used by the morph examples. */
final class MorphExampleGeometry {
    private MorphExampleGeometry() {
        throw new AssertionError("MorphExampleGeometry cannot be instantiated");
    }

    /** Creates a sphere with stretch, flatten, and twist displacement targets. */
    static BufferGeometry create() {
        BufferGeometry geometry = SphereGeometry.create(1.0f, 36, 24);
        BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
        BufferAttribute normals = Objects.requireNonNull(geometry.attribute(BufferGeometry.NORMAL));
        geometry.addMorphTarget(new MorphTarget(
                "stretch",
                targetPositions(positions, Deformation.STRETCH),
                targetNormals(normals, Deformation.STRETCH)));
        geometry.addMorphTarget(new MorphTarget(
                "flatten",
                targetPositions(positions, Deformation.FLATTEN),
                targetNormals(normals, Deformation.FLATTEN)));
        geometry.addMorphTarget(new MorphTarget(
                "twist", targetPositions(positions, Deformation.TWIST), targetNormals(normals, Deformation.TWIST)));
        return geometry;
    }

    /** Calculates relative target positions from the immutable base sphere. */
    private static BufferAttribute targetPositions(BufferAttribute source, Deformation deformation) {
        float[] deltas = new float[source.count() * 3];
        for (int vertex = 0; vertex < source.count(); vertex++) {
            float x = source.value(vertex, 0);
            float y = source.value(vertex, 1);
            float z = source.value(vertex, 2);
            int offset = vertex * 3;
            deltas[offset] = deformation.x(x, y, z) - x;
            deltas[offset + 1] = deformation.y(x, y, z) - y;
            deltas[offset + 2] = deformation.z(x, y, z) - z;
        }
        return BufferAttribute.of(deltas, 3);
    }

    /** Calculates approximate relative target normals and normalizes the transformed result. */
    private static BufferAttribute targetNormals(BufferAttribute source, Deformation deformation) {
        float[] deltas = new float[source.count() * 3];
        for (int vertex = 0; vertex < source.count(); vertex++) {
            float x = source.value(vertex, 0);
            float y = source.value(vertex, 1);
            float z = source.value(vertex, 2);
            float transformedX = deformation.normalX(x, y, z);
            float transformedY = deformation.normalY(x, y, z);
            float transformedZ = deformation.normalZ(x, y, z);
            float lengthSquared =
                    transformedX * transformedX + transformedY * transformedY + transformedZ * transformedZ;
            if (lengthSquared == 0.0f) {
                throw new IllegalStateException("Morph example produced a zero-length normal");
            }
            float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
            int offset = vertex * 3;
            deltas[offset] = transformedX * inverseLength - x;
            deltas[offset + 1] = transformedY * inverseLength - y;
            deltas[offset + 2] = transformedZ * inverseLength - z;
        }
        return BufferAttribute.of(deltas, 3);
    }

    /** Deterministic deformation formulas shared by position and normal generation. */
    private enum Deformation {
        STRETCH {
            @Override
            float x(float x, float y, float z) {
                return x * 0.72f;
            }

            @Override
            float y(float x, float y, float z) {
                return y * 1.65f;
            }

            @Override
            float z(float x, float y, float z) {
                return z * 0.72f;
            }

            @Override
            float normalX(float x, float y, float z) {
                return x / 0.72f;
            }

            @Override
            float normalY(float x, float y, float z) {
                return y / 1.65f;
            }

            @Override
            float normalZ(float x, float y, float z) {
                return z / 0.72f;
            }
        },
        FLATTEN {
            @Override
            float x(float x, float y, float z) {
                return x * 1.35f;
            }

            @Override
            float y(float x, float y, float z) {
                return y * 0.38f;
            }

            @Override
            float z(float x, float y, float z) {
                return z * 1.35f;
            }

            @Override
            float normalX(float x, float y, float z) {
                return x / 1.35f;
            }

            @Override
            float normalY(float x, float y, float z) {
                return y / 0.38f;
            }

            @Override
            float normalZ(float x, float y, float z) {
                return z / 1.35f;
            }
        },
        TWIST {
            @Override
            float x(float x, float y, float z) {
                float angle = y * 1.15f;
                return x * (float) Math.cos(angle) - z * (float) Math.sin(angle);
            }

            @Override
            float y(float x, float y, float z) {
                return y;
            }

            @Override
            float z(float x, float y, float z) {
                float angle = y * 1.15f;
                return x * (float) Math.sin(angle) + z * (float) Math.cos(angle);
            }

            @Override
            float normalX(float x, float y, float z) {
                return x(x, y, z);
            }

            @Override
            float normalY(float x, float y, float z) {
                return y;
            }

            @Override
            float normalZ(float x, float y, float z) {
                return z(x, y, z);
            }
        };

        abstract float x(float x, float y, float z);

        abstract float y(float x, float y, float z);

        abstract float z(float x, float y, float z);

        abstract float normalX(float x, float y, float z);

        abstract float normalY(float x, float y, float z);

        abstract float normalZ(float x, float y, float z);
    }
}
