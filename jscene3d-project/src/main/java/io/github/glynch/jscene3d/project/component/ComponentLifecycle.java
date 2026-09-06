/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

/** Semantic lifecycle callbacks in which a runtime component may participate. */
public enum ComponentLifecycle {
    /** Invoked once after the complete instance graph has been constructed and bound. */
    CREATED,
    /** Invoked whenever the owning entity becomes effectively active. */
    ACTIVATED,
    /** Invoked whenever the owning entity ceases to be effectively active. */
    DEACTIVATED,
    /** Invoked once when the owning entity is permanently destroyed. */
    DESTROYED
}
