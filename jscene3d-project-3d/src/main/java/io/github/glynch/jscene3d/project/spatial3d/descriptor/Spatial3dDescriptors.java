/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d.descriptor;

import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;

/**
 * Compatibility access to the safe built-in spatial descriptors.
 *
 * <p>New safe tooling should use {@link StandardSpatial3dDescriptors}.
 */
public final class Spatial3dDescriptors extends StandardSpatial3dDescriptors {
    private Spatial3dDescriptors() {
        throw new AssertionError("Spatial3dDescriptors cannot be instantiated");
    }
}
