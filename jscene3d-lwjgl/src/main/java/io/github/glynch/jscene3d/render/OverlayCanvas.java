/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.lwjgl.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Renderer-owned logical-coordinate canvas for safe two-dimensional overlays.
 *
 * <p>Overlay implementations receive this canvas only during {@link Overlay#paint}. The canvas
 * records CPU-side drawing commands; it does not expose OpenGL handles or state.
 */
public final class OverlayCanvas {
    private static final int COMPONENTS_PER_VERTEX = 8;
    private static final int RECTANGLE_VERTEX_COUNT = 6;
    private static final int CORNER_SEGMENTS = 6;

    private float[] vertices = new float[COMPONENTS_PER_VERTEX * 1024];
    private int[] commandStarts = new int[32];
    private int[] commandCounts = new int[32];
    private @Nullable Object[] commandImages = new Object[32];

    private int size;
    private int commandCount;
    private boolean commandOpen;
    private int activeCommandStart;
    private @Nullable OverlayImage activeImage;

    /** Restricts construction to the renderer while allowing overlays to use the supplied canvas. */
    OverlayCanvas() {}

    /**
     * Appends a solid rectangle.
     *
     * @param x finite horizontal origin
     * @param y finite vertical origin
     * @param width non-negative width
     * @param height non-negative height
     * @param color linear-sRGB color
     * @param alpha opacity in the inclusive range {@code [0, 1]}
     */
    public void rectangle(float x, float y, float width, float height, Color color, float alpha) {
        validateRectangle(x, y, width, height, color, alpha);
        if (width == 0.0f || height == 0.0f) {
            return;
        }
        beginCommand(null);
        rectangleVertices(x, y, width, height, color, alpha, null);
    }

    /**
     * Appends a solid rectangle with rounded corners.
     *
     * @param x finite horizontal origin
     * @param y finite vertical origin
     * @param width non-negative width
     * @param height non-negative height
     * @param radius non-negative corner radius, clamped to half the shortest dimension
     * @param color linear-sRGB color
     * @param alpha opacity in the inclusive range {@code [0, 1]}
     */
    public void roundedRectangle(float x, float y, float width, float height, float radius, Color color, float alpha) {
        validateRectangle(x, y, width, height, color, alpha);
        float validRadius = Preconditions.requireNonNegative(radius, "radius");
        if (width == 0.0f || height == 0.0f) {
            return;
        }
        float clampedRadius = Math.min(validRadius, Math.min(width, height) * 0.5f);
        if (clampedRadius == 0.0f) {
            rectangle(x, y, width, height, color, alpha);
            return;
        }

        beginCommand(null);
        int perimeterPoints = CORNER_SEGMENTS * 4;
        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;
        for (int point = 0; point < perimeterPoints; point++) {
            int nextPoint = (point + 1) % perimeterPoints;
            solidVertex(centerX, centerY, color, alpha);
            solidVertex(
                    roundedPointX(point, x, width, clampedRadius),
                    roundedPointY(point, y, height, clampedRadius),
                    color,
                    alpha);
            solidVertex(
                    roundedPointX(nextPoint, x, width, clampedRadius),
                    roundedPointY(nextPoint, y, height, clampedRadius),
                    color,
                    alpha);
        }
    }

    /**
     * Appends a solid line segment with square ends.
     *
     * @param startX finite starting x-coordinate
     * @param startY finite starting y-coordinate
     * @param endX finite ending x-coordinate
     * @param endY finite ending y-coordinate
     * @param thickness non-negative thickness
     * @param color linear-sRGB color
     * @param alpha opacity in the inclusive range {@code [0, 1]}
     */
    public void line(float startX, float startY, float endX, float endY, float thickness, Color color, float alpha) {
        float validStartX = Preconditions.requireFinite(startX, "startX");
        float validStartY = Preconditions.requireFinite(startY, "startY");
        float validEndX = Preconditions.requireFinite(endX, "endX");
        float validEndY = Preconditions.requireFinite(endY, "endY");
        float validThickness = Preconditions.requireNonNegative(thickness, "thickness");
        Color validColor = Objects.requireNonNull(color, "color");
        float validAlpha = Preconditions.requireUnitInterval(alpha, "alpha");
        float deltaX = validEndX - validStartX;
        float deltaY = validEndY - validStartY;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length == 0.0f || validThickness == 0.0f) {
            return;
        }
        float normalX = -deltaY / length * validThickness * 0.5f;
        float normalY = deltaX / length * validThickness * 0.5f;
        beginCommand(null);
        solidVertex(validStartX + normalX, validStartY + normalY, validColor, validAlpha);
        solidVertex(validEndX + normalX, validEndY + normalY, validColor, validAlpha);
        solidVertex(validEndX - normalX, validEndY - normalY, validColor, validAlpha);
        solidVertex(validStartX + normalX, validStartY + normalY, validColor, validAlpha);
        solidVertex(validEndX - normalX, validEndY - normalY, validColor, validAlpha);
        solidVertex(validStartX - normalX, validStartY - normalY, validColor, validAlpha);
    }

    /**
     * Appends a tinted rectangular region of an alpha-mask image.
     *
     * @param region immutable normalized source-image region
     * @param x finite horizontal destination origin
     * @param y finite vertical destination origin
     * @param width non-negative destination width
     * @param height non-negative destination height
     * @param color linear-sRGB tint
     * @param alpha opacity in the inclusive range {@code [0, 1]}
     */
    public void alphaMask(
            OverlayImage.Region region, float x, float y, float width, float height, Color color, float alpha) {
        OverlayImage.Region validRegion = Objects.requireNonNull(region, "region");
        validateRectangle(x, y, width, height, color, alpha);
        if (width == 0.0f || height == 0.0f) {
            return;
        }
        beginCommand(validRegion.image());
        rectangleVertices(x, y, width, height, color, alpha, validRegion);
    }

    /** Clears all accumulated vertices and commands while retaining storage. */
    void clear() {
        size = 0;
        commandCount = 0;
        commandOpen = false;
        activeCommandStart = 0;
        activeImage = null;
    }

    /** Returns packed position, texture-coordinate, and color data. */
    float[] vertices() {
        return vertices;
    }

    /** Returns the number of accumulated triangle vertices. */
    int vertexCount() {
        return size / COMPONENTS_PER_VERTEX;
    }

    /** Returns the number of contiguous solid or image draw commands. */
    int commandCount() {
        finishCommand();
        return commandCount;
    }

    /** Returns one command's first vertex. */
    int commandStart(int index) {
        return commandStarts[index];
    }

    /** Returns one command's vertex count. */
    int commandVertexCount(int index) {
        return commandCounts[index];
    }

    /** Returns one command's image, or {@code null} for solid geometry. */
    @Nullable
    OverlayImage commandImage(int index) {
        Object image = commandImages[index];
        return image instanceof OverlayImage overlayImage ? overlayImage : null;
    }

    /** Validates shared rectangle arguments. */
    private static void validateRectangle(float x, float y, float width, float height, Color color, float alpha) {
        Preconditions.requireFinite(x, "x");
        Preconditions.requireFinite(y, "y");
        Preconditions.requireNonNegative(width, "width");
        Preconditions.requireNonNegative(height, "height");
        Objects.requireNonNull(color, "color");
        Preconditions.requireUnitInterval(alpha, "alpha");
    }

    /** Begins or reuses a contiguous command for one image identity. */
    private void beginCommand(@Nullable OverlayImage image) {
        if (commandOpen && activeImage == image) {
            return;
        }
        finishCommand();
        commandOpen = true;
        activeCommandStart = vertexCount();
        activeImage = image;
    }

    /** Finalizes the active command when it contains vertices. */
    private void finishCommand() {
        if (!commandOpen) {
            return;
        }
        int count = vertexCount() - activeCommandStart;
        if (count > 0) {
            ensureCommandCapacity();
            commandStarts[commandCount] = activeCommandStart;
            commandCounts[commandCount] = count;
            commandImages[commandCount] = activeImage;
            commandCount++;
        }
        commandOpen = false;
        activeImage = null;
    }

    /** Appends a solid or alpha-masked rectangle using an optional image region. */
    private void rectangleVertices(
            float x,
            float y,
            float width,
            float height,
            Color color,
            float alpha,
            OverlayImage.@Nullable Region region) {
        float minimumU = region == null ? -1.0f : region.minimumU();
        float minimumV = region == null ? -1.0f : region.minimumV();
        float maximumU = region == null ? -1.0f : region.maximumU();
        float maximumV = region == null ? -1.0f : region.maximumV();
        ensureVertexCapacity(RECTANGLE_VERTEX_COUNT * COMPONENTS_PER_VERTEX);
        vertex(x, y, minimumU, minimumV, color, alpha);
        vertex(x + width, y, maximumU, minimumV, color, alpha);
        vertex(x + width, y + height, maximumU, maximumV, color, alpha);
        vertex(x, y, minimumU, minimumV, color, alpha);
        vertex(x + width, y + height, maximumU, maximumV, color, alpha);
        vertex(x, y + height, minimumU, maximumV, color, alpha);
    }

    /** Appends one untextured packed vertex. */
    private void solidVertex(float x, float y, Color color, float alpha) {
        vertex(x, y, -1.0f, -1.0f, color, alpha);
    }

    /** Appends one packed vertex. */
    private void vertex(float x, float y, float u, float v, Color color, float alpha) {
        ensureVertexCapacity(COMPONENTS_PER_VERTEX);
        vertices[size++] = x;
        vertices[size++] = y;
        vertices[size++] = u;
        vertices[size++] = v;
        vertices[size++] = color.red();
        vertices[size++] = color.green();
        vertices[size++] = color.blue();
        vertices[size++] = alpha;
    }

    /** Returns one x-coordinate on a clockwise rounded-rectangle perimeter. */
    private static float roundedPointX(int point, float x, float width, float radius) {
        int corner = point / CORNER_SEGMENTS;
        float centerX =
                switch (corner) {
                    case 0, 3 -> x + radius;
                    default -> x + width - radius;
                };
        return centerX + (float) Math.cos(roundedPointAngle(point)) * radius;
    }

    /** Returns one y-coordinate on a clockwise rounded-rectangle perimeter. */
    private static float roundedPointY(int point, float y, float height, float radius) {
        int corner = point / CORNER_SEGMENTS;
        float centerY =
                switch (corner) {
                    case 0, 1 -> y + radius;
                    default -> y + height - radius;
                };
        return centerY + (float) Math.sin(roundedPointAngle(point)) * radius;
    }

    /** Returns one angle on a clockwise rounded-rectangle perimeter. */
    private static float roundedPointAngle(int point) {
        int corner = point / CORNER_SEGMENTS;
        int cornerPoint = point % CORNER_SEGMENTS;
        float startAngle =
                switch (corner) {
                    case 0 -> (float) Math.PI;
                    case 1 -> (float) (Math.PI * 1.5);
                    case 2 -> 0.0f;
                    default -> (float) (Math.PI * 0.5);
                };
        return startAngle + (float) (Math.PI * 0.5 * cornerPoint / (CORNER_SEGMENTS - 1));
    }

    /** Grows packed vertex storage geometrically. */
    private void ensureVertexCapacity(int additionalComponents) {
        int required = size + additionalComponents;
        if (required > vertices.length) {
            vertices = Arrays.copyOf(vertices, Math.max(required, vertices.length * 2));
        }
    }

    /** Grows parallel draw-command storage geometrically. */
    private void ensureCommandCapacity() {
        if (commandCount == commandStarts.length) {
            int newLength = commandStarts.length * 2;
            commandStarts = Arrays.copyOf(commandStarts, newLength);
            commandCounts = Arrays.copyOf(commandCounts, newLength);
            commandImages = Arrays.copyOf(commandImages, newLength);
        }
    }
}
