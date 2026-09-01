/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.objects.Mesh;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Reusable CPU-side broad- and narrow-phase triangle intersection engine. */
final class MeshIntersector {
    private static final double PARALLEL_TOLERANCE = 1.0e-12;

    private final Matrix4f inverseWorldMatrix = new Matrix4f();
    private final Vector3f localOrigin = new Vector3f();
    private final Vector3f localDirection = new Vector3f();
    private final Vector3f worldPoint = new Vector3f();
    private final Vector2f textureCoordinate = new Vector2f();

    private @Nullable Mesh activeMesh;
    private @Nullable RayState activeRay;
    private @Nullable BufferAttribute activePositions;
    private @Nullable BufferAttribute activeTextureCoordinates;
    private @Nullable MaterialSide activeSide;
    private @Nullable List<RaycastHit> activeHits;
    private double boxNear;
    private double boxFar;

    /** Appends all selected triangle intersections for one visible mesh. */
    void intersect(Mesh mesh, RayState ray, List<RaycastHit> hits) {
        Material material = mesh.material();
        if (!material.visible()) {
            return;
        }
        BufferGeometry geometry = mesh.geometry();
        int drawCount = geometry.drawRangeCount();
        if (drawCount < 3) {
            return;
        }
        BufferAttribute positions = requirePositions(geometry);
        Matrix4fc worldMatrix = mesh.matrixWorld();
        transformRayToLocal(worldMatrix, ray);
        if (!intersectsBounds(geometry)) {
            return;
        }
        MaterialSide side = effectiveSide(material.side(), worldMatrix.determinant3x3());
        BufferAttribute uvs = geometry.attribute(BufferGeometry.UV);
        configureTriangleIntersection(mesh, ray, positions, uvs, side, hits);
        intersectTriangles(geometry, drawCount);
    }

    /** Returns the required non-empty position attribute. */
    private static BufferAttribute requirePositions(BufferGeometry geometry) {
        BufferAttribute positions = geometry.attribute(BufferGeometry.POSITION);
        if (positions == null || positions.count() == 0) {
            throw new IllegalStateException("Raycast mesh geometry must contain positions");
        }
        return positions;
    }

    /** Transforms the ray into mesh-local space after validating the world transform. */
    private void transformRayToLocal(Matrix4fc worldMatrix, RayState ray) {
        float determinant = worldMatrix.determinant();
        if (!worldMatrix.isFinite() || !Float.isFinite(determinant) || determinant == 0.0f) {
            throw new IllegalStateException("Mesh world transform must be finite and invertible for raycasting");
        }
        worldMatrix.invert(inverseWorldMatrix);
        if (!inverseWorldMatrix.isFinite()) {
            throw new IllegalStateException("Mesh world transform must have a finite inverse for raycasting");
        }
        inverseWorldMatrix.transformPosition(ray.origin, localOrigin);
        inverseWorldMatrix.transformDirection(ray.direction, localDirection);
    }

    /** Returns whether the local ray reaches the geometry's available bounds. */
    private boolean intersectsBounds(BufferGeometry geometry) {
        BoundingSphere sphere = geometry.boundingSphere();
        if (sphere == null) {
            sphere = geometry.computeBoundingSphere();
        }
        if (!intersectsSphere(sphere)) {
            return false;
        }
        BoundingBox box = geometry.boundingBox();
        return box == null || intersectsBox(box);
    }

    /** Intersects every selected triangle after broad-phase bounds acceptance. */
    private void intersectTriangles(BufferGeometry geometry, int drawCount) {
        IndexBuffer indices = geometry.index();
        int drawStart = geometry.drawRangeStart();
        int drawEnd = drawStart + drawCount;
        try {
            for (int elementIndex = drawStart; elementIndex + 2 < drawEnd; elementIndex += 3) {
                int first = indices == null ? elementIndex : indices.value(elementIndex);
                int second = indices == null ? elementIndex + 1 : indices.value(elementIndex + 1);
                int third = indices == null ? elementIndex + 2 : indices.value(elementIndex + 2);
                intersectTriangle(first, second, third, elementIndex / 3);
            }
        } finally {
            releaseTriangleIntersection();
        }
    }

    /** Retains the mesh-level values shared by every selected triangle. */
    private void configureTriangleIntersection(
            Mesh mesh,
            RayState ray,
            BufferAttribute positions,
            @Nullable BufferAttribute uvs,
            MaterialSide side,
            List<RaycastHit> hits) {
        activeMesh = mesh;
        activeRay = ray;
        activePositions = positions;
        activeTextureCoordinates = uvs;
        activeSide = side;
        activeHits = hits;
    }

    /** Releases mesh-level references after one narrow-phase pass. */
    private void releaseTriangleIntersection() {
        activeMesh = null;
        activeRay = null;
        activePositions = null;
        activeTextureCoordinates = null;
        activeSide = null;
        activeHits = null;
    }

    /** Swaps front and back selection when a world transform reverses triangle winding. */
    private static MaterialSide effectiveSide(MaterialSide side, float determinant) {
        if (determinant >= 0.0f || side == MaterialSide.DOUBLE) {
            return side;
        }
        return side == MaterialSide.FRONT ? MaterialSide.BACK : MaterialSide.FRONT;
    }

    /** Rejects a local ray that misses the geometry's local spherical bounds. */
    private boolean intersectsSphere(BoundingSphere sphere) {
        double offsetX = localOrigin.x() - sphere.center().x();
        double offsetY = localOrigin.y() - sphere.center().y();
        double offsetZ = localOrigin.z() - sphere.center().z();
        double directionX = localDirection.x();
        double directionY = localDirection.y();
        double directionZ = localDirection.z();
        double directionLengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        double projection = offsetX * directionX + offsetY * directionY + offsetZ * directionZ;
        double radius = sphere.radius();
        double separation = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ - radius * radius;
        if (separation <= 0.0) {
            return true;
        }
        return projection <= 0.0 && projection * projection - directionLengthSquared * separation >= 0.0;
    }

    /** Rejects a local ray that misses supplied local box bounds. */
    private boolean intersectsBox(BoundingBox box) {
        boxNear = 0.0;
        boxFar = Double.POSITIVE_INFINITY;
        return includeBoxAxis(
                        localOrigin.x(),
                        localDirection.x(),
                        box.minimum().x(),
                        box.maximum().x())
                && includeBoxAxis(
                        localOrigin.y(),
                        localDirection.y(),
                        box.minimum().y(),
                        box.maximum().y())
                && includeBoxAxis(
                        localOrigin.z(),
                        localDirection.z(),
                        box.minimum().z(),
                        box.maximum().z());
    }

    /** Narrows the retained forward-ray interval against one axis-aligned slab. */
    private boolean includeBoxAxis(double origin, double direction, double minimum, double maximum) {
        if (direction == 0.0) {
            return origin >= minimum && origin <= maximum;
        }
        double first = (minimum - origin) / direction;
        double second = (maximum - origin) / direction;
        if (first > second) {
            double temporary = first;
            first = second;
            second = temporary;
        }
        boxNear = Math.max(boxNear, first);
        boxFar = Math.min(boxFar, second);
        return boxFar >= boxNear;
    }

    /** Appends one narrow-phase triangle hit when side and barycentric tests pass. */
    private void intersectTriangle(int first, int second, int third, int faceIndex) {
        RayState ray = Objects.requireNonNull(activeRay, "activeRay");
        BufferAttribute positions = Objects.requireNonNull(activePositions, "activePositions");
        MaterialSide side = Objects.requireNonNull(activeSide, "activeSide");
        double ax = positions.value(first, 0);
        double ay = positions.value(first, 1);
        double az = positions.value(first, 2);
        double edgeOneX = positions.value(second, 0) - ax;
        double edgeOneY = positions.value(second, 1) - ay;
        double edgeOneZ = positions.value(second, 2) - az;
        double edgeTwoX = positions.value(third, 0) - ax;
        double edgeTwoY = positions.value(third, 1) - ay;
        double edgeTwoZ = positions.value(third, 2) - az;

        double directionX = localDirection.x();
        double directionY = localDirection.y();
        double directionZ = localDirection.z();
        double crossX = directionY * edgeTwoZ - directionZ * edgeTwoY;
        double crossY = directionZ * edgeTwoX - directionX * edgeTwoZ;
        double crossZ = directionX * edgeTwoY - directionY * edgeTwoX;
        double determinant = edgeOneX * crossX + edgeOneY * crossY + edgeOneZ * crossZ;
        double directionLengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        double edgeOneLengthSquared = edgeOneX * edgeOneX + edgeOneY * edgeOneY + edgeOneZ * edgeOneZ;
        double edgeTwoLengthSquared = edgeTwoX * edgeTwoX + edgeTwoY * edgeTwoY + edgeTwoZ * edgeTwoZ;
        double determinantTolerance =
                determinantTolerance(directionLengthSquared, edgeOneLengthSquared, edgeTwoLengthSquared);
        if (!acceptsDeterminant(side, determinant, determinantTolerance)) {
            return;
        }

        double inverseDeterminant = 1.0 / determinant;
        double originOffsetX = localOrigin.x() - ax;
        double originOffsetY = localOrigin.y() - ay;
        double originOffsetZ = localOrigin.z() - az;
        double secondWeight =
                (originOffsetX * crossX + originOffsetY * crossY + originOffsetZ * crossZ) * inverseDeterminant;
        if (secondWeight < 0.0 || secondWeight > 1.0) {
            return;
        }

        double perpendicularX = originOffsetY * edgeOneZ - originOffsetZ * edgeOneY;
        double perpendicularY = originOffsetZ * edgeOneX - originOffsetX * edgeOneZ;
        double perpendicularZ = originOffsetX * edgeOneY - originOffsetY * edgeOneX;
        double thirdWeight = (directionX * perpendicularX + directionY * perpendicularY + directionZ * perpendicularZ)
                * inverseDeterminant;
        if (thirdWeight < 0.0 || secondWeight + thirdWeight > 1.0) {
            return;
        }

        double distance = (edgeTwoX * perpendicularX + edgeTwoY * perpendicularY + edgeTwoZ * perpendicularZ)
                * inverseDeterminant;
        if (distance < 0.0 || distance > Float.MAX_VALUE) {
            return;
        }
        float hitDistance = (float) distance;
        worldPoint.set(
                (float) (ray.origin.x() + ray.direction.x() * distance),
                (float) (ray.origin.y() + ray.direction.y() * distance),
                (float) (ray.origin.z() + ray.direction.z() * distance));
        Objects.requireNonNull(activeHits, "activeHits")
                .add(new RaycastHit(
                        Objects.requireNonNull(activeMesh, "activeMesh"),
                        hitDistance,
                        worldPoint,
                        faceIndex,
                        interpolateTextureCoordinate(
                                activeTextureCoordinates, first, second, third, secondWeight, thirdWeight)));
    }

    /** Computes a scale-aware determinant tolerance for one ray and triangle. */
    private static double determinantTolerance(
            double directionLengthSquared, double edgeOneLengthSquared, double edgeTwoLengthSquared) {
        return PARALLEL_TOLERANCE * Math.sqrt(directionLengthSquared * edgeOneLengthSquared * edgeTwoLengthSquared);
    }

    /** Applies front-, back-, or double-sided face selection to one determinant. */
    private static boolean acceptsDeterminant(MaterialSide side, double determinant, double tolerance) {
        return switch (side) {
            case FRONT -> determinant > tolerance;
            case BACK -> determinant < -tolerance;
            case DOUBLE -> Math.abs(determinant) > tolerance;
        };
    }

    /** Interpolates optional UV data from the triangle barycentric weights. */
    private Vector2f interpolateTextureCoordinate(
            @Nullable BufferAttribute uvs, int first, int second, int third, double secondWeight, double thirdWeight) {
        if (uvs == null) {
            return null;
        }
        double firstWeight = 1.0 - secondWeight - thirdWeight;
        double u = uvs.value(first, 0) * firstWeight
                + uvs.value(second, 0) * secondWeight
                + uvs.value(third, 0) * thirdWeight;
        double v = uvs.itemSize() < 2
                ? 0.0
                : uvs.value(first, 1) * firstWeight
                        + uvs.value(second, 1) * secondWeight
                        + uvs.value(third, 1) * thirdWeight;
        return textureCoordinate.set((float) u, (float) v);
    }
}
