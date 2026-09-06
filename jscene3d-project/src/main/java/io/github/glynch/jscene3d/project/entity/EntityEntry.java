/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import java.util.Optional;

/** Authored hierarchy entry represented by a local entity or reusable-definition placement. */
public sealed interface EntityEntry permits EntityPlacement, LocalEntity {
    /**
     * Returns the stable identity of the entity produced by this entry.
     *
     * @return entity identity within the containing asset
     */
    EntityId id();

    /**
     * Returns the optional local display name.
     *
     * @return display name
     */
    Optional<String> name();

    /**
     * Returns whether the produced entity starts locally enabled.
     *
     * @return initial local enabled state
     */
    boolean isEnabled();
}
