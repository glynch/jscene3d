/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Shared closest-point and support operations for the narrow phase. */
final class ShapeGeometry {
    private static final float SEGMENT_EPSILON = 1.0E-8F;

    private ShapeGeometry() {}

    static Segment capsuleSegment(CapsuleShape capsule, ShapePose pose) {
        Vector3f offset = new Vector3f(0.0F, capsule.segmentLength() * 0.5F, 0.0F).rotate(pose.orientation());
        return new Segment(new Vector3f(pose.position()).sub(offset), new Vector3f(pose.position()).add(offset));
    }

    static Vector3f closestPoint(Vector3fc point, Segment segment) {
        Vector3f direction = new Vector3f(segment.end()).sub(segment.start());
        float lengthSquared = direction.lengthSquared();
        if (lengthSquared <= SEGMENT_EPSILON) {
            return new Vector3f(segment.start());
        }
        float fraction = new Vector3f(point).sub(segment.start()).dot(direction) / lengthSquared;
        return direction.mul(Math.clamp(fraction, 0.0F, 1.0F)).add(segment.start());
    }

    static SegmentPoints closestPoints(Segment first, Segment second) {
        Vector3f firstDirection = new Vector3f(first.end()).sub(first.start());
        Vector3f secondDirection = new Vector3f(second.end()).sub(second.start());
        Vector3f offset = new Vector3f(first.start()).sub(second.start());
        float firstLength = firstDirection.lengthSquared();
        float secondLength = secondDirection.lengthSquared();
        float firstSecond = firstDirection.dot(secondDirection);
        float firstOffset = firstDirection.dot(offset);
        float secondOffset = secondDirection.dot(offset);
        Fractions fractions = closestFractions(firstLength, secondLength, firstSecond, firstOffset, secondOffset);
        return new SegmentPoints(
                firstDirection.mul(fractions.first()).add(first.start()),
                secondDirection.mul(fractions.second()).add(second.start()));
    }

    static SegmentBoxPoints closestSegmentBox(Segment worldSegment, BoxShape box, ShapePose boxPose) {
        Vector3f start = toBoxLocal(worldSegment.start(), boxPose);
        Vector3f end = toBoxLocal(worldSegment.end(), boxPose);
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        Vector3f direction = new Vector3f(end).sub(start);
        List<Float> candidates = segmentBoxCandidates(start, direction, half);
        SegmentBoxPoints best = null;
        for (float fraction : candidates) {
            Vector3f segmentPoint = new Vector3f(direction).mul(fraction).add(start);
            Vector3f boxPoint =
                    new Vector3f(segmentPoint).max(new Vector3f(half).negate()).min(half);
            float distanceSquared = segmentPoint.distanceSquared(boxPoint);
            if (best == null || distanceSquared < best.distanceSquared()) {
                best = new SegmentBoxPoints(segmentPoint, boxPoint, distanceSquared);
            }
        }
        if (best == null) {
            throw new IllegalStateException("segment-box candidate set is empty");
        }
        return best.toWorld(boxPose);
    }

    static Vector3f support(ShapePose pose, Vector3fc direction) {
        return switch (pose.shape()) {
            case SphereShape sphere ->
                new Vector3f(direction).normalize().mul(sphere.radius()).add(pose.position());
            case BoxShape box -> boxSupport(box, pose, direction);
            case CapsuleShape capsule -> capsuleSupport(capsule, pose, direction);
            case TriangleMeshShape ignored ->
                throw new IllegalArgumentException("triangle meshes do not have a convex support point");
        };
    }

    static Vector3f toBoxLocal(Vector3fc point, ShapePose boxPose) {
        return new Vector3f(point)
                .sub(boxPose.position())
                .rotate(boxPose.orientation().invert(new Quaternionf()));
    }

    static Vector3f toWorld(Vector3fc localPoint, ShapePose pose) {
        return new Vector3f(localPoint).rotate(pose.orientation()).add(pose.position());
    }

    private static Fractions closestFractions(
            float firstLength, float secondLength, float firstSecond, float firstOffset, float secondOffset) {
        if (firstLength <= SEGMENT_EPSILON && secondLength <= SEGMENT_EPSILON) {
            return new Fractions(0.0F, 0.0F);
        }
        if (firstLength <= SEGMENT_EPSILON) {
            return new Fractions(0.0F, Math.clamp(secondOffset / secondLength, 0.0F, 1.0F));
        }
        if (secondLength <= SEGMENT_EPSILON) {
            return new Fractions(Math.clamp(-firstOffset / firstLength, 0.0F, 1.0F), 0.0F);
        }
        float denominator = firstLength * secondLength - firstSecond * firstSecond;
        float firstFraction = denominator <= SEGMENT_EPSILON
                ? 0.0F
                : Math.clamp((firstSecond * secondOffset - firstOffset * secondLength) / denominator, 0.0F, 1.0F);
        float secondFraction = (firstSecond * firstFraction + secondOffset) / secondLength;
        if (secondFraction < 0.0F) {
            return new Fractions(Math.clamp(-firstOffset / firstLength, 0.0F, 1.0F), 0.0F);
        }
        if (secondFraction > 1.0F) {
            return new Fractions(Math.clamp((firstSecond - firstOffset) / firstLength, 0.0F, 1.0F), 1.0F);
        }
        return new Fractions(firstFraction, secondFraction);
    }

    private static List<Float> segmentBoxCandidates(Vector3fc start, Vector3fc direction, Vector3fc half) {
        List<Float> boundaries = new ArrayList<>();
        boundaries.add(0.0F);
        boundaries.add(1.0F);
        for (int axis = 0; axis < 3; axis++) {
            addBoundary(start.get(axis), direction.get(axis), -half.get(axis), boundaries);
            addBoundary(start.get(axis), direction.get(axis), half.get(axis), boundaries);
        }
        boundaries.sort(Comparator.naturalOrder());
        List<Float> candidates = new ArrayList<>(boundaries);
        for (int index = 0; index + 1 < boundaries.size(); index++) {
            addIntervalMinimum(start, direction, half, boundaries.get(index), boundaries.get(index + 1), candidates);
        }
        return candidates;
    }

    private static void addBoundary(float start, float direction, float plane, List<Float> boundaries) {
        if (Math.abs(direction) <= SEGMENT_EPSILON) {
            return;
        }
        float fraction = (plane - start) / direction;
        if (fraction > 0.0F && fraction < 1.0F) {
            boundaries.add(fraction);
        }
    }

    private static void addIntervalMinimum(
            Vector3fc start, Vector3fc direction, Vector3fc half, float lower, float upper, List<Float> candidates) {
        float middle = (lower + upper) * 0.5F;
        float quadratic = 0.0F;
        float linear = 0.0F;
        for (int axis = 0; axis < 3; axis++) {
            float coordinate = start.get(axis) + direction.get(axis) * middle;
            float boundary = coordinate < -half.get(axis) ? -half.get(axis) : half.get(axis);
            if (Math.abs(coordinate) > half.get(axis)) {
                quadratic += direction.get(axis) * direction.get(axis);
                linear += direction.get(axis) * (start.get(axis) - boundary);
            }
        }
        if (quadratic > SEGMENT_EPSILON) {
            float minimum = -linear / quadratic;
            if (minimum > lower && minimum < upper) {
                candidates.add(minimum);
            }
        }
    }

    private static Vector3f boxSupport(BoxShape box, ShapePose pose, Vector3fc direction) {
        Vector3f localDirection =
                new Vector3f(direction).rotate(pose.orientation().invert(new Quaternionf()));
        Vector3f local = new Vector3f(
                Math.copySign(box.width() * 0.5F, localDirection.x),
                Math.copySign(box.height() * 0.5F, localDirection.y),
                Math.copySign(box.depth() * 0.5F, localDirection.z));
        return toWorld(local, pose);
    }

    private static Vector3f capsuleSupport(CapsuleShape capsule, ShapePose pose, Vector3fc direction) {
        Segment segment = capsuleSegment(capsule, pose);
        Vector3f endpoint = new Vector3f(
                direction.dot(new Vector3f(segment.end()).sub(segment.start())) >= 0.0F
                        ? segment.end()
                        : segment.start());
        return endpoint.add(new Vector3f(direction).normalize().mul(capsule.radius()));
    }

    record Segment(Vector3f start, Vector3f end) {}

    record SegmentPoints(Vector3f first, Vector3f second) {}

    record SegmentBoxPoints(Vector3f segmentPoint, Vector3f boxPoint, float distanceSquared) {
        SegmentBoxPoints toWorld(ShapePose pose) {
            return new SegmentBoxPoints(
                    ShapeGeometry.toWorld(segmentPoint, pose), ShapeGeometry.toWorld(boxPoint, pose), distanceSquared);
        }
    }

    private record Fractions(float first, float second) {}
}
