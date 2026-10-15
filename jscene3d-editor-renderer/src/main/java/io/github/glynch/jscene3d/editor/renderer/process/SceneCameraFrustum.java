/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraFrustumGeometry.Shape;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Camera3d;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Objects;

/** Owns one camera-local authored frustum line and its renderer resources. */
final class SceneCameraFrustum implements AutoCloseable {
    private static final Color[] COLORS = {
        Color.srgb(0x9f8cff), Color.srgb(0x79b8d1), Color.srgb(0xd5a36e), Color.srgb(0x7fbd9b)
    };
    private static final Color TRUNCATED_COLOR = Color.srgb(0xc9a45d);
    private static final float OPACITY = 0.62f;

    private final Camera3d description;
    private final float aspectRatio;
    private final Shape shape;
    private final BufferGeometry geometry;
    private final LineBasicMaterial material;
    private final LineSegments line;
    private boolean closed;

    SceneCameraFrustum(Camera3d description, float aspectRatio, Shape shape, int styleIndex) {
        this.description = Objects.requireNonNull(description, "description");
        this.aspectRatio = aspectRatio;
        this.shape = Objects.requireNonNull(shape, "shape");
        geometry = SceneCameraFrustumGeometry.geometry(shape);
        material = new LineBasicMaterial(
                shape.truncated() ? TRUNCATED_COLOR : COLORS[Math.floorMod(styleIndex, COLORS.length)]);
        material.setTransparent(true);
        material.setOpacity(OPACITY);
        line = new LineSegments(geometry, material);
        line.setVisible(false);
        line.setRenderOrder(1);
    }

    Camera3d description() {
        return description;
    }

    float aspectRatio() {
        return aspectRatio;
    }

    Shape shape() {
        return shape;
    }

    LineSegments line() {
        return line;
    }

    boolean isClosed() {
        return closed;
    }

    void attach(Object3D occurrence) {
        requireOpen();
        Objects.requireNonNull(occurrence, "occurrence").add(line);
    }

    void setVisible(boolean visible) {
        requireOpen();
        line.setVisible(visible);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        line.detach();
        geometry.close();
        material.close();
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene camera frustum is closed");
        }
    }
}
