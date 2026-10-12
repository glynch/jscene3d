/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import java.util.List;
import java.util.Optional;
import org.joml.Vector3f;

/** Immutable screen-space geometry and hit-test state for one transform-gizmo frame. */
record SceneTransformGizmoState(Point center, List<AxisVisual> axes, SceneTransformTool tool) {
    static final float AXIS_LENGTH = 132.0f;
    private static final float DRAG_DISTANCE = 82.0f;
    private static final float HIT_DISTANCE = 18.0f;
    private static final float PLANE_START = 36.0f;
    private static final float AUXILIARY_HIT_RADIUS = 18.0f;

    /** Copies the projected axes retained by this frame. */
    SceneTransformGizmoState {
        axes = List.copyOf(axes);
    }

    /** Returns one projected axis. */
    AxisVisual axis(Axis axis) {
        return axes.get(axis.ordinal());
    }

    /** Returns one axis endpoint in logical viewport coordinates. */
    Point endpoint(Axis axis) {
        AxisVisual visual = axis(axis);
        return new Point(center.x() + visual.screenX() * AXIS_LENGTH, center.y() + visual.screenY() * AXIS_LENGTH);
    }

    /** Returns one planar move-handle center in logical viewport coordinates. */
    Point planeCenter(Handle handle) {
        AxisVisual first = axis(handle.axes().get(0));
        AxisVisual second = axis(handle.axes().get(1));
        return new Point(
                center.x() + (first.screenX() + second.screenX()) * PLANE_START,
                center.y() + (first.screenY() + second.screenY()) * PLANE_START);
    }

    /** Returns one point on an axis ring projected from its actual rotation plane. */
    Point ringPoint(Axis axis, float angle) {
        AxisVisual visual = axis(axis);
        float cosine = (float) Math.cos(angle);
        float sine = (float) Math.sin(angle);
        return new Point(
                center.x() + (visual.ringFirstX() * cosine + visual.ringSecondX() * sine) * AXIS_LENGTH,
                center.y() + (visual.ringFirstY() * cosine + visual.ringSecondY() * sine) * AXIS_LENGTH);
    }

    /** Resolves one pointer location to the nearest handle for the current tool. */
    Optional<Handle> hit(float x, float y) {
        Optional<Handle> special =
                switch (tool) {
                    case MOVE -> hitMovePlane(x, y);
                    case SCALE -> hitScaleCenter(x, y);
                    case SELECT, ROTATE -> Optional.empty();
                };
        return special.isPresent() ? special : hitAxis(x, y);
    }

    /** Resolves one pointer location against the planar move handles. */
    private Optional<Handle> hitMovePlane(float x, float y) {
        for (Handle plane : List.of(Handle.XY, Handle.XZ, Handle.YZ)) {
            Point point = planeCenter(plane);
            if (Math.abs(x - point.x()) <= AUXILIARY_HIT_RADIUS && Math.abs(y - point.y()) <= AUXILIARY_HIT_RADIUS) {
                return Optional.of(plane);
            }
        }
        return Optional.empty();
    }

    /** Resolves one pointer location against the uniform-scale handle. */
    private Optional<Handle> hitScaleCenter(float x, float y) {
        return Math.hypot(x - center.x(), y - center.y()) <= AUXILIARY_HIT_RADIUS
                ? Optional.of(Handle.UNIFORM)
                : Optional.empty();
    }

    /** Resolves one pointer location against the projected axis handles. */
    private Optional<Handle> hitAxis(float x, float y) {
        Handle nearest = null;
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (AxisVisual visual : axes) {
            float distance = tool == SceneTransformTool.ROTATE
                    ? rotationRingDistance(visual.axis(), x, y)
                    : axisSegmentDistance(visual, x, y);
            if (distance <= HIT_DISTANCE && distance < nearestDistance) {
                nearest = Handle.axis(visual.axis());
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** Measures pointer distance to the same projected rotation polyline used for painting. */
    private float rotationRingDistance(Axis axis, float x, float y) {
        float nearest = Float.POSITIVE_INFINITY;
        Point previous = ringPoint(axis, 0.0f);
        for (int segment = 1; segment <= 64; segment++) {
            Point current = ringPoint(axis, (float) (Math.PI * 2.0 * segment / 64.0));
            nearest = Math.min(nearest, distanceToSegment(x, y, previous.x(), previous.y(), current.x(), current.y()));
            previous = current;
        }
        return nearest;
    }

    /** Measures pointer distance to one projected axis segment. */
    private float axisSegmentDistance(AxisVisual visual, float x, float y) {
        Point endpoint = endpoint(visual.axis());
        return distanceToSegment(x, y, center.x(), center.y(), endpoint.x(), endpoint.y());
    }

    /** Converts one pointer displacement to a normalized handle displacement. */
    float scalar(Handle handle, float deltaX, float deltaY) {
        if (handle == Handle.UNIFORM) {
            return (deltaX - deltaY) / DRAG_DISTANCE;
        }
        float scalar = 0.0f;
        for (Axis axis : handle.axes()) {
            AxisVisual visual = axis(axis);
            scalar += (deltaX * visual.screenX() + deltaY * visual.screenY()) / DRAG_DISTANCE;
        }
        return scalar / handle.axes().size();
    }

    /** Converts one pointer displacement to a world-space single-axis displacement. */
    Vector3f worldDelta(Handle handle, float deltaX, float deltaY) {
        if (handle.axes().size() != 1) {
            throw new IllegalArgumentException("plane displacement requires a retained world-space drag plane");
        }
        Axis axis = handle.axes().getFirst();
        AxisVisual visual = axis(axis);
        float amount = (deltaX * visual.screenX() + deltaY * visual.screenY()) / DRAG_DISTANCE;
        return worldAxis(axis).mul(amount);
    }

    /** Returns a defensive copy of one projected world axis. */
    Vector3f worldAxis(Axis axis) {
        return new Vector3f(this.axis(axis).world());
    }

    /** Computes point-to-segment distance for axis hit testing. */
    private static float distanceToSegment(float x, float y, float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float lengthSquared = deltaX * deltaX + deltaY * deltaY;
        float amount = lengthSquared == 0.0f
                ? 0.0f
                : Math.clamp(((x - startX) * deltaX + (y - startY) * deltaY) / lengthSquared, 0.0f, 1.0f);
        return (float) Math.hypot(x - (startX + amount * deltaX), y - (startY + amount * deltaY));
    }

    /** Conventional transform axis. */
    enum Axis {
        X,
        Y,
        Z;

        /** Returns the unit vector for this axis. */
        Vector3f vector() {
            return switch (this) {
                case X -> new Vector3f(1.0f, 0.0f, 0.0f);
                case Y -> new Vector3f(0.0f, 1.0f, 0.0f);
                case Z -> new Vector3f(0.0f, 0.0f, 1.0f);
            };
        }
    }

    /** Interactive axis, plane, or uniform-scale handle. */
    enum Handle {
        X(List.of(Axis.X)),
        Y(List.of(Axis.Y)),
        Z(List.of(Axis.Z)),
        XY(List.of(Axis.X, Axis.Y)),
        XZ(List.of(Axis.X, Axis.Z)),
        YZ(List.of(Axis.Y, Axis.Z)),
        UNIFORM(List.of(Axis.X, Axis.Y, Axis.Z));

        private final List<Axis> axes;

        /** Retains the axes controlled by this handle. */
        Handle(List<Axis> axes) {
            this.axes = axes;
        }

        /** Returns the controlled axes. */
        List<Axis> axes() {
            return axes;
        }

        /** Returns the single-axis handle for one axis. */
        static Handle axis(Axis axis) {
            return values()[axis.ordinal()];
        }
    }

    /** Immutable logical viewport point. */
    record Point(float x, float y) {}

    /** One world axis projected into the current viewport. */
    record AxisVisual(
            Axis axis,
            Vector3f world,
            float screenX,
            float screenY,
            float ringFirstX,
            float ringFirstY,
            float ringSecondX,
            float ringSecondY) {}
}
