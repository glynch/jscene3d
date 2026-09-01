/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

/**
 * A transform node used as one joint in a {@link Skeleton}.
 *
 * <p>Bones use the ordinary scene hierarchy and transform API. Their current world transforms are
 * sampled automatically when a skinned mesh is rendered.
 */
public final class Bone extends Object3D {
    /** Creates an unparented bone with the identity local transform. */
    public Bone() {
        super();
    }
}
