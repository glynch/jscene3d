/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.physics;

import io.github.glynch.jscene3d.game.input.InputAction;
import java.util.Objects;

/**
 * Built-in locomotion actions consumed by a {@link CharacterMovementController}.
 *
 * <p>This action set is not an exhaustive list of actions available to a character or game. Callers may define and
 * process any additional {@link InputAction} instances independently.
 *
 * @param forward action that moves along view forward
 * @param backward action that moves opposite view forward
 * @param left action that moves left relative to view forward
 * @param right action that moves right relative to view forward
 * @param jump action that requests a grounded jump
 */
public record CharacterMovementActions(
        InputAction forward, InputAction backward, InputAction left, InputAction right, InputAction jump) {
    /** Rejects incomplete action sets. */
    public CharacterMovementActions {
        Objects.requireNonNull(forward, "forward");
        Objects.requireNonNull(backward, "backward");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(jump, "jump");
    }
}
