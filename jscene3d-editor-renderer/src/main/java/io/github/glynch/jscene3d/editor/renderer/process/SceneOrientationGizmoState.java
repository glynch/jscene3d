/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.math.Color;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable render and future hit-test model for one Scene orientation gizmo frame. */
record SceneOrientationGizmoState(
        Bounds bounds, Point center, Backdrop backdrop, CubeVisual cube, List<AxisVisual> axes) {

    /** Copies collection state and validates required values. */
    SceneOrientationGizmoState {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(backdrop, "backdrop");
        Objects.requireNonNull(cube, "cube");
        axes = List.copyOf(Objects.requireNonNull(axes, "axes"));
    }

    /** World axis represented by one conventional editor color and positive label. */
    enum Axis {
        X("X", Color.RED, 1.0f, 0.0f, 0.0f),
        Y("Y", Color.GREEN, 0.0f, 1.0f, 0.0f),
        Z("Z", Color.BLUE, 0.0f, 0.0f, 1.0f);

        private final String label;
        private final Color color;
        private final float x;
        private final float y;
        private final float z;

        /** Retains one world axis's semantic and visual description. */
        Axis(String label, Color color, float x, float y, float z) {
            this.label = label;
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        String label() {
            return label;
        }

        Color color() {
            return color;
        }

        float x() {
            return x;
        }

        float y() {
            return y;
        }

        float z() {
            return z;
        }
    }

    /** Sign of a world-axis direction, retained separately for future directional hit testing. */
    enum Direction {
        NEGATIVE(-1.0f),
        POSITIVE(1.0f);

        private final float sign;

        /** Retains the direction multiplier. */
        Direction(float sign) {
            this.sign = sign;
        }

        float sign() {
            return sign;
        }
    }

    /** Tip presentation used when an axis is projected laterally or end-on. */
    enum TipStyle {
        ARROW,
        END_ON
    }

    /** Backdrop treatment for the overlay footprint. */
    enum Backdrop {
        NONE
    }

    /** Immutable widget bounds in logical Scene View coordinates. */
    record Bounds(float x, float y, float width, float height) {
        /** Creates square bounds centered around one screen point. */
        static Bounds centered(Point center, float size) {
            float halfSize = size * 0.5f;
            return new Bounds(center.x() - halfSize, center.y() - halfSize, size, size);
        }

        /** Creates padded bounds around a non-empty point collection. */
        static Bounds enclosing(List<Point> points, float padding) {
            if (points.isEmpty()) {
                throw new IllegalArgumentException("points must not be empty");
            }
            float minimumX = Float.POSITIVE_INFINITY;
            float minimumY = Float.POSITIVE_INFINITY;
            float maximumX = Float.NEGATIVE_INFINITY;
            float maximumY = Float.NEGATIVE_INFINITY;
            for (Point point : points) {
                minimumX = Math.min(minimumX, point.x());
                minimumY = Math.min(minimumY, point.y());
                maximumX = Math.max(maximumX, point.x());
                maximumY = Math.max(maximumY, point.y());
            }
            return new Bounds(
                    minimumX - padding,
                    minimumY - padding,
                    maximumX - minimumX + padding * 2.0f,
                    maximumY - minimumY + padding * 2.0f);
        }
    }

    /** Immutable point in logical Scene View coordinates. */
    record Point(float x, float y) {}

    /** One projected central-cube face. */
    record CubeFaceVisual(Axis axis, Direction direction, Color color, List<Point> points, float depth) {
        CubeFaceVisual {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(color, "color");
            points = List.copyOf(Objects.requireNonNull(points, "points"));
            if (points.size() != 4) {
                throw new IllegalArgumentException("cube faces require four points");
            }
        }
    }

    /** Camera-oriented central cube with future interaction bounds. */
    record CubeVisual(List<CubeFaceVisual> faces, Bounds hitBounds) {
        CubeVisual {
            faces = List.copyOf(Objects.requireNonNull(faces, "faces"));
            Objects.requireNonNull(hitBounds, "hitBounds");
        }
    }

    /** One projected signed axis, including geometry suitable for future interaction. */
    record AxisVisual(
            Axis axis,
            Direction direction,
            Color color,
            TipStyle tipStyle,
            Point stemStart,
            Point stemEnd,
            Point endpoint,
            List<Point> head,
            float depth,
            float alpha,
            float stemThickness,
            float tipSize,
            Optional<String> label,
            Point labelAnchor,
            Bounds hitBounds) {
        AxisVisual {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(tipStyle, "tipStyle");
            Objects.requireNonNull(stemStart, "stemStart");
            Objects.requireNonNull(stemEnd, "stemEnd");
            Objects.requireNonNull(endpoint, "endpoint");
            head = List.copyOf(Objects.requireNonNull(head, "head"));
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(labelAnchor, "labelAnchor");
            Objects.requireNonNull(hitBounds, "hitBounds");
        }
    }
}
