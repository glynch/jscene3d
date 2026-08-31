/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import org.joml.Vector3f;

/** Mutable normalized ray state retained by one {@link Raycaster}. */
final class RayState {
    final Vector3f origin = new Vector3f();
    final Vector3f direction = new Vector3f(0.0f, 0.0f, -1.0f);
}
