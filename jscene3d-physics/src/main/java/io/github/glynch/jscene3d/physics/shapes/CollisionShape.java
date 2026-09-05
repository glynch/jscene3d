/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.shapes;

/** A finite, immutable three-dimensional collision shape. */
public sealed interface CollisionShape permits BoxShape, CapsuleShape, SphereShape, TriangleMeshShape {}
