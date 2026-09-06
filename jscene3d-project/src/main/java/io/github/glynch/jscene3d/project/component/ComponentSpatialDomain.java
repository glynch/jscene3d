/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

/** Primary spatial authority supplied by a component. */
public enum ComponentSpatialDomain {
    /** The component does not supply primary spatial state. */
    NONE,
    /** The component supplies primary three-dimensional spatial state. */
    THREE_DIMENSIONAL,
    /** The component supplies primary two-dimensional spatial state. */
    TWO_DIMENSIONAL,
    /** The component supplies primary user-interface layout state. */
    USER_INTERFACE
}
