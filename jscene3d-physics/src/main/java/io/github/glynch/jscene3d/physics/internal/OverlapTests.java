/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.Optional;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Exact pairwise overlap tests for the supported convex shapes. */
final class OverlapTests {
    private static final float CONTACT_EPSILON = 1.0E-6F;

    private OverlapTests() {}

    static Optional<ContactResult> contact(ShapePose first, ShapePose second) {
        if (second.shape() instanceof TriangleMeshShape mesh) {
            return TriangleMeshQueries.contact(first, mesh, second);
        }
        if (first.shape() instanceof TriangleMeshShape mesh) {
            return reverse(TriangleMeshQueries.contact(second, mesh, first));
        }
        if (first.shape() instanceof SphereShape firstSphere) {
            return sphereContact(firstSphere, first, second);
        }
        if (first.shape() instanceof CapsuleShape firstCapsule) {
            return capsuleContact(firstCapsule, first, second);
        }
        return boxContact((BoxShape) first.shape(), first, second);
    }

    private static Optional<ContactResult> sphereContact(
            SphereShape sphere, ShapePose spherePose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> sphereSphere(sphere, spherePose, other, otherPose);
            case CapsuleShape other -> sphereCapsule(sphere, spherePose, other, otherPose);
            case BoxShape other -> sphereBox(sphere, spherePose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static Optional<ContactResult> capsuleContact(
            CapsuleShape capsule, ShapePose capsulePose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> reverse(sphereCapsule(other, otherPose, capsule, capsulePose));
            case CapsuleShape other -> capsuleCapsule(capsule, capsulePose, other, otherPose);
            case BoxShape other -> capsuleBox(capsule, capsulePose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static Optional<ContactResult> boxContact(BoxShape box, ShapePose boxPose, ShapePose otherPose) {
        return switch (otherPose.shape()) {
            case SphereShape other -> reverse(sphereBox(other, otherPose, box, boxPose));
            case CapsuleShape other -> reverse(capsuleBox(other, otherPose, box, boxPose));
            case BoxShape other -> boxBox(box, boxPose, other, otherPose);
            case TriangleMeshShape ignored -> throw new IllegalStateException("triangle mesh handled before dispatch");
        };
    }

    private static Optional<ContactResult> sphereSphere(
            SphereShape first, ShapePose firstPose, SphereShape second, ShapePose secondPose) {
        Vector3f offset = new Vector3f(firstPose.position()).sub(secondPose.position());
        float distance = offset.length();
        float radii = first.radius() + second.radius();
        if (distance > radii + CONTACT_EPSILON) {
            return Optional.empty();
        }
        Vector3f normal = normalizedOrFallback(offset, new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f point = new Vector3f(secondPose.position()).fma(second.radius(), normal);
        return Optional.of(new ContactResult(radii - distance, normal, point));
    }

    private static Optional<ContactResult> sphereCapsule(
            SphereShape sphere, ShapePose spherePose, CapsuleShape capsule, ShapePose capsulePose) {
        Vector3f axisPoint =
                ShapeGeometry.closestPoint(spherePose.position(), ShapeGeometry.capsuleSegment(capsule, capsulePose));
        Vector3f offset = new Vector3f(spherePose.position()).sub(axisPoint);
        float distance = offset.length();
        float radii = sphere.radius() + capsule.radius();
        if (distance > radii + CONTACT_EPSILON) {
            return Optional.empty();
        }
        Vector3f fallback = new Vector3f(spherePose.position()).sub(capsulePose.position());
        Vector3f normal = normalizedOrFallback(offset, normalizedOrFallback(fallback, new Vector3f(1.0F, 0.0F, 0.0F)));
        Vector3f point = axisPoint.fma(capsule.radius(), normal);
        return Optional.of(new ContactResult(radii - distance, normal, point));
    }

    private static Optional<ContactResult> sphereBox(
            SphereShape sphere, ShapePose spherePose, BoxShape box, ShapePose boxPose) {
        Vector3f localCenter = ShapeGeometry.toBoxLocal(spherePose.position(), boxPose);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        Vector3f closest =
                new Vector3f(localCenter).max(new Vector3f(half).negate()).min(half);
        Vector3f offset = new Vector3f(localCenter).sub(closest);
        float distance = offset.length();
        if (distance > sphere.radius() + CONTACT_EPSILON) {
            return Optional.empty();
        }
        if (distance <= CONTACT_EPSILON) {
            return Optional.of(insideBoxContact(sphere.radius(), localCenter, half, boxPose));
        }
        Vector3f localNormal = offset.div(distance);
        Vector3f normal = localNormal.rotate(boxPose.orientation());
        return Optional.of(
                new ContactResult(sphere.radius() - distance, normal, ShapeGeometry.toWorld(closest, boxPose)));
    }

    private static Optional<ContactResult> capsuleCapsule(
            CapsuleShape first, ShapePose firstPose, CapsuleShape second, ShapePose secondPose) {
        ShapeGeometry.SegmentPoints points = ShapeGeometry.closestPoints(
                ShapeGeometry.capsuleSegment(first, firstPose), ShapeGeometry.capsuleSegment(second, secondPose));
        Vector3f offset = new Vector3f(points.first()).sub(points.second());
        float distance = offset.length();
        float radii = first.radius() + second.radius();
        if (distance > radii + CONTACT_EPSILON) {
            return Optional.empty();
        }
        Vector3f fallback = new Vector3f(firstPose.position()).sub(secondPose.position());
        Vector3f normal = normalizedOrFallback(offset, normalizedOrFallback(fallback, new Vector3f(1.0F, 0.0F, 0.0F)));
        Vector3f point = new Vector3f(points.second()).fma(second.radius(), normal);
        return Optional.of(new ContactResult(radii - distance, normal, point));
    }

    private static Optional<ContactResult> capsuleBox(
            CapsuleShape capsule, ShapePose capsulePose, BoxShape box, ShapePose boxPose) {
        ShapeGeometry.SegmentBoxPoints points =
                ShapeGeometry.closestSegmentBox(ShapeGeometry.capsuleSegment(capsule, capsulePose), box, boxPose);
        float distance = (float) Math.sqrt(points.distanceSquared());
        if (distance > capsule.radius() + CONTACT_EPSILON) {
            return Optional.empty();
        }
        if (distance <= CONTACT_EPSILON) {
            Vector3f localPoint = ShapeGeometry.toBoxLocal(points.segmentPoint(), boxPose);
            Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
            return Optional.of(insideBoxContact(capsule.radius(), localPoint, half, boxPose));
        }
        Vector3f normal =
                new Vector3f(points.segmentPoint()).sub(points.boxPoint()).div(distance);
        return Optional.of(new ContactResult(capsule.radius() - distance, normal, points.boxPoint()));
    }

    private static Optional<ContactResult> boxBox(
            BoxShape first, ShapePose firstPose, BoxShape second, ShapePose secondPose) {
        OrientedBox firstBox = new OrientedBox(first, firstPose);
        OrientedBox secondBox = new OrientedBox(second, secondPose);
        AxisOverlap minimum = new AxisOverlap();
        if (!testPrincipalAxes(firstBox, secondBox, minimum) || !testCrossAxes(firstBox, secondBox, minimum)) {
            return Optional.empty();
        }
        Vector3f normal = minimum.normal();
        Vector3f firstPoint = ShapeGeometry.support(firstPose, new Vector3f(normal).negate());
        Vector3f secondPoint = ShapeGeometry.support(secondPose, normal);
        Vector3f point = firstPoint.add(secondPoint).mul(0.5F);
        return Optional.of(new ContactResult(minimum.depth(), normal, point));
    }

    private static boolean testPrincipalAxes(OrientedBox first, OrientedBox second, AxisOverlap minimum) {
        for (int axis = 0; axis < 3; axis++) {
            if (!minimum.test(first.axis(axis), first, second) || !minimum.test(second.axis(axis), first, second)) {
                return false;
            }
        }
        return true;
    }

    private static boolean testCrossAxes(OrientedBox first, OrientedBox second, AxisOverlap minimum) {
        for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
            for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                Vector3f axis = first.axis(firstAxis).cross(second.axis(secondAxis), new Vector3f());
                if (axis.lengthSquared() > CONTACT_EPSILON && !minimum.test(axis.normalize(), first, second)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static ContactResult insideBoxContact(
            float radius, Vector3fc localPoint, Vector3fc half, ShapePose boxPose) {
        int nearestAxis = 0;
        float faceDistance = half.x() - Math.abs(localPoint.x());
        for (int axis = 1; axis < 3; axis++) {
            float candidate = half.get(axis) - Math.abs(localPoint.get(axis));
            if (candidate < faceDistance) {
                nearestAxis = axis;
                faceDistance = candidate;
            }
        }
        Vector3f localNormal = new Vector3f();
        localNormal.setComponent(nearestAxis, localPoint.get(nearestAxis) < 0.0F ? -1.0F : 1.0F);
        Vector3f facePoint = new Vector3f(localPoint);
        facePoint.setComponent(nearestAxis, localNormal.get(nearestAxis) * half.get(nearestAxis));
        Vector3f normal = localNormal.rotate(boxPose.orientation());
        return new ContactResult(radius + faceDistance, normal, ShapeGeometry.toWorld(facePoint, boxPose));
    }

    private static Optional<ContactResult> reverse(Optional<ContactResult> contact) {
        return contact.map(value -> new ContactResult(
                value.penetrationDepth(), value.normal(new Vector3f()).negate(), value.point(new Vector3f())));
    }

    private static Vector3f normalizedOrFallback(Vector3f value, Vector3f fallback) {
        return value.lengthSquared() > CONTACT_EPSILON ? value.normalize() : fallback.normalize();
    }

    private static final class OrientedBox {
        private final Vector3f center;
        private final Vector3f half;
        private final Vector3f[] axes;

        OrientedBox(BoxShape shape, ShapePose pose) {
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

        float projectedRadius(Vector3fc axis) {
            return Math.abs(axis.dot(axes[0])) * half.x
                    + Math.abs(axis.dot(axes[1])) * half.y
                    + Math.abs(axis.dot(axes[2])) * half.z;
        }
    }

    private static final class AxisOverlap {
        private float depth = Float.POSITIVE_INFINITY;
        private final Vector3f normal = new Vector3f(1.0F, 0.0F, 0.0F);

        boolean test(Vector3fc axis, OrientedBox first, OrientedBox second) {
            Vector3f centerOffset = new Vector3f(first.center).sub(second.center);
            float signedDistance = centerOffset.dot(axis);
            float overlap = first.projectedRadius(axis) + second.projectedRadius(axis) - Math.abs(signedDistance);
            if (overlap < -CONTACT_EPSILON) {
                return false;
            }
            if (overlap < depth) {
                depth = Math.max(0.0F, overlap);
                normal.set(axis);
                if (signedDistance < 0.0F) {
                    normal.negate();
                }
            }
            return true;
        }

        float depth() {
            return depth;
        }

        Vector3f normal() {
            return new Vector3f(normal);
        }
    }
}
