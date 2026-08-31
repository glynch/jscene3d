/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

/** Renderer-internal interpretation of successive geometry elements. */
public enum PrimitiveTopology {
    /** Independent groups of three triangle vertices. */
    TRIANGLES(GL_TRIANGLES),

    /** One connected strip with a segment between each successive pair. */
    LINE_STRIP(GL_LINE_STRIP),

    /** Independent segments formed from successive pairs. */
    LINE_SEGMENTS(GL_LINES);

    private final int openGlMode;

    /** Associates one renderer topology with its OpenGL draw mode. */
    PrimitiveTopology(int openGlMode) {
        this.openGlMode = openGlMode;
    }

    /**
     * Returns the OpenGL draw mode.
     *
     * @return OpenGL primitive-mode constant
     */
    public int openGlMode() {
        return openGlMode;
    }

    /**
     * Returns whether this topology draws lines instead of surfaces.
     *
     * @return whether this is a line topology
     */
    public boolean isLine() {
        return this != TRIANGLES;
    }

    /**
     * Returns whether the selected element count contains any complete primitive.
     *
     * @param elementCount selected geometry-element count
     * @return whether at least one primitive can be drawn
     */
    public boolean hasPrimitives(int elementCount) {
        return switch (this) {
            case TRIANGLES -> elementCount > 0;
            case LINE_STRIP, LINE_SEGMENTS -> elementCount >= 2;
        };
    }

    /**
     * Rejects an element count that cannot satisfy this topology's grouping contract.
     *
     * @param elementCount selected geometry-element count
     */
    public void validateElementCount(int elementCount) {
        if (this == LINE_SEGMENTS && elementCount % 2 != 0) {
            throw new IllegalStateException(
                    "LineSegments draw range must contain an even number of elements: " + elementCount);
        }
    }

    /**
     * Returns the number of primitives submitted by the selected element count.
     *
     * @param elementCount selected geometry-element count
     * @return submitted primitive count
     */
    public long primitiveCount(int elementCount) {
        return switch (this) {
            case TRIANGLES -> elementCount / 3L;
            case LINE_STRIP -> elementCount - 1L;
            case LINE_SEGMENTS -> elementCount / 2L;
        };
    }
}
