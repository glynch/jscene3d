/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.util.Objects;

/** Immutable session snapshot of one retained structural definition.
 *
 * @param id authoritative asset identity
 * @param kind structural definition kind
 * @param origin authored or generated origin
 * @param editable whether authoring mutations are permitted
 * @param source logical source location
 * @param revision current session authoring revision
 * @param content current immutable domain definition
 * @param hierarchy complete hierarchy projection
 */
public record EditorRetainedDefinition(
        AssetId id,
        AssetKind kind,
        Origin origin,
        boolean editable,
        URI source,
        long revision,
        Content content,
        EditorHierarchyProjection hierarchy) {
    /** Validates one retained-definition snapshot. */
    public EditorRetainedDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(origin, "origin");
        source = Objects.requireNonNull(source, "source").normalize();
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(hierarchy, "hierarchy");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if (!source.isAbsolute()) {
            throw new IllegalArgumentException("source must be absolute");
        }
        if (editable != (origin == Origin.AUTHORED)) {
            throw new IllegalArgumentException("only authored definitions are editable");
        }
        if (!id.equals(content.id()) || kind != content.kind()) {
            throw new IllegalArgumentException("definition identity and content must agree");
        }
    }

    /** Definition-level source provenance, distinct from placed hierarchy descendants. */
    public enum Origin {
        /** Definition discovered in the authored project asset catalog. */
        AUTHORED,
        /** Definition produced by published import content. */
        GENERATED
    }

    /** Closed structural-definition content retained by the session. */
    public sealed interface Content permits Content.World, Content.Entity {
        /**
         * Returns the authoritative asset identity.
         *
         * @return definition asset identity
         */
        AssetId id();

        /**
         * Returns the structural definition kind.
         *
         * @return world or entity definition kind
         */
        AssetKind kind();

        /** Current immutable world content or the startup-world working-copy snapshot.
         *
         * @param definition current world definition
         */
        record World(WorldDefinition definition) implements Content {
            /** Validates world content. */
            public World {
                Objects.requireNonNull(definition, "definition");
            }

            @Override
            public AssetId id() {
                return definition.id();
            }

            @Override
            public AssetKind kind() {
                return AssetKind.WORLD_DEFINITION;
            }
        }

        /** Current immutable reusable entity-definition content.
         *
         * @param definition current entity definition
         */
        record Entity(EntityDefinition definition) implements Content {
            /** Validates entity-definition content. */
            public Entity {
                Objects.requireNonNull(definition, "definition");
            }

            @Override
            public AssetId id() {
                return definition.id();
            }

            @Override
            public AssetKind kind() {
                return AssetKind.ENTITY_DEFINITION;
            }
        }
    }
}
