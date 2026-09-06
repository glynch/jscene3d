/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

/** Spatial domain in which a component participates. */
public enum ComponentSpatialDomain {
    /** The component has no spatial participation. */
    NONE,
    /** The component participates in a three-dimensional spatial hierarchy. */
    THREE_DIMENSIONAL,
    /** The component participates in a two-dimensional spatial hierarchy. */
    TWO_DIMENSIONAL,
    /** The component participates in a user-interface layout hierarchy. */
    USER_INTERFACE
}
