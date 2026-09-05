/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.debug.PhysicsDebugLine;
import io.github.glynch.jscene3d.physics.debug.PhysicsDebugSnapshot;
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

/** Converts supported collision shapes into deterministic world-space line segments. */
public final class DebugGeometry {
    private static final int CURVE_SEGMENTS = 24;
    private static final Vector3fc X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3fc Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3fc Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final int[][] BOX_EDGES = {
        {0, 1}, {1, 3}, {3, 2}, {2, 0},
        {4, 5}, {5, 7}, {7, 6}, {6, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private DebugGeometry() {}

    /** Creates a complete snapshot from registered colliders.
     * @param colliders registered colliders to represent
     * @return immutable debug snapshot
     */
    public static PhysicsDebugSnapshot snapshot(Iterable<Collider> colliders) {
        List<PhysicsDebugLine> lines = new ArrayList<>();
        sorted(colliders).forEach(collider -> addCollider(lines, collider));
        return new PhysicsDebugSnapshot(lines);
    }

    private static void addCollider(List<PhysicsDebugLine> lines, Collider collider) {
        switch (collider.shape()) {
            case BoxShape box -> addBox(lines, collider, box);
            case SphereShape sphere -> addSphere(lines, collider, sphere);
            case CapsuleShape capsule -> addCapsule(lines, collider, capsule);
            case TriangleMeshShape mesh -> addTriangleMesh(lines, collider, mesh);
        }
    }

    /** Adds the three indexed edges of every mesh triangle. */
    private static void addTriangleMesh(List<PhysicsDebugLine> lines, Collider collider, TriangleMeshShape mesh) {
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
            int offset = triangle * 3;
            mesh.vertex(mesh.index(offset), first);
            mesh.vertex(mesh.index(offset + 1), second);
            mesh.vertex(mesh.index(offset + 2), third);
            addLocalLine(lines, collider, first, second);
            addLocalLine(lines, collider, second, third);
            addLocalLine(lines, collider, third, first);
        }
    }

    private static void addBox(List<PhysicsDebugLine> lines, Collider collider, BoxShape box) {
        float halfWidth = box.width() * 0.5F;
        float halfHeight = box.height() * 0.5F;
        float halfDepth = box.depth() * 0.5F;
        Vector3f[] vertices = {
            local(-halfWidth, -halfHeight, -halfDepth),
            local(halfWidth, -halfHeight, -halfDepth),
            local(-halfWidth, halfHeight, -halfDepth),
            local(halfWidth, halfHeight, -halfDepth),
            local(-halfWidth, -halfHeight, halfDepth),
            local(halfWidth, -halfHeight, halfDepth),
            local(-halfWidth, halfHeight, halfDepth),
            local(halfWidth, halfHeight, halfDepth)
        };
        for (int[] edge : BOX_EDGES) {
            addLocalLine(lines, collider, vertices[edge[0]], vertices[edge[1]]);
        }
    }

    private static void addSphere(List<PhysicsDebugLine> lines, Collider collider, SphereShape sphere) {
        addCircle(lines, collider, new Vector3f(), X_AXIS, Y_AXIS, sphere.radius());
        addCircle(lines, collider, new Vector3f(), X_AXIS, Z_AXIS, sphere.radius());
        addCircle(lines, collider, new Vector3f(), Y_AXIS, Z_AXIS, sphere.radius());
    }

    private static void addCapsule(List<PhysicsDebugLine> lines, Collider collider, CapsuleShape capsule) {
        float halfSegment = capsule.segmentLength() * 0.5F;
        Vector3f bottom = new Vector3f(0.0F, -halfSegment, 0.0F);
        Vector3f top = new Vector3f(0.0F, halfSegment, 0.0F);
        addCircle(lines, collider, bottom, X_AXIS, Z_AXIS, capsule.radius());
        addCircle(lines, collider, top, X_AXIS, Z_AXIS, capsule.radius());
        for (int index = 0; index < 4; index++) {
            double angle = Math.PI * 0.5 * index;
            Vector3f radial = new Vector3f(
                    (float) Math.cos(angle) * capsule.radius(), 0.0F, (float) Math.sin(angle) * capsule.radius());
            addLocalLine(lines, collider, new Vector3f(bottom).add(radial), new Vector3f(top).add(radial));
        }
        addCapsuleArc(lines, collider, capsule, X_AXIS);
        addCapsuleArc(lines, collider, capsule, Z_AXIS);
    }

    private static void addCapsuleArc(
            List<PhysicsDebugLine> lines, Collider collider, CapsuleShape capsule, Vector3fc radialAxis) {
        float halfSegment = capsule.segmentLength() * 0.5F;
        List<Vector3f> points = new ArrayList<>();
        for (int index = 0; index <= CURVE_SEGMENTS; index++) {
            double angle = Math.PI * 2.0 * index / CURVE_SEGMENTS;
            float radial = (float) Math.sin(angle) * capsule.radius();
            float y = Math.copySign(halfSegment, (float) Math.cos(angle)) + (float) Math.cos(angle) * capsule.radius();
            points.add(new Vector3f(radialAxis).mul(radial).add(0.0F, y, 0.0F));
        }
        addPolyline(lines, collider, points);
    }

    private static void addCircle(
            List<PhysicsDebugLine> lines,
            Collider collider,
            Vector3fc center,
            Vector3fc firstAxis,
            Vector3fc secondAxis,
            float radius) {
        List<Vector3f> points = new ArrayList<>();
        for (int index = 0; index <= CURVE_SEGMENTS; index++) {
            double angle = Math.PI * 2.0 * index / CURVE_SEGMENTS;
            points.add(new Vector3f(center)
                    .fma((float) Math.cos(angle) * radius, firstAxis)
                    .fma((float) Math.sin(angle) * radius, secondAxis));
        }
        addPolyline(lines, collider, points);
    }

    private static void addPolyline(List<PhysicsDebugLine> lines, Collider collider, List<Vector3f> points) {
        for (int index = 1; index < points.size(); index++) {
            addLocalLine(lines, collider, points.get(index - 1), points.get(index));
        }
    }

    private static void addLocalLine(
            List<PhysicsDebugLine> lines, Collider collider, Vector3fc localStart, Vector3fc localEnd) {
        Quaternionf orientation = collider.orientation(new Quaternionf());
        Vector3f position = collider.position(new Vector3f());
        Vector3f start = new Vector3f(localStart).rotate(orientation).add(position);
        Vector3f end = new Vector3f(localEnd).rotate(orientation).add(position);
        lines.add(new PhysicsDebugLine(collider, start, end));
    }

    private static List<Collider> sorted(Iterable<Collider> colliders) {
        List<Collider> sorted = new ArrayList<>();
        colliders.forEach(sorted::add);
        sorted.sort(Comparator.comparingLong(Collider::id));
        return sorted;
    }

    private static Vector3f local(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }
}
