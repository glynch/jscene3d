/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.standard.physics3d.StandardPhysics3dDescriptors;

/**
 * Compatibility access to the safe built-in physics descriptors.
 *
 * <p>New safe tooling should use {@link StandardPhysics3dDescriptors}.
 */
public final class Physics3dDescriptors extends StandardPhysics3dDescriptors {
    private Physics3dDescriptors() {
        throw new AssertionError("Physics3dDescriptors cannot be instantiated");
    }
}
