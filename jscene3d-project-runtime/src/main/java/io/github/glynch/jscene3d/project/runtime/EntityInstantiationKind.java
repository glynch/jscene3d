/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Identifies which declaration or runtime operation created one live {@link Entity}. */
public enum EntityInstantiationKind {
    /** The entity was produced directly from an authored local-entity declaration. */
    LOCAL_ENTITY,

    /** The entity is the root produced by an authored entity-definition placement. */
    PLACEMENT,

    /** The entity is the root produced by a runtime spawn of a prepared entity definition. */
    SPAWN
}
