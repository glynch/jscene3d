/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

/** Stable property identity used to combine animation tracks targeting the same scene object. */
public sealed interface AnimatedProperty permits MorphProperty, TransformProperty {}
