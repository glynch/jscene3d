/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.runtime.EntityInstantiationKind;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Internally preserves the authored and instantiation identities exposed by one live entity. */
record EntityProvenance(
        AssetId authoredAsset,
        EntityId authoredId,
        EntityInstantiationKind instantiationKind,
        @Nullable AssetId instantiatedDefinition) {

    /** Validates that only definition-instance roots name an instantiated definition. */
    EntityProvenance {
        Objects.requireNonNull(authoredAsset, "authoredAsset");
        Objects.requireNonNull(authoredId, "authoredId");
        Objects.requireNonNull(instantiationKind, "instantiationKind");
        boolean definitionRoot = instantiationKind != EntityInstantiationKind.LOCAL_ENTITY;
        if (definitionRoot != (instantiatedDefinition != null)) {
            throw new IllegalArgumentException("only placement and spawn roots name an instantiated definition");
        }
    }

    /** Creates provenance for one authored local-entity declaration. */
    static EntityProvenance local(AssetId authoredAsset, EntityId authoredId) {
        return new EntityProvenance(authoredAsset, authoredId, EntityInstantiationKind.LOCAL_ENTITY, null);
    }

    /** Creates provenance for one authored definition placement. */
    static EntityProvenance placement(AssetId containingAsset, EntityId placementId, AssetId definition) {
        return new EntityProvenance(containingAsset, placementId, EntityInstantiationKind.PLACEMENT, definition);
    }

    /** Creates provenance for one runtime-spawned definition root. */
    static EntityProvenance spawn(AssetId definition, EntityId rootId) {
        return new EntityProvenance(definition, rootId, EntityInstantiationKind.SPAWN, definition);
    }
}
