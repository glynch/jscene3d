/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.physics.CollisionObject;

/** Internal common boundary for declarative bodies that accept collider child nodes. */
interface CollisionBodyRuntimeObject {
    /** Returns the physics object that owns direct collision-shape children. */
    CollisionObject collisionObject();
}
