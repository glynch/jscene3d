/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import io.github.glynch.jscene3d.project.standard.application.StandardGameApplicationDescriptors;

/**
 * Compatibility access to the safe built-in application descriptors.
 *
 * <p>New safe tooling should use {@link StandardGameApplicationDescriptors}.
 */
public final class GameApplicationDescriptors extends StandardGameApplicationDescriptors {
    private GameApplicationDescriptors() {
        throw new AssertionError("GameApplicationDescriptors cannot be instantiated");
    }
}
