/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Computes world-axis-aligned bounds for visible renderable objects in one subtree. */
final class VisibleObjectBounds {
    /** Prevents instantiation of this bounds operation. */
    private VisibleObjectBounds() {
        throw new AssertionError("VisibleObjectBounds cannot be instantiated");
    }

    /**
     * Computes bounds for visible meshes and lines beneath {@code root}.
     *
     * <p>Generated line helpers are excluded so debugging aids do not enlarge the bounds they are
     * intended to describe.
     *
     * @return world-axis-aligned bounds, or {@code null} when no visible renderable geometry exists
     */
    static @Nullable BoundingBox compute(Object3D root) {
        Accumulator accumulator = new Accumulator();
        root.traverseVisible(object -> includeObject(object, accumulator));
        return accumulator.toBoundingBox();
    }

    /** Includes one supported renderable object's transformed local bounds. */
    private static void includeObject(Object3D object, Accumulator accumulator) {
        if (object instanceof GeneratedLineSegmentsHelper) {
            return;
        }
        BufferGeometry geometry =
                switch (object) {
                    case Mesh mesh -> mesh.geometry();
                    case Line line -> line.geometry();
                    default -> null;
                };
        if (geometry == null) {
            return;
        }
        BufferAttribute positions = geometry.attribute(BufferGeometry.POSITION);
        if (positions == null || positions.count() == 0) {
            return;
        }
        BoundingBox localBounds = geometry.boundingBox();
        accumulator.include(localBounds == null ? geometry.computeBoundingBox() : localBounds, object.matrixWorld());
    }

    /** Mutable aggregation state confined to one computation. */
    private static final class Accumulator {
        private final Vector3f transformedCorner = new Vector3f();

        private float minimumX = Float.POSITIVE_INFINITY;
        private float minimumY = Float.POSITIVE_INFINITY;
        private float minimumZ = Float.POSITIVE_INFINITY;
        private float maximumX = Float.NEGATIVE_INFINITY;
        private float maximumY = Float.NEGATIVE_INFINITY;
        private float maximumZ = Float.NEGATIVE_INFINITY;
        private boolean populated;

        /** Expands this accumulator by all eight transformed corners of one local box. */
        void include(BoundingBox bounds, Matrix4fc matrixWorld) {
            float localMinimumX = bounds.minimum().x();
            float localMinimumY = bounds.minimum().y();
            float localMinimumZ = bounds.minimum().z();
            float localMaximumX = bounds.maximum().x();
            float localMaximumY = bounds.maximum().y();
            float localMaximumZ = bounds.maximum().z();
            for (int corner = 0; corner < 8; corner++) {
                float x = (corner & 1) == 0 ? localMinimumX : localMaximumX;
                float y = (corner & 2) == 0 ? localMinimumY : localMaximumY;
                float z = (corner & 4) == 0 ? localMinimumZ : localMaximumZ;
                transformedCorner.set(x, y, z);
                matrixWorld.transformPosition(transformedCorner);
                include(transformedCorner);
            }
        }

        /** Expands this accumulator by one transformed point. */
        private void include(Vector3f point) {
            minimumX = Math.min(minimumX, point.x());
            minimumY = Math.min(minimumY, point.y());
            minimumZ = Math.min(minimumZ, point.z());
            maximumX = Math.max(maximumX, point.x());
            maximumY = Math.max(maximumY, point.y());
            maximumZ = Math.max(maximumZ, point.z());
            populated = true;
        }

        /** Returns immutable bounds after at least one point was included. */
        private @Nullable BoundingBox toBoundingBox() {
            return populated ? new BoundingBox(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ) : null;
        }
    }
}
