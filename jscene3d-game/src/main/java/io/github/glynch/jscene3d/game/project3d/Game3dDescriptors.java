/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import io.github.glynch.jscene3d.project.standard.game3d.StandardGame3dDescriptors;

/**
 * Compatibility access to the safe built-in 3D gameplay descriptors.
 *
 * <p>New safe tooling should use {@link StandardGame3dDescriptors}.
 */
public final class Game3dDescriptors extends StandardGame3dDescriptors {
    private Game3dDescriptors() {
        throw new AssertionError("Game3dDescriptors cannot be instantiated");
    }
}
