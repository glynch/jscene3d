/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.ArrayList;
import java.util.List;

/** Records GUI drawing commands for headless layout assertions. */
final class RecordingGuiCanvas implements GuiCanvas {
    private final List<Color> rectangleColors = new ArrayList<>();
    private final List<Float> roundedRectangleAlphas = new ArrayList<>();

    private int rectangleCount;
    private int roundedRectangleCount;
    private int lineCount;
    private int alphaMaskCount;

    /** Records one rectangle command. */
    @Override
    public void rectangle(float x, float y, float width, float height, Color color, float alpha) {
        rectangleCount++;
        rectangleColors.add(color);
    }

    /** Records one rounded-rectangle command. */
    @Override
    public void roundedRectangle(float x, float y, float width, float height, float radius, Color color, float alpha) {
        roundedRectangleCount++;
        roundedRectangleAlphas.add(alpha);
    }

    /** Records one line command. */
    @Override
    public void line(float startX, float startY, float endX, float endY, float thickness, Color color, float alpha) {
        lineCount++;
    }

    /** Records one alpha-mask command. */
    @Override
    public void alphaMask(
            OverlayImage.Region region, float x, float y, float width, float height, Color color, float alpha) {
        alphaMaskCount++;
    }

    /** Returns the number of rectangle commands. */
    int rectangleCount() {
        return rectangleCount;
    }

    /** Returns the colors used by rectangle commands. */
    List<Color> rectangleColors() {
        return List.copyOf(rectangleColors);
    }

    /** Returns the number of rounded-rectangle commands. */
    int roundedRectangleCount() {
        return roundedRectangleCount;
    }

    /** Returns the opacities used by rounded-rectangle commands. */
    List<Float> roundedRectangleAlphas() {
        return List.copyOf(roundedRectangleAlphas);
    }

    /** Returns the number of line commands. */
    int lineCount() {
        return lineCount;
    }

    /** Returns the number of alpha-mask commands. */
    int alphaMaskCount() {
        return alphaMaskCount;
    }

    /** Returns the total number of recorded commands. */
    int commandCount() {
        return rectangleCount + roundedRectangleCount + lineCount + alphaMaskCount;
    }
}
