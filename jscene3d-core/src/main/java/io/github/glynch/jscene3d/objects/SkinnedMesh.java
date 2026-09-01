/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import java.util.Objects;

/** A triangular mesh deformed by four weighted skeleton joints per vertex. */
public final class SkinnedMesh extends Mesh {
    private final Skeleton skeleton;

    /**
     * Creates a skinned mesh retaining shared geometry, material, and skeleton references.
     *
     * <p>The geometry must contain four-component {@link BufferGeometry#JOINTS joints} and {@link
     * BufferGeometry#WEIGHTS weights} attributes. Animated bounds are not maintained in version
     * 0.1, so frustum culling is disabled initially and may be explicitly re-enabled only when the
     * caller supplies bounds covering every pose.
     *
     * @param geometry open geometry containing skinning attributes
     * @param material open surface material
     * @param skeleton skeleton controlling vertex deformation
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a resource is closed or skinning attributes are absent
     *     or malformed
     */
    public SkinnedMesh(BufferGeometry geometry, Material material, Skeleton skeleton) {
        super(geometry, material);
        this.skeleton = Objects.requireNonNull(skeleton, "skeleton");
        requireSkinAttribute(geometry, BufferGeometry.JOINTS);
        requireSkinAttribute(geometry, BufferGeometry.WEIGHTS);
        setFrustumCullingEnabled(false);
    }

    /**
     * Returns the retained skeleton.
     *
     * @return controlling skeleton
     */
    public Skeleton skeleton() {
        return skeleton;
    }

    /**
     * Replaces the shared geometry after preserving this mesh's skinning invariant.
     *
     * @param geometry open geometry containing four-component joint and weight attributes
     * @throws NullPointerException if {@code geometry} is {@code null}
     * @throws IllegalArgumentException if {@code geometry} is closed or lacks valid skinning
     *     attributes
     */
    @Override
    public void setGeometry(BufferGeometry geometry) {
        requireSkinAttribute(geometry, BufferGeometry.JOINTS);
        requireSkinAttribute(geometry, BufferGeometry.WEIGHTS);
        super.setGeometry(geometry);
    }

    /** Rejects an absent or non-four-component skinning attribute. */
    private static void requireSkinAttribute(BufferGeometry geometry, String name) {
        Objects.requireNonNull(geometry, "geometry");
        BufferAttribute attribute = geometry.attribute(name);
        if (attribute == null) {
            throw new IllegalArgumentException("SkinnedMesh geometry requires " + name + " attribute");
        }
        if (attribute.itemSize() != 4) {
            throw new IllegalArgumentException(name + " attribute itemSize must be 4: " + attribute.itemSize());
        }
    }
}
