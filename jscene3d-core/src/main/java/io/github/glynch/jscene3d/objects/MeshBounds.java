/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import java.util.List;

/** Exact local-space bounds resolved from one mesh's current morph influences. */
record MeshBounds(BoundingBox box, BoundingSphere sphere) {
    /** Computes bounds for an ordinary mesh or one independently morphed instance. */
    static MeshBounds compute(Mesh mesh, int instanceIndex) {
        BufferGeometry geometry = mesh.geometry();
        BufferAttribute positions = geometry.attribute(BufferGeometry.POSITION);
        if (positions == null || positions.count() == 0) {
            throw new IllegalStateException("Mesh bounds require non-empty positions");
        }
        if (mesh.morphTargetCount() == 0) {
            return staticBounds(geometry);
        }
        return computeMorphed(mesh, positions, geometry.morphTargets(), instanceIndex);
    }

    /** Reuses geometry-owned static bounds when no morph deformation is active. */
    private static MeshBounds staticBounds(BufferGeometry geometry) {
        BoundingBox box = geometry.boundingBox();
        if (box == null) {
            box = geometry.computeBoundingBox();
        }
        BoundingSphere sphere = geometry.boundingSphere();
        if (sphere == null) {
            sphere = geometry.computeBoundingSphere();
        }
        return new MeshBounds(box, sphere);
    }

    /** Resolves every deformed vertex twice: once for its box and once for its sphere. */
    private static MeshBounds computeMorphed(
            Mesh mesh, BufferAttribute positions, List<MorphTarget> targets, int instanceIndex) {
        MutableExtents extents = new MutableExtents();
        for (int vertexIndex = 0; vertexIndex < positions.count(); vertexIndex++) {
            extents.include(resolvedPosition(mesh, positions, targets, instanceIndex, vertexIndex));
        }
        BoundingBox box = extents.toBox();
        double centerX = ((double) box.minimum().x() + box.maximum().x()) * 0.5;
        double centerY = ((double) box.minimum().y() + box.maximum().y()) * 0.5;
        double centerZ = ((double) box.minimum().z() + box.maximum().z()) * 0.5;
        double maximumDistanceSquared = 0.0;
        for (int vertexIndex = 0; vertexIndex < positions.count(); vertexIndex++) {
            ResolvedPosition position = resolvedPosition(mesh, positions, targets, instanceIndex, vertexIndex);
            double offsetX = position.x() - centerX;
            double offsetY = position.y() - centerY;
            double offsetZ = position.z() - centerZ;
            maximumDistanceSquared =
                    Math.max(maximumDistanceSquared, offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        }
        float radius = (float) Math.sqrt(maximumDistanceSquared);
        if (!Float.isFinite(radius)) {
            throw new IllegalStateException("Computed morphed bounding-sphere radius must be finite");
        }
        return new MeshBounds(box, new BoundingSphere((float) centerX, (float) centerY, (float) centerZ, radius));
    }

    /** Resolves one base position plus every weighted relative target displacement. */
    private static ResolvedPosition resolvedPosition(
            Mesh mesh, BufferAttribute positions, List<MorphTarget> targets, int instanceIndex, int vertexIndex) {
        double x = positions.value(vertexIndex, 0);
        double y = positions.value(vertexIndex, 1);
        double z = positions.value(vertexIndex, 2);
        for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
            float influence = instanceIndex < 0
                    ? mesh.morphTargetInfluence(targetIndex)
                    : ((InstancedMesh) mesh).morphTargetInfluenceAt(instanceIndex, targetIndex);
            BufferAttribute deltas = targets.get(targetIndex).positions();
            x += deltas.value(vertexIndex, 0) * influence;
            y += deltas.value(vertexIndex, 1) * influence;
            z += deltas.value(vertexIndex, 2) * influence;
        }
        return new ResolvedPosition(x, y, z);
    }

    /** One resolved position retained only during a bounds computation. */
    private record ResolvedPosition(double x, double y, double z) {}

    /** Mutable extrema accumulator kept private to the calculation. */
    private static final class MutableExtents {
        private double minimumX = Double.POSITIVE_INFINITY;
        private double minimumY = Double.POSITIVE_INFINITY;
        private double minimumZ = Double.POSITIVE_INFINITY;
        private double maximumX = Double.NEGATIVE_INFINITY;
        private double maximumY = Double.NEGATIVE_INFINITY;
        private double maximumZ = Double.NEGATIVE_INFINITY;

        /** Includes one finite resolved position. */
        private void include(ResolvedPosition position) {
            minimumX = Math.min(minimumX, position.x());
            minimumY = Math.min(minimumY, position.y());
            minimumZ = Math.min(minimumZ, position.z());
            maximumX = Math.max(maximumX, position.x());
            maximumY = Math.max(maximumY, position.y());
            maximumZ = Math.max(maximumZ, position.z());
        }

        /** Creates immutable, float-representable bounds. */
        private BoundingBox toBox() {
            return new BoundingBox(
                    finiteFloat(minimumX),
                    finiteFloat(minimumY),
                    finiteFloat(minimumZ),
                    finiteFloat(maximumX),
                    finiteFloat(maximumY),
                    finiteFloat(maximumZ));
        }

        /** Narrows one coordinate while rejecting overflow caused by extreme influences. */
        private static float finiteFloat(double value) {
            float narrowed = (float) value;
            if (!Float.isFinite(narrowed)) {
                throw new IllegalStateException("Computed morphed bounds must be finite");
            }
            return narrowed;
        }
    }
}
