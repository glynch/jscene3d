/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import java.net.URI;
import java.util.Objects;

/** Immutable session snapshot of one retained structural definition.
 *
 * @param id authoritative asset identity
 * @param kind structural definition kind
 * @param origin authored or generated origin
 * @param editable whether authoring mutations are permitted
 * @param source logical source location
 * @param revision current per-definition authoring revision
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
        if (editable && origin != Origin.AUTHORED) {
            throw new IllegalArgumentException("generated definitions cannot be editable");
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
    public sealed interface Content permits Content.Scene, Content.Entity {
        /**
         * Returns the authoritative asset identity.
         *
         * @return definition asset identity
         */
        AssetId id();

        /**
         * Returns the structural definition kind.
         *
         * @return Scene or entity definition kind
         */
        AssetKind kind();

        /** Current immutable Scene content or its working-copy snapshot.
         *
         * @param definition current Scene definition
         */
        record Scene(SceneDefinition definition) implements Content {
            /** Validates Scene content. */
            public Scene {
                Objects.requireNonNull(definition, "definition");
            }

            @Override
            public AssetId id() {
                return definition.id();
            }

            @Override
            public AssetKind kind() {
                return AssetKind.SCENE_DEFINITION;
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
