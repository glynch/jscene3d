/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Backdrop;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Bounds;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.CubeFaceVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.CubeVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Direction;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Point;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.TipStyle;
import io.github.glynch.jscene3d.math.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.joml.Vector3f;

/** Camera-relative projection, geometry, and hit-test layout for the Scene orientation gizmo. */
final class SceneOrientationGizmoLayout {
    static final float WIDTH = 238.0f;
    static final float HEIGHT = 202.0f;
    static final float MARGIN = 26.0f;
    static final float LABEL_FONT_SIZE = 20.0f;

    private static final float CENTER_X = WIDTH * 0.5f;
    private static final float CENTER_Y = HEIGHT * 0.5f;
    private static final float AXIS_LENGTH = 68.0f;
    private static final float CUBE_HALF_EXTENT = 15.5f;
    private static final float POSITIVE_STEM_THICKNESS = 5.5f;
    private static final float NEGATIVE_STEM_THICKNESS = 3.0f;
    private static final float POSITIVE_HEAD_LENGTH = 20.0f;
    private static final float NEGATIVE_HEAD_LENGTH = 12.0f;
    private static final float POSITIVE_HEAD_HALF_WIDTH = 11.0f;
    private static final float NEGATIVE_HEAD_HALF_WIDTH = 6.0f;
    private static final float END_ON_THRESHOLD = 0.18f;
    private static final float POSITIVE_END_ON_SIZE = 28.0f;
    private static final float NEGATIVE_END_ON_SIZE = 16.0f;
    private static final float LABEL_OFFSET = 13.0f;
    private static final float POSITIVE_ALPHA = 1.0f;
    private static final float NEGATIVE_ALPHA = 0.42f;
    private static final Color CUBE_LIGHT = Color.srgb(0xf4f6fa);
    private static final Color CUBE_MID = Color.srgb(0xcbd2de);
    private static final Color CUBE_DARK = Color.srgb(0x9da8b9);

    private static final int[][] CUBE_FACES = {
        {0, 2, 6, 4},
        {1, 5, 7, 3},
        {0, 4, 5, 1},
        {2, 3, 7, 6},
        {0, 1, 3, 2},
        {4, 6, 7, 5}
    };

    private final Camera camera;
    private final Vector3f projected = new Vector3f();

    /** Retains the editor camera used to project world directions. */
    SceneOrientationGizmoLayout(Camera camera) {
        this.camera = camera;
    }

    /** Builds one immutable logical-coordinate render and interaction model. */
    SceneOrientationGizmoState renderState(int viewportWidth, int viewportHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        float x = Math.max(MARGIN, viewportWidth - WIDTH - MARGIN);
        Bounds bounds = new Bounds(x, MARGIN, WIDTH, HEIGHT);
        Point center = new Point(x + CENTER_X, MARGIN + CENTER_Y);
        List<AxisVisual> axes = createAxes(center);
        axes.sort(Comparator.comparingDouble(AxisVisual::depth));
        return new SceneOrientationGizmoState(bounds, center, Backdrop.NONE, createCube(center), axes);
    }

    /** Creates projected visuals for all six signed world-axis directions. */
    private List<AxisVisual> createAxes(Point center) {
        List<AxisVisual> axes = new ArrayList<>(Axis.values().length * Direction.values().length);
        for (Axis axis : Axis.values()) {
            for (Direction direction : Direction.values()) {
                axes.add(createAxis(center, axis, direction));
            }
        }
        return axes;
    }

    /** Creates one camera-relative signed-axis visual with future hit-test geometry. */
    private AxisVisual createAxis(Point center, Axis axis, Direction direction) {
        projected.set(axis.x() * direction.sign(), axis.y() * direction.sign(), axis.z() * direction.sign());
        camera.viewMatrix().transformDirection(projected);
        float screenLength = (float) Math.hypot(projected.x, projected.y);
        Optional<String> label = direction == Direction.POSITIVE ? Optional.of(axis.label()) : Optional.empty();
        float alpha = direction == Direction.POSITIVE ? POSITIVE_ALPHA : NEGATIVE_ALPHA;
        float stemThickness = direction == Direction.POSITIVE ? POSITIVE_STEM_THICKNESS : NEGATIVE_STEM_THICKNESS;
        float tipSize = direction == Direction.POSITIVE ? POSITIVE_END_ON_SIZE : NEGATIVE_END_ON_SIZE;

        if (screenLength < END_ON_THRESHOLD) {
            Bounds hitBounds = Bounds.centered(center, tipSize + 6.0f);
            return new AxisVisual(
                    axis,
                    direction,
                    axis.color(),
                    TipStyle.END_ON,
                    center,
                    center,
                    center,
                    List.of(),
                    projected.z,
                    alpha,
                    stemThickness,
                    tipSize,
                    label,
                    center,
                    hitBounds);
        }

        float directionX = projected.x / screenLength;
        float directionY = -projected.y / screenLength;
        float headLength = direction == Direction.POSITIVE ? POSITIVE_HEAD_LENGTH : NEGATIVE_HEAD_LENGTH;
        float headHalfWidth = direction == Direction.POSITIVE ? POSITIVE_HEAD_HALF_WIDTH : NEGATIVE_HEAD_HALF_WIDTH;
        Point endpoint = new Point(center.x() + directionX * AXIS_LENGTH, center.y() + directionY * AXIS_LENGTH);
        Point stemEnd = new Point(endpoint.x() - directionX * headLength, endpoint.y() - directionY * headLength);
        Point headLeft = new Point(stemEnd.x() - directionY * headHalfWidth, stemEnd.y() + directionX * headHalfWidth);
        Point headRight = new Point(stemEnd.x() + directionY * headHalfWidth, stemEnd.y() - directionX * headHalfWidth);
        Point labelAnchor = new Point(
                endpoint.x() + directionX * LABEL_OFFSET,
                endpoint.y() + directionY * LABEL_OFFSET - LABEL_FONT_SIZE * 0.5f);
        Bounds hitBounds = Bounds.enclosing(List.of(center, endpoint, headLeft, headRight), 4.0f);
        return new AxisVisual(
                axis,
                direction,
                axis.color(),
                TipStyle.ARROW,
                center,
                stemEnd,
                endpoint,
                List.of(endpoint, headLeft, headRight),
                projected.z,
                alpha,
                stemThickness,
                tipSize,
                label,
                labelAnchor,
                hitBounds);
    }

    /** Creates the camera-oriented neutral central cube and its interaction bounds. */
    private CubeVisual createCube(Point center) {
        List<ProjectedCorner> corners = projectCubeCorners(center);
        List<CubeFaceVisual> faces = new ArrayList<>(3);
        for (int faceIndex = 0; faceIndex < CUBE_FACES.length; faceIndex++) {
            Axis axis = Axis.values()[faceIndex / 2];
            Direction direction = faceIndex % 2 == 0 ? Direction.NEGATIVE : Direction.POSITIVE;
            projected.set(axis.x() * direction.sign(), axis.y() * direction.sign(), axis.z() * direction.sign());
            camera.viewMatrix().transformDirection(projected);
            if (projected.z > 0.001f) {
                faces.add(createFace(corners, faceIndex, axis, direction, projected.z));
            }
        }
        faces.sort(Comparator.comparingDouble(CubeFaceVisual::depth));
        List<Point> visiblePoints =
                faces.stream().flatMap(face -> face.points().stream()).toList();
        Bounds hitBounds = visiblePoints.isEmpty()
                ? Bounds.centered(center, CUBE_HALF_EXTENT * 2.0f)
                : Bounds.enclosing(visiblePoints, 2.0f);
        return new CubeVisual(faces, hitBounds);
    }

    /** Creates one visible cube face from projected corners. */
    private static CubeFaceVisual createFace(
            List<ProjectedCorner> corners, int faceIndex, Axis axis, Direction direction, float facing) {
        List<Point> points = new ArrayList<>(4);
        float depth = 0.0f;
        for (int cornerIndex : CUBE_FACES[faceIndex]) {
            ProjectedCorner corner = corners.get(cornerIndex);
            points.add(corner.point());
            depth += corner.depth();
        }
        return new CubeFaceVisual(axis, direction, cubeFaceColor(facing), points, depth / 4.0f);
    }

    /** Projects the eight cube corners through only the editor camera's orientation. */
    private List<ProjectedCorner> projectCubeCorners(Point center) {
        List<ProjectedCorner> corners = new ArrayList<>(8);
        for (int zSign : new int[] {-1, 1}) {
            for (int ySign : new int[] {-1, 1}) {
                for (int xSign : new int[] {-1, 1}) {
                    projected.set(xSign, ySign, zSign);
                    camera.viewMatrix().transformDirection(projected);
                    corners.add(new ProjectedCorner(
                            new Point(
                                    center.x() + projected.x * CUBE_HALF_EXTENT,
                                    center.y() - projected.y * CUBE_HALF_EXTENT),
                            projected.z));
                }
            }
        }
        return corners;
    }

    /** Selects a neutral cube-face shade from its camera-facing strength. */
    private static Color cubeFaceColor(float facing) {
        if (facing >= 0.75f) {
            return CUBE_LIGHT;
        }
        return facing >= 0.35f ? CUBE_MID : CUBE_DARK;
    }

    /** Projected cube corner with retained camera-space depth. */
    private record ProjectedCorner(Point point, float depth) {}
}
