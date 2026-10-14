/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.sceneview;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.composition.CompositionScope;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, runtime-free visual projection of one authored Scene revision.
 *
 * <p>The snapshot contains only the built-in visual semantics required by the first Scene View. Resource values remain
 * references; this model never realizes project code, runtime components, renderer objects, or GPU resources.
 *
 * @param scene authoritative Scene asset identity
 * @param revision authored working-copy revision
 * @param occurrences expanded entity occurrences in deterministic owner-first order
 */
public record SceneViewSnapshot(AssetId scene, long revision, List<VisualOccurrence> occurrences) {
    /** Copies and validates one Scene View snapshot. */
    public SceneViewSnapshot {
        Objects.requireNonNull(scene, "scene");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        occurrences = List.copyOf(occurrences);
        if (occurrences.stream()
                .anyMatch(occurrence -> !scene.equals(occurrence.occurrence().rootDefinition()))) {
            throw new IllegalArgumentException("all occurrences must belong to the snapshot Scene");
        }
    }

    /**
     * One expanded entity occurrence and its supported visual components.
     *
     * @param occurrence stable expanded occurrence identity
     * @param parent parent occurrence, absent for a Scene root
     * @param authoredAsset asset containing the local entity or placement declaration
     * @param authoredSource source containing the local entity or placement declaration
     * @param authoredEntity local entity or placement identity in the containing asset
     * @param name effective display name
     * @param enabled effective enabled state after ownership propagation
     * @param transform optional built-in Transform 3D projection
     * @param meshes built-in Mesh Renderer 3D projections in declaration order
     * @param directionalLight optional built-in Directional Light 3D projection
     * @param cameras built-in authored camera projections in declaration order
     */
    public record VisualOccurrence(
            CompositionOccurrenceId occurrence,
            Optional<CompositionOccurrenceId> parent,
            AssetId authoredAsset,
            URI authoredSource,
            EntityId authoredEntity,
            Optional<String> name,
            boolean enabled,
            Optional<Transform3d> transform,
            List<MeshRenderer3d> meshes,
            Optional<DirectionalLight3d> directionalLight,
            List<Camera3d> cameras) {
        /** Copies and validates one visual occurrence. */
        public VisualOccurrence {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(authoredAsset, "authoredAsset");
            authoredSource =
                    Objects.requireNonNull(authoredSource, "authoredSource").normalize();
            Objects.requireNonNull(authoredEntity, "authoredEntity");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(transform, "transform");
            meshes = List.copyOf(meshes);
            Objects.requireNonNull(directionalLight, "directionalLight");
            cameras = List.copyOf(cameras);
            if (!authoredSource.isAbsolute()) {
                throw new IllegalArgumentException("authoredSource must be absolute");
            }
            parent.ifPresent(parentOccurrence -> {
                if (!parentOccurrence.rootDefinition().equals(occurrence.rootDefinition())) {
                    throw new IllegalArgumentException("parent and child must belong to the same Scene");
                }
            });
            transform.ifPresent(component -> requireOwner(component.identity(), occurrence));
            meshes.forEach(component -> requireOwner(component.identity(), occurrence));
            directionalLight.ifPresent(component -> requireOwner(component.identity(), occurrence));
            cameras.forEach(component -> requireOwner(component.identity(), occurrence));
        }
    }

    /** Authored camera projection semantics retained for editor visualization. */
    public sealed interface Camera3d permits PerspectiveCamera3d, OrthographicCamera3d {
        /**
         * Returns the stable authored component identity.
         *
         * @return stable authored component identity
         */
        ComponentIdentity identity();

        /**
         * Returns the near clipping distance.
         *
         * @return near clipping distance
         */
        BigDecimal near();

        /**
         * Returns the far clipping distance.
         *
         * @return far clipping distance
         */
        BigDecimal far();

        /**
         * Reports whether this is an authored primary camera.
         *
         * @return whether this is an authored primary camera
         */
        boolean primary();
    }

    /**
     * Built-in Perspective Camera 3D projection state.
     *
     * @param identity stable authored component identity
     * @param fieldOfViewDegrees vertical field of view in degrees
     * @param near near clipping distance
     * @param far far clipping distance
     * @param primary whether this is an authored primary camera
     */
    public record PerspectiveCamera3d(
            ComponentIdentity identity, BigDecimal fieldOfViewDegrees, BigDecimal near, BigDecimal far, boolean primary)
            implements Camera3d {
        /** Validates one perspective camera projection. */
        public PerspectiveCamera3d {
            Objects.requireNonNull(identity, "identity");
            requireFinite(fieldOfViewDegrees, "fieldOfViewDegrees");
            requireFinite(near, "near");
            requireFinite(far, "far");
            if (fieldOfViewDegrees.signum() <= 0 || fieldOfViewDegrees.compareTo(BigDecimal.valueOf(180)) >= 0) {
                throw new IllegalArgumentException("fieldOfViewDegrees must be greater than 0 and less than 180");
            }
            if (near.signum() <= 0) {
                throw new IllegalArgumentException("near must be greater than 0");
            }
            if (far.compareTo(near) <= 0) {
                throw new IllegalArgumentException("far must be greater than near");
            }
        }
    }

    /**
     * Exact orthographic projection bounds and magnification.
     *
     * @param left left projection boundary
     * @param right right projection boundary
     * @param top top projection boundary
     * @param bottom bottom projection boundary
     * @param near near clipping distance
     * @param far far clipping distance
     * @param zoom projection magnification
     */
    public record OrthographicProjection(
            BigDecimal left,
            BigDecimal right,
            BigDecimal top,
            BigDecimal bottom,
            BigDecimal near,
            BigDecimal far,
            BigDecimal zoom) {
        /** Validates one orthographic projection. */
        public OrthographicProjection {
            requireFinite(left, "left");
            requireFinite(right, "right");
            requireFinite(top, "top");
            requireFinite(bottom, "bottom");
            requireFinite(near, "near");
            requireFinite(far, "far");
            requireFinite(zoom, "zoom");
            if (left.compareTo(right) >= 0) {
                throw new IllegalArgumentException("left must be less than right");
            }
            if (bottom.compareTo(top) >= 0) {
                throw new IllegalArgumentException("bottom must be less than top");
            }
            if (near.signum() < 0) {
                throw new IllegalArgumentException("near must be non-negative");
            }
            if (far.compareTo(near) <= 0) {
                throw new IllegalArgumentException("far must be greater than near");
            }
            if (zoom.signum() <= 0) {
                throw new IllegalArgumentException("zoom must be greater than 0");
            }
        }
    }

    /**
     * Built-in Orthographic Camera 3D projection state.
     *
     * @param identity stable authored component identity
     * @param projection orthographic bounds and magnification
     * @param primary whether this is an authored primary camera
     */
    public record OrthographicCamera3d(ComponentIdentity identity, OrthographicProjection projection, boolean primary)
            implements Camera3d {
        /** Validates one orthographic camera projection. */
        public OrthographicCamera3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(projection, "projection");
        }

        @Override
        public BigDecimal near() {
            return projection.near();
        }

        @Override
        public BigDecimal far() {
            return projection.far();
        }
    }

    /**
     * Stable mapping from a projected visual component to its expanded and authored identities.
     *
     * @param occurrence expanded owning entity occurrence
     * @param scope authored definition occurrence containing the component
     * @param authoredEntity local entity identity within the authored scope
     * @param component stable component identity within the authored entity
     */
    public record ComponentIdentity(
            CompositionOccurrenceId occurrence,
            CompositionScope scope,
            EntityId authoredEntity,
            ComponentId component) {
        /** Validates one projected component identity. */
        public ComponentIdentity {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(authoredEntity, "authoredEntity");
            Objects.requireNonNull(component, "component");
        }
    }

    /**
     * Three exact decimal coordinates used by projected visual semantics.
     *
     * @param x first coordinate
     * @param y second coordinate
     * @param z third coordinate
     */
    public record Vector3(BigDecimal x, BigDecimal y, BigDecimal z) {
        /** Validates one vector. */
        public Vector3 {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(z, "z");
        }
    }

    /**
     * Built-in Transform 3D visual state.
     *
     * @param identity stable authored component identity
     * @param position local XYZ position
     * @param orientationDegrees local Euler orientation in canonical XYZ order
     * @param scale local XYZ scale
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
     * Built-in Mesh Renderer 3D visual state with unrealized resources.
     *
     * @param identity stable authored component identity
     * @param mesh mesh resource reference
     * @param material material resource reference
     * @param visible local mesh visibility
     */
    public record MeshRenderer3d(
            ComponentIdentity identity, ResourceReference mesh, ResourceReference material, boolean visible) {
        /** Validates one Mesh Renderer 3D projection. */
        public MeshRenderer3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    /**
     * Built-in Directional Light 3D visual state.
     *
     * @param identity stable authored component identity
     * @param color linear-sRGB light color
     * @param intensity linear intensity multiplier
     * @param target world-space target point
     */
    public record DirectionalLight3d(ComponentIdentity identity, Vector3 color, BigDecimal intensity, Vector3 target) {
        /** Validates one Directional Light 3D projection. */
        public DirectionalLight3d {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(intensity, "intensity");
            Objects.requireNonNull(target, "target");
        }
    }

    /** Rejects a projected component attached to another occurrence. */
    private static void requireOwner(ComponentIdentity identity, CompositionOccurrenceId occurrence) {
        if (!identity.occurrence().equals(occurrence)) {
            throw new IllegalArgumentException("projected component must belong to its visual occurrence");
        }
    }

    /** Rejects a decimal that cannot be represented by the renderer's finite float domain. */
    private static void requireFinite(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (!Float.isFinite(value.floatValue())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
