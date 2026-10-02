/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable semantic description of one expanded entity occurrence.
 *
 * @param occurrence stable occurrence identity
 * @param parent parent occurrence, absent for a root
 * @param bindings authored scope identities that address this occurrence
 * @param authoredAsset asset containing the local entity or placement declaration
 * @param authoredSource source containing the local entity or placement declaration
 * @param authoredId local entity or placement identity in the containing asset
 * @param instantiatedDefinition referenced definition for placement roots
 * @param name effective display name
 * @param locallyEnabled locally authored enablement
 * @param effectivelyEnabled enablement after ownership propagation
 * @param components ordered effective components
 * @param location diagnostic location within the root composition
 */
public record CompositionEntity(
        CompositionOccurrenceId occurrence,
        Optional<CompositionOccurrenceId> parent,
        List<CompositionEntityBinding> bindings,
        AssetId authoredAsset,
        URI authoredSource,
        EntityId authoredId,
        Optional<AssetId> instantiatedDefinition,
        Optional<String> name,
        boolean locallyEnabled,
        boolean effectivelyEnabled,
        List<CompositionComponent> components,
        String location) {
    /** Copies one immutable entity plan. */
    public CompositionEntity {
        Objects.requireNonNull(occurrence, "occurrence");
        Objects.requireNonNull(parent, "parent");
        bindings = List.copyOf(bindings);
        if (bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings must not be empty");
        }
        Objects.requireNonNull(authoredAsset, "authoredAsset");
        Objects.requireNonNull(authoredSource, "authoredSource");
        Objects.requireNonNull(authoredId, "authoredId");
        Objects.requireNonNull(instantiatedDefinition, "instantiatedDefinition");
        Objects.requireNonNull(name, "name");
        components = List.copyOf(components);
        Objects.requireNonNull(location, "location");
    }
}
