/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Pairwise separation calculations used by conservative shape casts. */
final class SeparationTests {
    private static final float AXIS_EPSILON = 1.0E-8F;

    private SeparationTests() {}

    static SeparationResult between(ShapePose first, ShapePose second) {
        if (second.shape() instanceof TriangleMeshShape mesh) {
            return TriangleMeshQueries.separation(first, mesh, second);
        }
        if (first.shape() instanceof TriangleMeshShape mesh) {
            return reverse(TriangleMeshQueries.separation(second, mesh, first));
        }
        if (first.shape() instanceof SphereShape firstSphere) {
            return sphereSeparation(firstSphere, first, second);
        }
        if (first.shape() instanceof CapsuleShape firstCapsule) {
            return capsuleSeparation(firstCapsule, first, second);
        }
        return boxSeparation((BoxShape) first.shape(), first, second);
    }

    private static SeparationResult sphereSeparation(SphereShape sphere, ShapePose spherePose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> sphereSphere(sphere, spherePose, other, otherPose);
            case CapsuleShape other -> sphereCapsule(sphere, spherePose, other, otherPose);
            case BoxShape other -> sphereBox(sphere, spherePose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static SeparationResult capsuleSeparation(
            CapsuleShape capsule, ShapePose capsulePose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> reverse(sphereCapsule(other, otherPose, capsule, capsulePose));
            case CapsuleShape other -> capsuleCapsule(capsule, capsulePose, other, otherPose);
            case BoxShape other -> capsuleBox(capsule, capsulePose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static SeparationResult boxSeparation(BoxShape box, ShapePose boxPose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> reverse(sphereBox(other, otherPose, box, boxPose));
            case CapsuleShape other -> reverse(capsuleBox(other, otherPose, box, boxPose));
            case BoxShape other -> boxBox(box, boxPose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static SeparationResult sphereSphere(
            SphereShape first, ShapePose firstPose, SphereShape second, ShapePose secondPose) {
        Vector3f normal =
                new Vector3f(firstPose.position()).sub(secondPose.position()).normalize();
        float distance = firstPose.position().distance(secondPose.position()) - first.radius() - second.radius();
        Vector3f point = new Vector3f(secondPose.position()).fma(second.radius(), normal);
        return new SeparationResult(distance, normal, point);
    }

    private static SeparationResult sphereCapsule(
            SphereShape sphere, ShapePose spherePose, CapsuleShape capsule, ShapePose capsulePose) {
        Vector3f axisPoint =
                ShapeGeometry.closestPoint(spherePose.position(), ShapeGeometry.capsuleSegment(capsule, capsulePose));
        Vector3f normal = new Vector3f(spherePose.position()).sub(axisPoint).normalize();
        float distance = spherePose.position().distance(axisPoint) - sphere.radius() - capsule.radius();
        Vector3f point = axisPoint.fma(capsule.radius(), normal);
        return new SeparationResult(distance, normal, point);
    }

    private static SeparationResult sphereBox(
            SphereShape sphere, ShapePose spherePose, BoxShape box, ShapePose boxPose) {
        Vector3f localCenter = ShapeGeometry.toBoxLocal(spherePose.position(), boxPose);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        Vector3f localPoint =
                new Vector3f(localCenter).max(new Vector3f(half).negate()).min(half);
        Vector3f point = ShapeGeometry.toWorld(localPoint, boxPose);
        Vector3f normal = new Vector3f(spherePose.position()).sub(point).normalize();
        float distance = spherePose.position().distance(point) - sphere.radius();
        return new SeparationResult(distance, normal, point);
    }

    private static SeparationResult capsuleCapsule(
            CapsuleShape first, ShapePose firstPose, CapsuleShape second, ShapePose secondPose) {
        ShapeGeometry.SegmentPoints points = ShapeGeometry.closestPoints(
                ShapeGeometry.capsuleSegment(first, firstPose), ShapeGeometry.capsuleSegment(second, secondPose));
        Vector3f normal = new Vector3f(points.first()).sub(points.second()).normalize();
        float distance = points.first().distance(points.second()) - first.radius() - second.radius();
        Vector3f point = new Vector3f(points.second()).fma(second.radius(), normal);
        return new SeparationResult(distance, normal, point);
    }

    private static SeparationResult capsuleBox(
            CapsuleShape capsule, ShapePose capsulePose, BoxShape box, ShapePose boxPose) {
        ShapeGeometry.SegmentBoxPoints points =
                ShapeGeometry.closestSegmentBox(ShapeGeometry.capsuleSegment(capsule, capsulePose), box, boxPose);
        float centerDistance = (float) Math.sqrt(points.distanceSquared());
        Vector3f normal =
                new Vector3f(points.segmentPoint()).sub(points.boxPoint()).normalize();
        return new SeparationResult(centerDistance - capsule.radius(), normal, points.boxPoint());
    }

    private static SeparationResult boxBox(BoxShape first, ShapePose firstPose, BoxShape second, ShapePose secondPose) {
        BoxProjection firstProjection = new BoxProjection(first, firstPose);
        BoxProjection secondProjection = new BoxProjection(second, secondPose);
        SeparatingAxis separation = new SeparatingAxis();
        testAxes(firstProjection, secondProjection, separation);
        Vector3f normal = separation.normal();
        Vector3f firstPoint = ShapeGeometry.support(firstPose, new Vector3f(normal).negate());
        Vector3f secondPoint = ShapeGeometry.support(secondPose, normal);
        return new SeparationResult(
                separation.distance(), normal, firstPoint.add(secondPoint).mul(0.5F));
    }

    private static void testAxes(BoxProjection first, BoxProjection second, SeparatingAxis separation) {
        for (int axis = 0; axis < 3; axis++) {
            separation.test(first.axis(axis), first, second);
            separation.test(second.axis(axis), first, second);
        }
        for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
            for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                Vector3f cross = first.axis(firstAxis).cross(second.axis(secondAxis), new Vector3f());
                if (cross.lengthSquared() > AXIS_EPSILON) {
                    separation.test(cross.normalize(), first, second);
                }
            }
        }
    }

    private static SeparationResult reverse(SeparationResult separation) {
        return new SeparationResult(separation.distance(), separation.normal().negate(), separation.point());
    }

    private static final class BoxProjection {
        private final Vector3f center;
        private final Vector3f half;
        private final Vector3f[] axes;

        BoxProjection(BoxShape shape, ShapePose pose) {
            center = new Vector3f(pose.position());
            half = new Vector3f(shape.width(), shape.height(), shape.depth()).mul(0.5F);
            axes = new Vector3f[] {
                new Vector3f(1.0F, 0.0F, 0.0F).rotate(pose.orientation()),
                new Vector3f(0.0F, 1.0F, 0.0F).rotate(pose.orientation()),
                new Vector3f(0.0F, 0.0F, 1.0F).rotate(pose.orientation())
            };
        }

        Vector3f axis(int index) {
            return axes[index];
        }

        float radius(Vector3fc axis) {
            return Math.abs(axis.dot(axes[0])) * half.x
                    + Math.abs(axis.dot(axes[1])) * half.y
                    + Math.abs(axis.dot(axes[2])) * half.z;
        }
    }

    private static final class SeparatingAxis {
        private float distance = Float.NEGATIVE_INFINITY;
        private final Vector3f normal = new Vector3f(1.0F, 0.0F, 0.0F);

        void test(Vector3fc axis, BoxProjection first, BoxProjection second) {
            Vector3f centerOffset = new Vector3f(first.center).sub(second.center);
            float signedDistance = centerOffset.dot(axis);
            float gap = Math.abs(signedDistance) - first.radius(axis) - second.radius(axis);
            if (gap > distance) {
                distance = gap;
                normal.set(axis);
                if (signedDistance < 0.0F) {
                    normal.negate();
                }
            }
        }

        float distance() {
            return distance;
        }

        Vector3f normal() {
            return new Vector3f(normal);
        }
    }
}
