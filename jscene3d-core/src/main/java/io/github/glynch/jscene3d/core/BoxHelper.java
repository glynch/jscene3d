/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;

/** World-axis-aligned wireframe bounds for visible renderable geometry in an object subtree. */
public final class BoxHelper extends GeneratedLineSegmentsHelper {
    private static final int[] EDGE_INDICES = {
        0, 1, 1, 2, 2, 3, 3, 0,
        4, 5, 5, 6, 6, 7, 7, 4,
        0, 4, 1, 5, 2, 6, 3, 7
    };

    private Object3D target;

    /**
     * Creates yellow bounds around a target and its visible renderable descendants.
     *
     * @param target object whose subtree supplies the initial bounds
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if the target contains no visible renderable geometry
     */
    public BoxHelper(Object3D target) {
        this(target, Color.YELLOW);
    }

    /**
     * Creates colored bounds around a target and its visible renderable descendants.
     *
     * <p>The helper owns its generated geometry and material. Close the helper rather than those
     * resources individually. Their replacement is unsupported. Its vertices use world
     * coordinates, so add the helper without applying a transform. Call {@link #update()} after
     * changing target transforms, visibility, hierarchy, or geometry.
     *
     * @param target object whose subtree supplies the initial bounds
     * @param color immutable linear-sRGB line color
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the target contains no visible renderable geometry
     */
    public BoxHelper(Object3D target, Color color) {
        this(initialize(target, color));
    }

    /** Creates the helper from fully validated initialization state. */
    private BoxHelper(Initialization initialization) {
        super(createGeometry(initialization.bounds()), new LineBasicMaterial(initialization.color()));
        target = initialization.target();
        setRenderOrder(1);
    }

    /**
     * Returns the object whose visible subtree supplies these bounds.
     *
     * @return the retained target
     */
    public Object3D target() {
        return target;
    }

    /**
     * Changes the target and immediately refreshes the generated bounds.
     *
     * <p>If validation or bounds computation fails, the existing target and vertices remain
     * unchanged.
     *
     * @param target replacement target
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if the target contains no visible renderable geometry
     * @throws IllegalStateException if this helper is closed
     */
    public void setTarget(Object3D target) {
        Object3D validTarget = Objects.requireNonNull(target, "target");
        BoundingBox bounds = requireInitialBounds(validTarget);
        updatePositions(bounds);
        this.target = validTarget;
    }

    /**
     * Refreshes the generated vertices from the target's current visible world-space bounds.
     *
     * @throws IllegalStateException if this helper is closed or the target no longer contains
     *     visible renderable geometry
     */
    public void update() {
        BoundingBox bounds = VisibleObjectBounds.compute(target);
        if (bounds == null) {
            throw new IllegalStateException("BoxHelper target contains no visible renderable geometry");
        }
        updatePositions(bounds);
    }

    /** Validates constructor state before any owned resources are created. */
    private static Initialization initialize(Object3D target, Color color) {
        Object3D validTarget = Objects.requireNonNull(target, "target");
        Color validColor = Objects.requireNonNull(color, "color");
        return new Initialization(validTarget, validColor, requireInitialBounds(validTarget));
    }

    /** Requires a target with bounds suitable for construction or retargeting. */
    private static BoundingBox requireInitialBounds(Object3D target) {
        BoundingBox bounds = VisibleObjectBounds.compute(target);
        if (bounds == null) {
            throw new IllegalArgumentException("target must contain visible renderable geometry");
        }
        return bounds;
    }

    /** Creates dynamic corner positions and the static twelve-edge index. */
    private static BufferGeometry createGeometry(BoundingBox bounds) {
        BufferAttribute positions = BufferAttribute.of(new float[24], 3, BufferUsage.DYNAMIC);
        BufferGeometry geometry = BufferGeometry.builder()
                .attribute(BufferGeometry.POSITION, positions)
                .indices(EDGE_INDICES)
                .build();
        writePositions(positions, bounds);
        return geometry;
    }

    /** Rewrites all eight generated corners in one versioned edit. */
    private void updatePositions(BoundingBox bounds) {
        BufferAttribute positions = Objects.requireNonNull(geometry().attribute(BufferGeometry.POSITION));
        writePositions(positions, bounds);
    }

    /** Writes the eight corners in edge-index order. */
    private static void writePositions(BufferAttribute positions, BoundingBox bounds) {
        float minimumX = bounds.minimum().x();
        float minimumY = bounds.minimum().y();
        float minimumZ = bounds.minimum().z();
        float maximumX = bounds.maximum().x();
        float maximumY = bounds.maximum().y();
        float maximumZ = bounds.maximum().z();
        positions.edit(editor -> {
            editor.setXYZ(0, minimumX, minimumY, minimumZ);
            editor.setXYZ(1, maximumX, minimumY, minimumZ);
            editor.setXYZ(2, maximumX, maximumY, minimumZ);
            editor.setXYZ(3, minimumX, maximumY, minimumZ);
            editor.setXYZ(4, minimumX, minimumY, maximumZ);
            editor.setXYZ(5, maximumX, minimumY, maximumZ);
            editor.setXYZ(6, maximumX, maximumY, maximumZ);
            editor.setXYZ(7, minimumX, maximumY, maximumZ);
        });
    }

    /** Fully validated constructor input. */
    private record Initialization(Object3D target, Color color, BoundingBox bounds) {}
}
