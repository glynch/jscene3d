/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Code;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Diagnostic;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Status;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.DirectionalLight3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.MeshRenderer3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.helpers.AxesHelper;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;

/** Renderer-side owner of one safe Scene View graph, editor camera, helpers, and resource cache. */
final class SceneViewRendererSession implements AutoCloseable {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    private final Scene scene = new Scene();
    private final PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1000.0f);
    private final GridHelper grid = new GridHelper(20.0f, 20);
    private final AxesHelper axes = new AxesHelper(2.0f);
    private final SceneViewResourceCache resources;

    private @Nullable RealizedContent content;
    private @Nullable AssetId sceneId;
    private boolean closed;

    SceneViewRendererSession(SceneViewResourceResolver resolver) {
        resources = new SceneViewResourceCache(Objects.requireNonNull(resolver, "resolver"));
        camera.setPosition(6.0f, 4.0f, 6.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        scene.add(camera);
        scene.add(grid);
        scene.add(axes);
    }

    SceneViewRealizationResult replaceSnapshot(SceneViewSnapshot snapshot) {
        requireOpen();
        SceneViewSnapshot validSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        if (sceneId != null && !sceneId.equals(validSnapshot.scene())) {
            return failure(
                    Code.SCENE_MISMATCH, "snapshot Scene does not match this renderer session", Optional.empty());
        }
        if (content != null && validSnapshot.revision() < content.revision()) {
            return SceneViewRealizationResult.success(Status.STALE, content.revision());
        }
        if (content != null && validSnapshot.revision() == content.revision()) {
            return SceneViewRealizationResult.success(Status.UNCHANGED, content.revision());
        }

        RealizedContent replacement;
        try {
            replacement = RealizedContent.realize(validSnapshot, resources);
        } catch (RealizationFailure failure) {
            return failure(failure.code, message(failure), Optional.of(failure.resource));
        } catch (RuntimeException failure) {
            return failure(Code.INVALID_SNAPSHOT, message(failure), Optional.empty());
        }

        RealizedContent previous = content;
        scene.add(replacement.root());
        if (previous != null) {
            previous.root().detach();
        }
        content = replacement;
        sceneId = validSnapshot.scene();
        if (previous != null) {
            try {
                previous.close();
            } catch (RuntimeException failure) {
                return SceneViewRealizationResult.failure(
                        OptionalLong.of(replacement.revision()),
                        new Diagnostic(Code.CLEANUP_FAILURE, message(failure), Optional.empty()));
            }
        }
        return SceneViewRealizationResult.success(Status.APPLIED, replacement.revision());
    }

    void render(Renderer renderer, float aspectRatio) {
        requireOpen();
        camera.setAspectRatio(aspectRatio);
        Objects.requireNonNull(renderer, "renderer").render(scene, camera);
    }

    Scene scene() {
        requireOpen();
        return scene;
    }

    PerspectiveCamera camera() {
        requireOpen();
        return camera;
    }

    GridHelper grid() {
        requireOpen();
        return grid;
    }

    AxesHelper axes() {
        requireOpen();
        return axes;
    }

    OptionalLong revision() {
        requireOpen();
        return content == null ? OptionalLong.empty() : OptionalLong.of(content.revision());
    }

    Optional<Object3D> objectFor(CompositionOccurrenceId occurrence) {
        requireOpen();
        return content == null
                ? Optional.empty()
                : Optional.ofNullable(
                        content.occurrenceObjects().get(Objects.requireNonNull(occurrence, "occurrence")));
    }

    Optional<SceneViewObjectIdentity> identityFor(Object3D object) {
        requireOpen();
        return content == null
                ? Optional.empty()
                : Optional.ofNullable(content.identities().get(Objects.requireNonNull(object, "object")));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException failure = null;
        if (content != null) {
            try {
                content.close();
            } catch (RuntimeException exception) {
                failure = exception;
            }
            content = null;
        }
        try {
            resources.close();
        } catch (RuntimeException exception) {
            failure = retainFailure(failure, exception);
        }
        try {
            axes.close();
        } catch (RuntimeException exception) {
            failure = retainFailure(failure, exception);
        }
        try {
            grid.close();
        } catch (RuntimeException exception) {
            failure = retainFailure(failure, exception);
        }
        scene.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private SceneViewRealizationResult failure(Code code, String detail, Optional<ResourceReference> resource) {
        OptionalLong currentRevision = content == null ? OptionalLong.empty() : OptionalLong.of(content.revision());
        return SceneViewRealizationResult.failure(currentRevision, new Diagnostic(code, detail, resource));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene View renderer session is closed");
        }
    }

    private static String message(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? "Scene View realization failed" : message;
    }

    private static RuntimeException retainFailure(@Nullable RuntimeException retained, RuntimeException next) {
        if (retained == null) {
            return next;
        }
        retained.addSuppressed(next);
        return retained;
    }

    /** One fully built authored graph and all graph-specific resource retentions. */
    private record RealizedContent(
            long revision,
            Object3D root,
            Map<CompositionOccurrenceId, Object3D> occurrenceObjects,
            IdentityHashMap<Object3D, SceneViewObjectIdentity> identities,
            List<AutoCloseable> retainedResources)
            implements AutoCloseable {
        private static RealizedContent realize(SceneViewSnapshot snapshot, SceneViewResourceCache resources) {
            Object3D root = new Object3D();
            Map<CompositionOccurrenceId, Object3D> occurrences = new LinkedHashMap<>();
            IdentityHashMap<Object3D, SceneViewObjectIdentity> identities = new IdentityHashMap<>();
            List<AutoCloseable> retained = new ArrayList<>();
            try {
                for (VisualOccurrence occurrence : snapshot.occurrences()) {
                    Object3D node = realizeOccurrence(occurrence);
                    if (occurrences.putIfAbsent(occurrence.occurrence(), node) != null) {
                        throw new IllegalArgumentException("snapshot contains a duplicate occurrence identity");
                    }
                    identities.put(node, SceneViewObjectIdentity.occurrence(occurrence.occurrence()));
                    realizeComponents(occurrence, node, resources, identities, retained);
                }
                attachHierarchy(snapshot.occurrences(), root, occurrences);
                return new RealizedContent(snapshot.revision(), root, occurrences, identities, retained);
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
                List<VisualOccurrence> descriptions,
                Object3D root,
                Map<CompositionOccurrenceId, Object3D> occurrences) {
            for (VisualOccurrence description : descriptions) {
                Object3D node = occurrences.get(description.occurrence());
                if (node == null) {
                    throw new IllegalStateException("realized occurrence is missing");
                }
                Object3D parent = description
                        .parent()
                        .map(parentId -> Optional.ofNullable(occurrences.get(parentId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "snapshot occurrence parent is missing: " + parentId)))
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
    }

    /** Resource-scoped failure translated into a structured Scene View diagnostic. */
    private static final class RealizationFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final Code code;
        private final ResourceReference resource;

        private RealizationFailure(Code code, ResourceReference resource, String message, RuntimeException cause) {
            super(message, cause);
            this.code = Objects.requireNonNull(code, "code");
            this.resource = Objects.requireNonNull(resource, "resource");
        }
    }
}
