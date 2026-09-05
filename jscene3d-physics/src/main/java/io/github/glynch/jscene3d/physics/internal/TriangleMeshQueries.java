/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.Objects;
import java.util.Optional;
import org.joml.Intersectionf;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Narrow-phase queries between convex shapes or rays and one static triangle mesh. */
final class TriangleMeshQueries {
    private static final float EPSILON = 1.0E-6F;
    private static final Vector3f[] BOX_AXES = {
        new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(0.0F, 0.0F, 1.0F)
    };
    private static final int[][] BOX_EDGES = {
        {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
        {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

    private TriangleMeshQueries() {
        throw new AssertionError("TriangleMeshQueries cannot be instantiated");
    }

    /** Finds the nearest double-sided ray intersection. */
    static Optional<RayHitResult> raycast(
            Vector3fc origin, Vector3fc direction, float maximumDistance, TriangleMeshShape mesh, ShapePose meshPose) {
        Quaternionf inverse = meshPose.orientation().invert(new Quaternionf());
        Vector3f localOrigin = new Vector3f(origin).sub(meshPose.position()).rotate(inverse);
        Vector3f localDirection = new Vector3f(direction).rotate(inverse);
        RayCandidate nearest = new RayCandidate(maximumDistance);
        forEachTriangle(mesh, triangle -> nearest.include(localOrigin, localDirection, triangle));
        return nearest.result(origin, direction, meshPose);
    }

    /** Finds the deepest contact between one convex shape and the mesh. */
    static Optional<ContactResult> contact(ShapePose convexPose, TriangleMeshShape mesh, ShapePose meshPose) {
        ContactCandidate deepest = new ContactCandidate();
        forEachWorldTriangle(mesh, meshPose, triangle -> deepest.include(contact(convexPose, triangle)));
        return deepest.result();
    }

    /** Finds the closest separation between one convex shape and the mesh. */
    static SeparationResult separation(ShapePose convexPose, TriangleMeshShape mesh, ShapePose meshPose) {
        SeparationCandidate closest = new SeparationCandidate();
        forEachWorldTriangle(mesh, meshPose, triangle -> closest.include(separation(convexPose, triangle)));
        return closest.result();
    }

    /** Dispatches contact to the supported convex query shapes. */
    private static Optional<ContactResult> contact(ShapePose pose, Triangle triangle) {
        return switch (pose.shape()) {
            case SphereShape sphere -> sphereContact(sphere, pose, triangle);
            case CapsuleShape capsule -> capsuleContact(capsule, pose, triangle);
            case BoxShape box -> boxContact(box, pose, triangle);
            case TriangleMeshShape ignored -> throw new IllegalArgumentException("triangle mesh pairs are unsupported");
        };
    }

    /** Dispatches separation to the supported convex query shapes. */
    private static SeparationResult separation(ShapePose pose, Triangle triangle) {
        return switch (pose.shape()) {
            case SphereShape sphere -> sphereSeparation(sphere, pose, triangle);
            case CapsuleShape capsule -> capsuleSeparation(capsule, pose, triangle);
            case BoxShape box -> boxSeparation(box, pose, triangle);
            case TriangleMeshShape ignored -> throw new IllegalArgumentException("triangle mesh pairs are unsupported");
        };
    }

    /** Tests one sphere against one triangle. */
    private static Optional<ContactResult> sphereContact(SphereShape sphere, ShapePose pose, Triangle triangle) {
        Vector3f point = triangle.closestPoint(pose.position());
        Vector3f offset = new Vector3f(pose.position()).sub(point);
        float distance = offset.length();
        if (distance > sphere.radius() + EPSILON) {
            return Optional.empty();
        }
        Vector3f normal = triangle.normalToward(offset);
        return Optional.of(new ContactResult(sphere.radius() - distance, normal, point));
    }

    /** Calculates sphere separation from one triangle. */
    private static SeparationResult sphereSeparation(SphereShape sphere, ShapePose pose, Triangle triangle) {
        Vector3f point = triangle.closestPoint(pose.position());
        Vector3f offset = new Vector3f(pose.position()).sub(point);
        float distance = offset.length();
        return new SeparationResult(distance - sphere.radius(), triangle.normalToward(offset), point);
    }

    /** Tests one capsule against one triangle. */
    private static Optional<ContactResult> capsuleContact(CapsuleShape capsule, ShapePose pose, Triangle triangle) {
        SegmentTrianglePoints points = closestCapsuleAxis(capsule, pose, triangle);
        float distance = (float) Math.sqrt(points.distanceSquared());
        if (distance > capsule.radius() + EPSILON) {
            return Optional.empty();
        }
        Vector3f offset = new Vector3f(points.segmentPoint()).sub(points.trianglePoint());
        return Optional.of(
                new ContactResult(capsule.radius() - distance, triangle.normalToward(offset), points.trianglePoint()));
    }

    /** Calculates capsule separation from one triangle. */
    private static SeparationResult capsuleSeparation(CapsuleShape capsule, ShapePose pose, Triangle triangle) {
        SegmentTrianglePoints points = closestCapsuleAxis(capsule, pose, triangle);
        float distance = (float) Math.sqrt(points.distanceSquared());
        Vector3f offset = new Vector3f(points.segmentPoint()).sub(points.trianglePoint());
        return new SeparationResult(distance - capsule.radius(), triangle.normalToward(offset), points.trianglePoint());
    }

    /** Finds the closest points between a capsule axis and one triangle. */
    private static SegmentTrianglePoints closestCapsuleAxis(CapsuleShape capsule, ShapePose pose, Triangle triangle) {
        ShapeGeometry.Segment axis = ShapeGeometry.capsuleSegment(capsule, pose);
        return closestSegmentTriangle(axis.start(), axis.end(), triangle);
    }

    /** Tests one oriented box against one triangle using the complete separating-axis set. */
    private static Optional<ContactResult> boxContact(BoxShape box, ShapePose pose, Triangle worldTriangle) {
        Triangle triangle = worldTriangle.toLocal(pose);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        TriangleBoxOverlap overlap = TriangleBoxOverlap.test(triangle, half);
        if (!overlap.intersects()) {
            return Optional.empty();
        }
        Vector3f localPoint = triangle.closestPoint(new Vector3f());
        return Optional.of(new ContactResult(
                overlap.depth(), overlap.normal().rotate(pose.orientation()), ShapeGeometry.toWorld(localPoint, pose)));
    }

    /** Calculates the closest feature pair between one oriented box and one triangle. */
    private static SeparationResult boxSeparation(BoxShape box, ShapePose pose, Triangle worldTriangle) {
        Triangle triangle = worldTriangle.toLocal(pose);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        TriangleBoxOverlap overlap = TriangleBoxOverlap.test(triangle, half);
        if (overlap.intersects()) {
            Vector3f point = triangle.closestPoint(new Vector3f());
            return new SeparationResult(
                    0.0F, overlap.normal().rotate(pose.orientation()), ShapeGeometry.toWorld(point, pose));
        }
        ClosestPair closest = ClosestPair.between(triangle, half);
        Vector3f offset = new Vector3f(closest.boxPoint()).sub(closest.trianglePoint());
        Vector3f normal = normalOrTriangle(offset, triangle);
        return new SeparationResult(
                (float) Math.sqrt(closest.distanceSquared()),
                normal.rotate(pose.orientation()),
                ShapeGeometry.toWorld(closest.trianglePoint(), pose));
    }

    /** Finds the closest points between one segment and triangle. */
    private static SegmentTrianglePoints closestSegmentTriangle(Vector3fc start, Vector3fc end, Triangle triangle) {
        Vector3f segmentPoint = new Vector3f();
        Vector3f trianglePoint = new Vector3f();
        float distanceSquared = Intersectionf.findClosestPointsLineSegmentTriangle(
                start.x(),
                start.y(),
                start.z(),
                end.x(),
                end.y(),
                end.z(),
                triangle.first().x(),
                triangle.first().y(),
                triangle.first().z(),
                triangle.second().x(),
                triangle.second().y(),
                triangle.second().z(),
                triangle.third().x(),
                triangle.third().y(),
                triangle.third().z(),
                segmentPoint,
                trianglePoint);
        return new SegmentTrianglePoints(distanceSquared, segmentPoint, trianglePoint);
    }

    /** Produces one stable normal from a triangle toward a convex feature. */
    private static Vector3f normalOrTriangle(Vector3f offset, Triangle triangle) {
        return offset.lengthSquared() > EPSILON ? offset.normalize() : triangle.normalToward(offset);
    }

    /** Visits each local-space indexed triangle without exposing mesh arrays. */
    private static void forEachTriangle(TriangleMeshShape mesh, TriangleConsumer consumer) {
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
            int offset = triangle * 3;
            mesh.vertex(mesh.index(offset), first);
            mesh.vertex(mesh.index(offset + 1), second);
            mesh.vertex(mesh.index(offset + 2), third);
            consumer.accept(new Triangle(first, second, third));
        }
    }

    /** Visits each indexed triangle after applying the mesh pose. */
    private static void forEachWorldTriangle(TriangleMeshShape mesh, ShapePose meshPose, TriangleConsumer consumer) {
        forEachTriangle(mesh, triangle -> consumer.accept(triangle.toWorld(meshPose)));
    }

    /** Receives one immutable triangle. */
    @FunctionalInterface
    private interface TriangleConsumer {
        void accept(Triangle triangle);
    }

    /** Three copied triangle vertices with local geometric operations. */
    private record Triangle(Vector3f first, Vector3f second, Vector3f third) {
        Triangle {
            first = new Vector3f(first);
            second = new Vector3f(second);
            third = new Vector3f(third);
        }

        Vector3f closestPoint(Vector3fc point) {
            Vector3f result = new Vector3f();
            Intersectionf.findClosestPointOnTriangle(first, second, third, point, result);
            return result;
        }

        Vector3f normalToward(Vector3fc offset) {
            Vector3f normal = new Vector3f(second)
                    .sub(first)
                    .cross(new Vector3f(third).sub(first))
                    .normalize();
            if (normal.dot(offset) < 0.0F) {
                normal.negate();
            }
            return normal;
        }

        Triangle toWorld(ShapePose pose) {
            return new Triangle(
                    ShapeGeometry.toWorld(first, pose),
                    ShapeGeometry.toWorld(second, pose),
                    ShapeGeometry.toWorld(third, pose));
        }

        Triangle toLocal(ShapePose pose) {
            Quaternionf inverse = pose.orientation().invert(new Quaternionf());
            return new Triangle(
                    new Vector3f(first).sub(pose.position()).rotate(inverse),
                    new Vector3f(second).sub(pose.position()).rotate(inverse),
                    new Vector3f(third).sub(pose.position()).rotate(inverse));
        }
    }

    /** Accumulates the closest ray hit without allocating result objects per triangle. */
    private static final class RayCandidate {
        private float distance;
        private @Nullable Vector3f normal;

        RayCandidate(float maximumDistance) {
            distance = maximumDistance + EPSILON;
        }

        void include(Vector3fc origin, Vector3fc direction, Triangle triangle) {
            float candidate = Intersectionf.intersectRayTriangle(
                    origin, direction, triangle.first(), triangle.second(), triangle.third(), EPSILON);
            if (candidate < 0.0F || candidate >= distance) {
                return;
            }
            distance = candidate;
            normal = triangle.normalToward(new Vector3f(direction).negate());
        }

        Optional<RayHitResult> result(Vector3fc origin, Vector3fc direction, ShapePose pose) {
            Vector3f hitNormal = normal;
            if (hitNormal == null) {
                return Optional.empty();
            }
            Vector3f point = new Vector3f(direction).mul(distance).add(origin);
            return Optional.of(new RayHitResult(distance, point, hitNormal.rotate(pose.orientation())));
        }
    }

    /** Accumulates the deepest mesh contact. */
    private static final class ContactCandidate {
        private Optional<ContactResult> deepest = Optional.empty();

        void include(Optional<ContactResult> candidate) {
            if (candidate.isPresent()
                    && (deepest.isEmpty()
                            || candidate.orElseThrow().penetrationDepth()
                                    > deepest.orElseThrow().penetrationDepth())) {
                deepest = candidate;
            }
        }

        Optional<ContactResult> result() {
            return deepest;
        }
    }

    /** Accumulates the nearest triangle separation. */
    private static final class SeparationCandidate {
        private @Nullable SeparationResult closest;

        void include(SeparationResult candidate) {
            if (closest == null || candidate.distance() < closest.distance()) {
                closest = candidate;
            }
        }

        SeparationResult result() {
            return Objects.requireNonNull(closest, "triangle mesh must contain a triangle");
        }
    }

    /** Closest points between a segment and triangle. */
    private record SegmentTrianglePoints(float distanceSquared, Vector3f segmentPoint, Vector3f trianglePoint) {}

    /** SAT result for one triangle and origin-centered AABB. */
    private static final class TriangleBoxOverlap {
        private boolean intersects = true;
        private float depth = Float.POSITIVE_INFINITY;
        private final Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

        static TriangleBoxOverlap test(Triangle triangle, Vector3fc half) {
            TriangleBoxOverlap result = new TriangleBoxOverlap();
            for (Vector3f axis : BOX_AXES) {
                result.include(axis, triangle, half);
            }
            Vector3f firstEdge = new Vector3f(triangle.second()).sub(triangle.first());
            Vector3f secondEdge = new Vector3f(triangle.third()).sub(triangle.second());
            Vector3f thirdEdge = new Vector3f(triangle.first()).sub(triangle.third());
            Vector3f faceNormal = new Vector3f(firstEdge).cross(new Vector3f(triangle.third()).sub(triangle.first()));
            result.include(faceNormal, triangle, half);
            for (Vector3f edge : new Vector3f[] {firstEdge, secondEdge, thirdEdge}) {
                for (Vector3f boxAxis : BOX_AXES) {
                    result.include(new Vector3f(edge).cross(boxAxis), triangle, half);
                }
            }
            return result;
        }

        void include(Vector3f axis, Triangle triangle, Vector3fc half) {
            if (!intersects || axis.lengthSquared() <= EPSILON) {
                return;
            }
            axis.normalize();
            float first = axis.dot(triangle.first());
            float second = axis.dot(triangle.second());
            float third = axis.dot(triangle.third());
            float minimum = Math.min(first, Math.min(second, third));
            float maximum = Math.max(first, Math.max(second, third));
            float radius = Math.abs(axis.x) * half.x() + Math.abs(axis.y) * half.y() + Math.abs(axis.z) * half.z();
            float overlap = Math.min(radius - minimum, maximum + radius);
            if (overlap < -EPSILON) {
                intersects = false;
            } else if (overlap < depth) {
                depth = Math.max(0.0F, overlap);
                normal.set(axis);
                Vector3f triangleCenter = new Vector3f(triangle.first())
                        .add(triangle.second())
                        .add(triangle.third())
                        .div(3.0F);
                if (normal.dot(new Vector3f(triangleCenter).negate()) < 0.0F) {
                    normal.negate();
                }
            }
        }

        boolean intersects() {
            return intersects;
        }

        float depth() {
            return depth;
        }

        Vector3f normal() {
            return new Vector3f(normal);
        }
    }

    /** Closest local-space box and triangle feature points. */
    private static final class ClosestPair {
        private float distanceSquared = Float.POSITIVE_INFINITY;
        private final Vector3f boxPoint = new Vector3f();
        private final Vector3f trianglePoint = new Vector3f();

        static ClosestPair between(Triangle triangle, Vector3fc half) {
            ClosestPair result = new ClosestPair();
            result.includeTriangleVertices(triangle, half);
            Vector3f[] vertices = boxVertices(half);
            result.includeBoxVertices(vertices, triangle);
            result.includeBoxEdges(vertices, triangle);
            return result;
        }

        void includeTriangleVertices(Triangle triangle, Vector3fc half) {
            includeTriangleVertex(triangle.first(), half);
            includeTriangleVertex(triangle.second(), half);
            includeTriangleVertex(triangle.third(), half);
        }

        void includeTriangleVertex(Vector3fc vertex, Vector3fc half) {
            Vector3f onBox = new Vector3f(
                    Math.clamp(vertex.x(), -half.x(), half.x()),
                    Math.clamp(vertex.y(), -half.y(), half.y()),
                    Math.clamp(vertex.z(), -half.z(), half.z()));
            include(onBox, vertex);
        }

        void includeBoxVertices(Vector3f[] vertices, Triangle triangle) {
            for (Vector3f vertex : vertices) {
                include(vertex, triangle.closestPoint(vertex));
            }
        }

        void includeBoxEdges(Vector3f[] vertices, Triangle triangle) {
            for (int[] edge : BOX_EDGES) {
                SegmentTrianglePoints points = closestSegmentTriangle(vertices[edge[0]], vertices[edge[1]], triangle);
                include(points.segmentPoint(), points.trianglePoint());
            }
        }

        void include(Vector3fc candidateBoxPoint, Vector3fc candidateTrianglePoint) {
            float candidateDistance = candidateBoxPoint.distanceSquared(candidateTrianglePoint);
            if (candidateDistance < distanceSquared) {
                distanceSquared = candidateDistance;
                boxPoint.set(candidateBoxPoint);
                trianglePoint.set(candidateTrianglePoint);
            }
        }

        float distanceSquared() {
            return distanceSquared;
        }

        Vector3f boxPoint() {
            return new Vector3f(boxPoint);
        }

        Vector3f trianglePoint() {
            return new Vector3f(trianglePoint);
        }

        private static Vector3f[] boxVertices(Vector3fc half) {
            Vector3f[] vertices = new Vector3f[8];
            for (int index = 0; index < vertices.length; index++) {
                vertices[index] = new Vector3f(
                        (index & 1) == 0 ? -half.x() : half.x(),
                        (index & 2) == 0 ? -half.y() : half.y(),
                        (index & 4) == 0 ? -half.z() : half.z());
            }
            return vertices;
        }
    }
}
