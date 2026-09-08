/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Optional;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

/** World-scoped seam that realizes live three-dimensional spatial and presentation components. */
public interface Spatial3dWorldModule extends WorldModule {
    /**
     * Creates and registers the unique primary three-dimensional transform for one entity.
     *
     * <p>The composed-world runtime invokes this operation owner before child. When the direct ownership parent has a
     * transform registered in this module, the new transform inherits it; otherwise the transform begins a spatial
     * root. Inputs are copied before this method returns. The returned component owns removal of its registration when
     * the world closes it.
     *
     * @param owner live component owner
     * @param position finite local position
     * @param orientation finite non-zero local orientation
     * @param scale finite local scale
     * @return registered world-owned transform component
     * @throws IllegalArgumentException if an input is invalid, the owner belongs to another world, or the owner already
     *     has a registered transform
     * @throws IllegalStateException if this module is closed
     */
    Transform3d createTransform(Entity owner, Vector3fc position, Quaternionfc orientation, Vector3fc scale);

    /**
     * Finds the unique primary three-dimensional transform registered for one entity.
     *
     * @param owner live entity owned by the same world
     * @return registered transform, when present
     * @throws IllegalArgumentException if the entity belongs to another world
     * @throws IllegalStateException if this module is closed
     */
    Optional<Transform3d> findTransform(Entity owner);

    /**
     * Returns the unique primary three-dimensional transform registered for one entity.
     *
     * @param owner live entity owned by the same world
     * @return registered transform
     * @throws IllegalArgumentException if the entity belongs to another world or has no registered transform
     * @throws IllegalStateException if this module is closed
     */
    default Transform3d requireTransform(Entity owner) {
        return findTransform(owner)
                .orElseThrow(() -> new IllegalArgumentException("entity has no registered Transform3d: " + owner.id()));
    }

    /**
     * Creates a perspective camera attached to an entity's primary transform.
     *
     * @param owner live component owner
     * @param fieldOfViewDegrees vertical field of view in degrees
     * @param near positive near clipping distance
     * @param far clipping distance greater than {@code near}
     * @param primary whether this is the world's explicitly selected authored camera
     * @return registered world-owned camera component
     * @throws IllegalArgumentException if a value is invalid, the owner is incompatible, or another primary camera is
     *     already registered
     * @throws IllegalStateException if this module is closed
     */
    PerspectiveCamera3d createPerspectiveCamera(
            Entity owner, float fieldOfViewDegrees, float near, float far, boolean primary);

    /**
     * Creates a directional light attached to an entity's primary transform.
     *
     * @param owner live component owner
     * @param color immutable linear-sRGB color
     * @param intensity finite non-negative intensity
     * @param target finite world-space target
     * @return registered world-owned light component
     * @throws IllegalArgumentException if a value or owner is invalid
     * @throws IllegalStateException if this module is closed
     */
    DirectionalLight3d createDirectionalLight(Entity owner, Color color, float intensity, Vector3fc target);

    /**
     * Creates a mesh renderer attached to an entity's primary transform.
     *
     * @param owner live component owner
     * @param mesh shared immutable mesh resource
     * @param material shared immutable material resource
     * @param visible initial local visibility
     * @return registered world-owned mesh-renderer component
     * @throws IllegalArgumentException if a resource or owner is invalid
     * @throws IllegalStateException if this module is closed
     */
    MeshRenderer3d createMeshRenderer(Entity owner, Mesh3dResource mesh, Material3dResource material, boolean visible);

    /**
     * Creates a camera-facing billboard attached to an entity's primary transform.
     *
     * @param owner live component owner
     * @param material shared immutable unlit material resource
     * @param size positive finite world-space width and height
     * @param anchor finite normalized anchor coordinates
     * @param alignment camera-facing alignment mode
     * @param visible initial local visibility
     * @return registered world-owned billboard-renderer component
     * @throws IllegalArgumentException if a resource, value, or owner is invalid
     * @throws IllegalStateException if this module is closed
     */
    BillboardRenderer3d createBillboardRenderer(
            Entity owner,
            Material3dResource material,
            Vector2fc size,
            Vector2fc anchor,
            BillboardAlignment alignment,
            boolean visible);

    /**
     * Renders the current presentation through the effectively active primary camera.
     *
     * <p>The caller retains ownership of the renderer. This method updates the camera projection from the supplied
     * viewport aspect ratio before submitting the hidden scene graph.
     *
     * @param renderer open host renderer
     * @param viewportAspectRatio positive width-to-height ratio
     * @throws IllegalArgumentException if the aspect ratio is invalid
     * @throws IllegalStateException if this module is closed or has no effectively active primary camera
     */
    void render(Renderer renderer, float viewportAspectRatio);

    /**
     * Returns whether an effectively active primary camera is available.
     *
     * @return {@code true} when {@link #render(Renderer, float)} can select a camera
     * @throws IllegalStateException if this module is closed
     */
    boolean isReadyToRender();

    /**
     * Returns whether this adapter has released all registered spatial and presentation components.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();
}
