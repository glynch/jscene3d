/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Code;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.DirectionalLight3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.MeshRenderer3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** One fully realized authored graph and the graph-scoped resources it retains. */
record SceneViewRealizedContent(
        long revision,
        Object3D root,
        Map<CompositionOccurrenceId, Object3D> occurrenceObjects,
        Map<CompositionOccurrenceId, VisualOccurrence> occurrenceDescriptions,
        IdentityHashMap<Object3D, SceneViewObjectIdentity> identities,
        List<AutoCloseable> retainedResources)
        implements AutoCloseable {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    static SceneViewRealizedContent realize(SceneViewSnapshot snapshot, SceneViewResourceCache resources) {
        Object3D root = new Object3D();
        Map<CompositionOccurrenceId, Object3D> occurrences = new LinkedHashMap<>();
        Map<CompositionOccurrenceId, VisualOccurrence> descriptions = new LinkedHashMap<>();
        IdentityHashMap<Object3D, SceneViewObjectIdentity> identities = new IdentityHashMap<>();
        List<AutoCloseable> retained = new ArrayList<>();
        try {
            for (VisualOccurrence occurrence : snapshot.occurrences()) {
                Object3D node = realizeOccurrence(occurrence);
                if (occurrences.putIfAbsent(occurrence.occurrence(), node) != null) {
                    throw new IllegalArgumentException("snapshot contains a duplicate occurrence identity");
                }
                descriptions.put(occurrence.occurrence(), occurrence);
                identities.put(node, SceneViewObjectIdentity.occurrence(occurrence.occurrence()));
                realizeComponents(occurrence, node, resources, identities, retained);
            }
            attachHierarchy(snapshot.occurrences(), root, occurrences);
            return new SceneViewRealizedContent(
                    snapshot.revision(), root, occurrences, descriptions, identities, retained);
        } catch (RuntimeException failure) {
            closeRetainedSuppressing(retained, failure);
            throw failure;
        }
    }

    private static Object3D realizeOccurrence(VisualOccurrence occurrence) {
        Object3D node = new Object3D();
        node.setVisible(occurrence.enabled());
        occurrence.transform().ifPresent(transform -> applyTransform(node, transform));
        return node;
    }

    private static void realizeComponents(
            VisualOccurrence occurrence,
            Object3D node,
            SceneViewResourceCache resources,
            IdentityHashMap<Object3D, SceneViewObjectIdentity> identities,
            List<AutoCloseable> retained) {
        for (MeshRenderer3d description : occurrence.meshes()) {
            Mesh mesh = realizeMesh(description, resources, retained);
            node.add(mesh);
            identities.put(mesh, SceneViewObjectIdentity.component(description.identity()));
        }
        occurrence.directionalLight().ifPresent(description -> {
            DirectionalLight light = realizeLight(description);
            node.add(light);
            identities.put(light, SceneViewObjectIdentity.component(description.identity()));
        });
    }

    private static Mesh realizeMesh(
            MeshRenderer3d description, SceneViewResourceCache resources, List<AutoCloseable> retained) {
        SceneViewResourceCache.Retained<BufferGeometry> mesh = retainMesh(resources, description.mesh());
        SceneViewResourceCache.Retained<Material> material = null;
        try {
            material = retainMaterial(resources, description.material());
            Mesh realized = new Mesh(mesh.value(), material.value());
            realized.setVisible(description.visible());
            retained.add(mesh);
            retained.add(material);
            return realized;
        } catch (RuntimeException failure) {
            if (material != null) {
                material.close();
            }
            mesh.close();
            throw failure;
        }
    }

    private static SceneViewResourceCache.Retained<BufferGeometry> retainMesh(
            SceneViewResourceCache resources, ResourceReference reference) {
        try {
            return resources.retainMesh(reference);
        } catch (UnsupportedSceneViewResourceException failure) {
            throw new RealizationFailure(Code.RESOURCE_UNSUPPORTED, reference, message(failure), failure);
        } catch (RuntimeException failure) {
            throw new RealizationFailure(Code.RESOURCE_FAILURE, reference, message(failure), failure);
        }
    }

    private static SceneViewResourceCache.Retained<Material> retainMaterial(
            SceneViewResourceCache resources, ResourceReference reference) {
        try {
            return resources.retainMaterial(reference);
        } catch (UnsupportedSceneViewResourceException failure) {
            throw new RealizationFailure(Code.RESOURCE_UNSUPPORTED, reference, message(failure), failure);
        } catch (RuntimeException failure) {
            throw new RealizationFailure(Code.RESOURCE_FAILURE, reference, message(failure), failure);
        }
    }

    private static DirectionalLight realizeLight(DirectionalLight3d description) {
        Vector3 color = description.color();
        DirectionalLight light = new DirectionalLight(
                Color.linear(value(color.x()), value(color.y()), value(color.z())), value(description.intensity()));
        light.setPosition(0.0f, 0.0f, 0.0f);
        Vector3 target = description.target();
        light.setTarget(value(target.x()), value(target.y()), value(target.z()));
        return light;
    }

    private static void applyTransform(Object3D node, Transform3d transform) {
        Vector3 position = transform.position();
        node.setPosition(value(position.x()), value(position.y()), value(position.z()));
        Vector3 orientation = transform.orientationDegrees();
        node.setRotationFromEuler(
                value(orientation.x()) * DEGREES_TO_RADIANS,
                value(orientation.y()) * DEGREES_TO_RADIANS,
                value(orientation.z()) * DEGREES_TO_RADIANS,
                RotationOrder.XYZ);
        Vector3 scale = transform.scale();
        node.setScale(value(scale.x()), value(scale.y()), value(scale.z()));
    }

    private static void attachHierarchy(
            List<VisualOccurrence> descriptions, Object3D root, Map<CompositionOccurrenceId, Object3D> occurrences) {
        for (VisualOccurrence description : descriptions) {
            Object3D node = occurrences.get(description.occurrence());
            if (node == null) {
                throw new IllegalStateException("realized occurrence is missing");
            }
            Object3D parent = description
                    .parent()
                    .map(parentId -> Optional.ofNullable(occurrences.get(parentId))
                            .orElseThrow(() ->
                                    new IllegalArgumentException("snapshot occurrence parent is missing: " + parentId)))
                    .orElse(root);
            parent.add(node);
        }
    }

    private static float value(BigDecimal number) {
        float value = Objects.requireNonNull(number, "number").floatValue();
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("snapshot number must be finite");
        }
        return value;
    }

    @Override
    public void close() {
        root.detach();
        @Nullable RuntimeException failure = closeRetained(retainedResources, null);
        if (failure != null) {
            throw failure;
        }
    }

    private static void closeRetainedSuppressing(List<AutoCloseable> retained, RuntimeException original) {
        RuntimeException failure = closeRetained(retained, null);
        if (failure != null) {
            original.addSuppressed(failure);
        }
    }

    private static @Nullable RuntimeException closeRetained(
            List<AutoCloseable> retained, @Nullable RuntimeException previous) {
        RuntimeException failure = previous;
        for (int index = retained.size() - 1; index >= 0; index--) {
            try {
                retained.get(index).close();
            } catch (RuntimeException exception) {
                failure = retainFailure(failure, exception);
            } catch (Exception exception) {
                failure = retainFailure(
                        failure, new IllegalStateException("Unable to release Scene View resource", exception));
            }
        }
        return failure;
    }

    private static RuntimeException retainFailure(@Nullable RuntimeException retained, RuntimeException next) {
        if (retained == null) {
            return next;
        }
        retained.addSuppressed(next);
        return retained;
    }

    private static String message(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? "Scene View realization failed" : message;
    }

    /** Resource-scoped realization failure translated by the renderer session. */
    static final class RealizationFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final Code code;
        private final ResourceReference resource;

        private RealizationFailure(Code code, ResourceReference resource, String message, RuntimeException cause) {
            super(message, cause);
            this.code = Objects.requireNonNull(code, "code");
            this.resource = Objects.requireNonNull(resource, "resource");
        }

        Code code() {
            return code;
        }

        ResourceReference resource() {
            return resource;
        }
    }
}
