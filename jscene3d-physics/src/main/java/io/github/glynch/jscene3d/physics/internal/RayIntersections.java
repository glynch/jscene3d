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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Exact ray intersections for supported shapes. */
final class RayIntersections {
    private static final float EPSILON = 1.0E-7F;

    private RayIntersections() {}

    static Optional<RayHitResult> intersect(
            Vector3fc origin, Vector3fc direction, float maximumDistance, ShapePose pose) {
        return switch (pose.shape()) {
            case SphereShape sphere -> sphere(origin, direction, maximumDistance, sphere.radius(), pose.position());
            case BoxShape box -> box(origin, direction, maximumDistance, box, pose);
            case CapsuleShape capsule -> capsule(origin, direction, maximumDistance, capsule, pose);
            case TriangleMeshShape mesh -> TriangleMeshQueries.raycast(origin, direction, maximumDistance, mesh, pose);
        };
    }

    private static Optional<RayHitResult> sphere(
            Vector3fc origin, Vector3fc direction, float maximumDistance, float radius, Vector3fc center) {
        float distance = sphereDistance(origin, direction, radius, center);
        if (distance > maximumDistance) {
            return Optional.empty();
        }
        Vector3f point = new Vector3f(direction).mul(distance).add(origin);
        Vector3f normal = distance <= EPSILON
                ? new Vector3f(direction).negate()
                : new Vector3f(point).sub(center).normalize();
        return Optional.of(new RayHitResult(distance, point, normal));
    }

    private static Optional<RayHitResult> box(
            Vector3fc origin, Vector3fc direction, float maximumDistance, BoxShape box, ShapePose pose) {
        Quaternionf inverse = pose.orientation().invert(new Quaternionf());
        Vector3f localOrigin = new Vector3f(origin).sub(pose.position()).rotate(inverse);
        Vector3f localDirection = new Vector3f(direction).rotate(inverse);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        if (inside(localOrigin, half)) {
            return Optional.of(new RayHitResult(0.0F, origin, new Vector3f(direction).negate()));
        }
        SlabIntersection slabs = new SlabIntersection(maximumDistance);
        for (int axis = 0; axis < 3 && slabs.canContinue(); axis++) {
            slabs.include(axis, localOrigin.get(axis), localDirection.get(axis), half.get(axis));
        }
        if (!slabs.isValid()) {
            return Optional.empty();
        }
        Vector3f point = new Vector3f(direction).mul(slabs.near()).add(origin);
        Vector3f normal = slabs.localNormal().rotate(pose.orientation());
        return Optional.of(new RayHitResult(slabs.near(), point, normal));
    }

    private static Optional<RayHitResult> capsule(
            Vector3fc origin, Vector3fc direction, float maximumDistance, CapsuleShape capsule, ShapePose pose) {
        Quaternionf inverse = pose.orientation().invert(new Quaternionf());
        Vector3f localOrigin = new Vector3f(origin).sub(pose.position()).rotate(inverse);
        Vector3f localDirection = new Vector3f(direction).rotate(inverse);
        float halfSegment = capsule.segmentLength() * 0.5F;
        if (insideCapsule(localOrigin, capsule.radius(), halfSegment)) {
            return Optional.of(new RayHitResult(0.0F, origin, new Vector3f(direction).negate()));
        }
        float distance = capsuleDistance(localOrigin, localDirection, capsule.radius(), halfSegment);
        if (!Float.isFinite(distance) || distance > maximumDistance) {
            return Optional.empty();
        }
        Vector3f localPoint = new Vector3f(localDirection).mul(distance).add(localOrigin);
        Vector3f axisPoint = new Vector3f(0.0F, Math.clamp(localPoint.y, -halfSegment, halfSegment), 0.0F);
        Vector3f normal = localPoint.sub(axisPoint).normalize().rotate(pose.orientation());
        Vector3f point = new Vector3f(direction).mul(distance).add(origin);
        return Optional.of(new RayHitResult(distance, point, normal));
    }

    private static float sphereDistance(Vector3fc origin, Vector3fc direction, float radius, Vector3fc center) {
        Vector3f offset = new Vector3f(origin).sub(center);
        float constant = offset.lengthSquared() - radius * radius;
        if (constant <= 0.0F) {
            return 0.0F;
        }
        float linear = offset.dot(direction);
        float discriminant = linear * linear - constant;
        if (discriminant < 0.0F) {
            return Float.POSITIVE_INFINITY;
        }
        float distance = -linear - (float) Math.sqrt(discriminant);
        return distance >= 0.0F ? distance : Float.POSITIVE_INFINITY;
    }

    private static float capsuleDistance(Vector3fc origin, Vector3fc direction, float radius, float halfSegment) {
        float cylinderDistance = cylinderDistance(origin, direction, radius, halfSegment);
        float lowerCap = sphereDistance(origin, direction, radius, new Vector3f(0.0F, -halfSegment, 0.0F));
        float upperCap = sphereDistance(origin, direction, radius, new Vector3f(0.0F, halfSegment, 0.0F));
        return Math.min(cylinderDistance, Math.min(lowerCap, upperCap));
    }

    private static float cylinderDistance(Vector3fc origin, Vector3fc direction, float radius, float halfSegment) {
        float quadratic = direction.x() * direction.x() + direction.z() * direction.z();
        if (quadratic <= EPSILON) {
            return Float.POSITIVE_INFINITY;
        }
        float linear = origin.x() * direction.x() + origin.z() * direction.z();
        float constant = origin.x() * origin.x() + origin.z() * origin.z() - radius * radius;
        float discriminant = linear * linear - quadratic * constant;
        if (discriminant < 0.0F) {
            return Float.POSITIVE_INFINITY;
        }
        float distance = (-linear - (float) Math.sqrt(discriminant)) / quadratic;
        float height = origin.y() + distance * direction.y();
        return distance >= 0.0F && Math.abs(height) <= halfSegment ? distance : Float.POSITIVE_INFINITY;
    }

    private static boolean inside(Vector3fc point, Vector3fc half) {
        return Math.abs(point.x()) <= half.x() && Math.abs(point.y()) <= half.y() && Math.abs(point.z()) <= half.z();
    }

    private static boolean insideCapsule(Vector3fc point, float radius, float halfSegment) {
        float closestY = Math.clamp(point.y(), -halfSegment, halfSegment);
        return new Vector3f(point).sub(0.0F, closestY, 0.0F).lengthSquared() <= radius * radius;
    }

    private static final class SlabIntersection {
        private float near;
        private float far;
        private int nearAxis = -1;
        private float nearSign;
        private boolean valid = true;

        SlabIntersection(float maximumDistance) {
            far = maximumDistance;
        }

        void include(int axis, float origin, float direction, float halfExtent) {
            if (Math.abs(direction) <= EPSILON) {
                valid = origin >= -halfExtent && origin <= halfExtent;
                return;
            }
            float inverse = 1.0F / direction;
            float first = (-halfExtent - origin) * inverse;
            float second = (halfExtent - origin) * inverse;
            float axisNear = Math.min(first, second);
            float axisFar = Math.max(first, second);
            if (axisNear > near) {
                near = axisNear;
                nearAxis = axis;
                nearSign = first < second ? -1.0F : 1.0F;
            }
            far = Math.min(far, axisFar);
            valid = near <= far;
        }

        boolean isValid() {
            return valid && nearAxis >= 0;
        }

        boolean canContinue() {
            return valid;
        }

        float near() {
            return near;
        }

        Vector3f localNormal() {
            Vector3f normal = new Vector3f();
            normal.setComponent(nearAxis, nearSign);
            return normal;
        }
    }
}
