/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.sceneview;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Camera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.DirectionalLight3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.MeshRenderer3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicProjection;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.PerspectiveCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionComponent;
import io.github.glynch.jscene3d.project.composition.CompositionEntity;
import io.github.glynch.jscene3d.project.composition.CompositionPlan;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Converts a safe generic composition plan into the deliberately narrow first Scene View snapshot. */
public final class SceneViewProjector {
    /** Prevents construction of this stateless projector. */
    private SceneViewProjector() {
        throw new AssertionError("SceneViewProjector cannot be instantiated");
    }

    /**
     * Projects supported product-owned visual semantics without creating runtime or renderer objects.
     *
     * <p>Unknown and project-defined component types are intentionally ignored. The supplied plan must describe the
     * same validated Scene revision identified by the caller.
     *
     * @param plan complete runtime-free Scene composition
     * @param revision authored working-copy revision represented by the plan
     * @return immutable Scene View snapshot
     * @throws IllegalArgumentException if the plan is not a Scene or supported visual values are inconsistent
     */
    public static SceneViewSnapshot project(CompositionPlan plan, long revision) {
        CompositionPlan validPlan = Objects.requireNonNull(plan, "plan");
        if (validPlan.kind() != CompositionPlan.Kind.SCENE_DEFINITION) {
            throw new IllegalArgumentException("Scene View projection requires a Scene composition plan");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        return new SceneViewSnapshot(
                validPlan.rootDefinition(),
                revision,
                validPlan.entities().stream()
                        .map(SceneViewProjector::projectOccurrence)
                        .toList());
    }

    /** Projects one entity occurrence and only its supported built-in visual components. */
    private static VisualOccurrence projectOccurrence(CompositionEntity entity) {
        Optional<Transform3d> transform = Optional.empty();
        List<MeshRenderer3d> meshes = new ArrayList<>();
        Optional<DirectionalLight3d> directionalLight = Optional.empty();
        List<Camera3d> cameras = new ArrayList<>();
        for (CompositionComponent component : entity.components()) {
            if (component.descriptor().type().equals(StandardSpatial3dDescriptors.transformType())) {
                if (transform.isPresent()) {
                    throw invalid(component, "multiple Transform 3D components were planned for one occurrence");
                }
                transform = Optional.of(transform(component));
            } else if (component.descriptor().type().equals(StandardSpatial3dDescriptors.meshRendererType())) {
                meshes.add(mesh(component));
            } else if (component.descriptor().type().equals(StandardSpatial3dDescriptors.directionalLightType())) {
                if (directionalLight.isPresent()) {
                    throw invalid(
                            component, "multiple Directional Light 3D components were planned for one occurrence");
                }
                directionalLight = Optional.of(directionalLight(component));
            } else if (component.descriptor().type().equals(StandardSpatial3dDescriptors.perspectiveCameraType())) {
                cameras.add(perspectiveCamera(component));
            } else if (component.descriptor().type().equals(StandardSpatial3dDescriptors.orthographicCameraType())) {
                cameras.add(orthographicCamera(component));
            }
        }
        return new VisualOccurrence(
                entity.occurrence(),
                entity.parent(),
                entity.authoredAsset(),
                entity.authoredSource(),
                entity.authoredId(),
                entity.name(),
                entity.effectivelyEnabled(),
                transform,
                meshes,
                directionalLight,
                cameras);
    }

    /** Projects Transform 3D values using stable standard property identities. */
    private static Transform3d transform(CompositionComponent component) {
        return new Transform3d(
                identity(component),
                vector(component, StandardSpatial3dDescriptors.positionProperty()),
                vector(component, StandardSpatial3dDescriptors.orientationProperty()),
                vector(component, StandardSpatial3dDescriptors.scaleProperty()));
    }

    /** Projects Mesh Renderer 3D values without realizing either resource. */
    private static MeshRenderer3d mesh(CompositionComponent component) {
        return new MeshRenderer3d(
                identity(component),
                reference(component, StandardSpatial3dDescriptors.meshProperty()),
                reference(component, StandardSpatial3dDescriptors.materialProperty()),
                booleanValue(component, StandardSpatial3dDescriptors.visibleProperty()));
    }

    /** Projects Directional Light 3D values using stable standard property identities. */
    private static DirectionalLight3d directionalLight(CompositionComponent component) {
        return new DirectionalLight3d(
                identity(component),
                vector(component, StandardSpatial3dDescriptors.colorProperty()),
                number(component, StandardSpatial3dDescriptors.intensityProperty()),
                vector(component, StandardSpatial3dDescriptors.targetProperty()));
    }

    /** Projects Perspective Camera 3D values using stable standard property identities. */
    private static PerspectiveCamera3d perspectiveCamera(CompositionComponent component) {
        return new PerspectiveCamera3d(
                identity(component),
                number(component, StandardSpatial3dDescriptors.fieldOfViewDegreesProperty()),
                number(component, StandardSpatial3dDescriptors.nearProperty()),
                number(component, StandardSpatial3dDescriptors.farProperty()),
                booleanValue(component, StandardSpatial3dDescriptors.primaryProperty()));
    }

    /** Projects Orthographic Camera 3D values using stable standard property identities. */
    private static OrthographicCamera3d orthographicCamera(CompositionComponent component) {
        return new OrthographicCamera3d(
                identity(component),
                new OrthographicProjection(
                        number(component, StandardSpatial3dDescriptors.leftProperty()),
                        number(component, StandardSpatial3dDescriptors.rightProperty()),
                        number(component, StandardSpatial3dDescriptors.topProperty()),
                        number(component, StandardSpatial3dDescriptors.bottomProperty()),
                        number(component, StandardSpatial3dDescriptors.nearProperty()),
                        number(component, StandardSpatial3dDescriptors.farProperty()),
                        number(component, StandardSpatial3dDescriptors.zoomProperty())),
                booleanValue(component, StandardSpatial3dDescriptors.primaryProperty()));
    }

    /** Preserves expanded and authored component identities for later selection mapping. */
    private static ComponentIdentity identity(CompositionComponent component) {
        return new ComponentIdentity(
                component.owner(),
                component.scope(),
                component.authoredEntity(),
                component.definition().id());
    }

    /** Reads one exact three-number property. */
    private static Vector3 vector(CompositionComponent component, PropertyId property) {
        ProjectValue value = value(component, property);
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != 3) {
            throw invalid(component, property + " must be a three-number array");
        }
        return new Vector3(
                number(component, property, array.values().get(0)),
                number(component, property, array.values().get(1)),
                number(component, property, array.values().get(2)));
    }

    /** Reads one exact number property. */
    private static BigDecimal number(CompositionComponent component, PropertyId property) {
        return number(component, property, value(component, property));
    }

    /** Reads one number member while preserving its exact decimal representation. */
    private static BigDecimal number(CompositionComponent component, PropertyId property, ProjectValue value) {
        if (value instanceof ProjectValue.NumberValue number) {
            return number.value();
        }
        throw invalid(component, property + " must be a number");
    }

    /** Reads one exact boolean property. */
    private static boolean booleanValue(CompositionComponent component, PropertyId property) {
        ProjectValue value = value(component, property);
        if (value instanceof ProjectValue.BooleanValue booleanValue) {
            return booleanValue.value();
        }
        throw invalid(component, property + " must be a boolean");
    }

    /** Reads one unrealized resource reference. */
    private static ResourceReference reference(CompositionComponent component, PropertyId property) {
        ProjectValue value = value(component, property);
        if (value instanceof ProjectValue.ReferenceValue reference) {
            return reference.reference();
        }
        throw invalid(component, property + " must be a resource reference");
    }

    /** Reads one planner-supplied effective value. */
    private static ProjectValue value(CompositionComponent component, PropertyId property) {
        return Optional.ofNullable(component.effectiveProperties().get(property))
                .orElseThrow(() -> invalid(component, "missing effective property " + property))
                .value();
    }

    /** Creates a context-rich, transport-independent projection failure. */
    private static IllegalArgumentException invalid(CompositionComponent component, String detail) {
        return new IllegalArgumentException(component.location() + ": " + detail);
    }
}
