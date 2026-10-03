/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Runtime-free wire projection consumed by the safe Scene View renderer.
 *
 * @param sceneAssetId authoritative Scene asset identity
 * @param revision authored working-copy revision
 * @param occurrences expanded visual occurrences in owner-first order
 */
public record SceneViewSnapshotDto(String sceneAssetId, long revision, List<VisualOccurrence> occurrences) {
    /** Copies and validates one wire snapshot. */
    public SceneViewSnapshotDto {
        requireText(sceneAssetId, "sceneAssetId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        occurrences = List.copyOf(occurrences);
    }

    /**
     * Stable expanded occurrence identity.
     *
     * @param rootDefinitionAssetId root definition asset identity
     * @param entityPath ordered authored entity path from the root definition
     */
    public record Occurrence(String rootDefinitionAssetId, List<String> entityPath) {
        /** Copies and validates one occurrence identity. */
        public Occurrence {
            requireText(rootDefinitionAssetId, "rootDefinitionAssetId");
            entityPath = List.copyOf(entityPath);
            entityPath.forEach(value -> requireText(value, "entityPath value"));
        }
    }

    /**
     * Stable authored scope for one expanded definition instance.
     *
     * @param definitionAssetId authored definition identity
     * @param anchor expanded occurrence anchoring the definition instance
     */
    public record Scope(String definitionAssetId, Occurrence anchor) {
        /** Validates one scope. */
        public Scope {
            requireText(definitionAssetId, "definitionAssetId");
            Objects.requireNonNull(anchor, "anchor");
        }
    }

    /**
     * Stable component identity retained for reverse renderer lookup.
     *
     * @param occurrence expanded occurrence owning the realized component
     * @param scope authored definition scope containing the component
     * @param authoredEntityId authored entity identity within the scope
     * @param componentId authored component identity
     */
    public record ComponentIdentity(Occurrence occurrence, Scope scope, String authoredEntityId, String componentId) {
        /** Validates one component identity. */
        public ComponentIdentity {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(scope, "scope");
            requireText(authoredEntityId, "authoredEntityId");
            requireText(componentId, "componentId");
        }
    }

    /**
     * Exact decimal vector represented without binary floating-point conversion.
     *
     * @param x exact x component
     * @param y exact y component
     * @param z exact z component
     */
    public record Vector3(String x, String y, String z) {
        /** Validates one exact vector. */
        public Vector3 {
            requireText(x, "x");
            requireText(y, "y");
            requireText(z, "z");
        }
    }

    /**
     * Built-in Transform 3D projection.
     *
     * @param identity stable authored component identity
     * @param position effective local position
     * @param orientationDegrees effective Euler orientation in degrees
     * @param scale effective local scale
     */
    public record Transform3d(ComponentIdentity identity, Vector3 position, Vector3 orientationDegrees, Vector3 scale) {
        /** Validates one Transform 3D projection. */
        public Transform3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(orientationDegrees, "orientationDegrees");
            Objects.requireNonNull(scale, "scale");
        }
    }

    /**
     * Portable renderer-resource reference.
     *
     * @param kind serialized resource namespace
     * @param locator portable namespace-relative locator
     * @param projectPath resolved project path only for project resources
     */
    public record ResourceReference(
            String kind, String locator, @Nullable String projectPath) {
        /** Validates one resource reference shape. */
        public ResourceReference {
            requireText(kind, "kind");
            requireText(locator, "locator");
            if (("project".equals(kind)) != (projectPath != null)) {
                throw new IllegalArgumentException("only project resources require projectPath");
            }
            if (!"project".equals(kind) && !"asset".equals(kind) && !"import".equals(kind)) {
                throw new IllegalArgumentException("unsupported resource kind");
            }
        }
    }

    /**
     * Built-in Mesh Renderer 3D projection.
     *
     * @param identity stable authored component identity
     * @param mesh unrealized mesh resource reference
     * @param material unrealized material resource reference
     * @param visible effective authored visibility
     */
    public record MeshRenderer3d(
            ComponentIdentity identity, ResourceReference mesh, ResourceReference material, boolean visible) {
        /** Validates one mesh projection. */
        public MeshRenderer3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    /**
     * Built-in Directional Light 3D projection.
     *
     * @param identity stable authored component identity
     * @param color effective linear RGB color
     * @param intensity exact effective intensity
     * @param target effective local target position
     */
    public record DirectionalLight3d(ComponentIdentity identity, Vector3 color, String intensity, Vector3 target) {
        /** Validates one directional-light projection. */
        public DirectionalLight3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(color, "color");
            requireText(intensity, "intensity");
            Objects.requireNonNull(target, "target");
        }
    }

    /**
     * One expanded visual occurrence.
     *
     * @param occurrence stable expanded occurrence identity
     * @param parent parent expanded occurrence, or {@code null} for a root
     * @param authoredAssetId authored definition containing the entity
     * @param authoredSource authoritative authored source URI
     * @param authoredEntityId authored entity identity
     * @param name optional author-facing occurrence name
     * @param enabled effective enabled state
     * @param transform effective Transform 3D projection, when present
     * @param meshes effective mesh projections
     * @param directionalLight effective directional-light projection, when present
     */
    public record VisualOccurrence(
            Occurrence occurrence,
            @Nullable Occurrence parent,
            String authoredAssetId,
            String authoredSource,
            String authoredEntityId,
            @Nullable String name,
            boolean enabled,
            @Nullable Transform3d transform,
            List<MeshRenderer3d> meshes,
            @Nullable DirectionalLight3d directionalLight) {
        /** Copies and validates one visual occurrence. */
        public VisualOccurrence {
            Objects.requireNonNull(occurrence, "occurrence");
            requireText(authoredAssetId, "authoredAssetId");
            requireText(authoredSource, "authoredSource");
            requireText(authoredEntityId, "authoredEntityId");
            meshes = List.copyOf(meshes);
        }
    }

    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
