/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/**
 * Boolean state produced by one resolved character movement update.
 *
 * @param grounded whether the final pose has walkable ground
 * @param stepped whether this update traversed a bounded step
 * @param jumped whether this update consumed a successful jump request
 */
public record CharacterMove3dState(boolean grounded, boolean stepped, boolean jumped) {}
