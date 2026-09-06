/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireAbsoluteUri;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/** Builds immutable definition resolvers without exposing physical generated-content storage. */
public final class DefinitionResolvers {
    /** Prevents construction of this factory container. */
    private DefinitionResolvers() {
        throw new AssertionError("DefinitionResolvers cannot be instantiated");
    }

    /**
     * Starts a resolver containing every definition in an authored asset catalog.
     *
     * @param assets authored definition catalog
     * @return mutable builder for adding generated definitions
     */
    public static Builder builder(AssetCatalog assets) {
        return new Builder(Objects.requireNonNull(assets, "assets"));
    }

    /** Collects complete generated definition documents before publishing an immutable resolver. */
    public static final class Builder {
        private final DefinitionAssetIndex.Builder definitions;
        private boolean built;

        /** Starts with every authored definition source. */
        private Builder(AssetCatalog assets) {
            definitions = DefinitionAssetIndex.builder(assets);
        }

        /**
         * Adds one generated entity-definition document.
         *
         * <p>The complete content is consumed immediately. The caller retains ownership of {@code input}.
         *
         * @param id authoritative generated asset identity
         * @param source absolute logical source used for diagnostics
         * @param input canonical entity-definition JSON
         * @return this builder
         * @throws IOException when the content cannot be read
         * @throws IllegalArgumentException when the identity is already present
         * @throws IllegalStateException after this builder has built a resolver
         */
        public Builder addGeneratedEntity(AssetId id, URI source, InputStream input) throws IOException {
            requireOpen();
            definitions.addGeneratedEntity(
                    Objects.requireNonNull(id, "id"),
                    requireAbsoluteUri(source, "source").normalize(),
                    Objects.requireNonNull(input, "input").readAllBytes());
            return this;
        }

        /**
         * Publishes one immutable resolver and closes this builder to further mutation.
         *
         * @return resolver over authored and generated definitions
         */
        public DefinitionResolver build() {
            requireOpen();
            built = true;
            return new IndexedDefinitionResolver(definitions.build());
        }

        /** Rejects mutation or repeated publication after build. */
        private void requireOpen() {
            if (built) {
                throw new IllegalStateException("definition resolver builder has already built a resolver");
            }
        }
    }
}
