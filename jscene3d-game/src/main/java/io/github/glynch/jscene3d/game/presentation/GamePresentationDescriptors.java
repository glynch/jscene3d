/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.project.standard.presentation.StandardGamePresentationDescriptors;

/**
 * Compatibility access to the safe built-in presentation descriptors.
 *
 * <p>New safe tooling should use {@link StandardGamePresentationDescriptors}.
 */
public final class GamePresentationDescriptors extends StandardGamePresentationDescriptors {
    private GamePresentationDescriptors() {
        throw new AssertionError("GamePresentationDescriptors cannot be instantiated");
    }
}
