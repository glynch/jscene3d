/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Objects;

/** Immutable, renderer-independent description of one selected object draw. */
public final class RenderContext {
    private final Scene scene;
    private final Camera camera;
    private final RenderableObject object;
    private final BufferGeometry geometry;
    private final Material material;
    private final RenderPass pass;

    private RenderContext(
            Scene scene,
            Camera camera,
            RenderableObject object,
            BufferGeometry geometry,
            Material material,
            RenderPass pass) {
        this.scene = scene;
        this.camera = camera;
        this.object = object;
        this.geometry = geometry;
        this.material = material;
        this.pass = pass;
    }

    /**
     * Describes a renderer-selected object draw without exposing backend state.
     *
     * <p>For a shadow pass, {@code camera} is the application camera that initiated the scene
     * render. The renderer-owned shadow projection is intentionally not exposed.
     *
     * @param scene scene containing the selected object
     * @param camera application camera that initiated rendering
     * @param object selected renderable object
     * @param geometry geometry captured while building the draw submission
     * @param material material captured while building the draw submission
     * @param pass active render pass
     * @return immutable render context
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RenderContext of(
            Scene scene,
            Camera camera,
            RenderableObject object,
            BufferGeometry geometry,
            Material material,
            RenderPass pass) {
        return new RenderContext(
                Objects.requireNonNull(scene, "scene"),
                Objects.requireNonNull(camera, "camera"),
                Objects.requireNonNull(object, "object"),
                Objects.requireNonNull(geometry, "geometry"),
                Objects.requireNonNull(material, "material"),
                Objects.requireNonNull(pass, "pass"));
    }

    /**
     * Returns the scene containing the selected object.
     *
     * @return active scene
     */
    public Scene scene() {
        return scene;
    }

    /**
     * Returns the application camera that initiated the scene render.
     *
     * @return active application camera
     */
    public Camera camera() {
        return camera;
    }

    /**
     * Returns the selected renderable object.
     *
     * @return object being drawn
     */
    public RenderableObject object() {
        return object;
    }

    /**
     * Returns the geometry captured for this draw.
     *
     * @return submitted geometry
     */
    public BufferGeometry geometry() {
        return geometry;
    }

    /**
     * Returns the material captured for this draw.
     *
     * @return submitted material
     */
    public Material material() {
        return material;
    }

    /**
     * Returns the active rendering pass.
     *
     * @return main or shadow pass
     */
    public RenderPass pass() {
        return pass;
    }
}
