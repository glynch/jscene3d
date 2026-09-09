/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import java.util.Objects;

/** Anchored rectangular screen layout relative to an owning entity's region. */
final class ScreenRegion {
    private final float anchorX;
    private final float anchorY;
    private final float pivotX;
    private final float pivotY;
    private final float offsetX;
    private final float offsetY;
    private final float width;
    private final float height;

    ScreenRegion(Point anchor, Point pivot, Point offset, Size size) {
        Point validAnchor = requirePoint(anchor, "anchor");
        Point validPivot = requirePoint(pivot, "pivot");
        Point validOffset = requirePoint(offset, "offset");
        Size validSize = Objects.requireNonNull(size, "size");
        anchorX = requireUnit(validAnchor.x(), "anchor.x");
        anchorY = requireUnit(validAnchor.y(), "anchor.y");
        pivotX = requireUnit(validPivot.x(), "pivot.x");
        pivotY = requireUnit(validPivot.y(), "pivot.y");
        offsetX = requireFinite(validOffset.x(), "offset.x");
        offsetY = requireFinite(validOffset.y(), "offset.y");
        width = requirePositive(validSize.width(), "size.width");
        height = requirePositive(validSize.height(), "size.height");
    }

    /** Resolves this design-space region into logical viewport coordinates. */
    Bounds resolve(Bounds parent, float scale) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        return new Bounds(
                parent.x() + parent.width() * anchorX + offsetX * scale - scaledWidth * pivotX,
                parent.y() + parent.height() * anchorY + offsetY * scale - scaledHeight * pivotY,
                scaledWidth,
                scaledHeight);
    }

    private static float requireUnit(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]: " + value);
        }
        return value;
    }

    private static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    private static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
        return value;
    }

    private static Point requirePoint(Point point, String name) {
        return Objects.requireNonNull(point, name);
    }

    /** One authored two-dimensional location or vector. */
    record Point(float x, float y) {}

    /** One authored positive two-dimensional extent. */
    record Size(float width, float height) {}

    /** One logical-coordinate rectangle resolved by the screen hierarchy. */
    record Bounds(float x, float y, float width, float height) {}
}
