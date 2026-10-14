/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraIconState.Bounds;
import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraProjection.ProjectedPoint;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Camera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.PerspectiveCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Owns authored-camera icon presentation, picking, frustums, and renderer resource lifetime. */
final class SceneCameraVisualizations implements Overlay, AutoCloseable {
    static final float ICON_WIDTH = 36.0f;
    static final float ICON_HEIGHT = 26.0f;

    private final Supplier<? extends Camera> editorCamera;
    private final float sceneScale;
    private final SceneCameraIconRenderer iconRenderer = new SceneCameraIconRenderer();
    private final Map<CompositionOccurrenceId, Entry> entries = new LinkedHashMap<>();

    private @Nullable CompositionOccurrenceId selectedOccurrence;
    private boolean closed;

    SceneCameraVisualizations(Supplier<? extends Camera> editorCamera, float sceneScale) {
        this.editorCamera = Objects.requireNonNull(editorCamera, "editorCamera");
        if (!Float.isFinite(sceneScale) || sceneScale <= 0.0f) {
            throw new IllegalArgumentException("sceneScale must be finite and positive");
        }
        this.sceneScale = sceneScale;
    }

    /** Reconciles camera visualization ownership with one newly realized Scene snapshot. */
    void replace(SceneViewRealizedContent content, float aspectRatio) {
        requireOpen();
        Objects.requireNonNull(content, "content");
        requireAspect(aspectRatio);
        Map<CompositionOccurrenceId, Entry> previous = new LinkedHashMap<>(entries);
        entries.clear();
        for (Map.Entry<CompositionOccurrenceId, VisualOccurrence> item :
                content.occurrenceDescriptions().entrySet()) {
            VisualOccurrence description = item.getValue();
            Object3D object = content.occurrenceObjects().get(item.getKey());
            if (object != null && !description.cameras().isEmpty()) {
                Entry entry = previous.remove(item.getKey());
                if (entry == null) {
                    entry = new Entry(object, description, aspectRatio);
                } else {
                    entry.replace(object, description, aspectRatio);
                }
                entries.put(item.getKey(), entry);
            }
        }
        previous.values().forEach(Entry::close);
        if (selectedOccurrence != null && !entries.containsKey(selectedOccurrence)) {
            selectedOccurrence = null;
        }
        updateFrustumVisibility();
    }

    /** Rebuilds only perspective frustums when the Scene View aspect changes. */
    void resize(float aspectRatio) {
        requireOpen();
        requireAspect(aspectRatio);
        entries.values().forEach(entry -> entry.resize(aspectRatio));
        updateFrustumVisibility();
    }

    /** Updates the occurrence whose icon and selected-only frustums are highlighted. */
    void select(@Nullable CompositionOccurrenceId occurrence) {
        requireOpen();
        selectedOccurrence = occurrence != null && entries.containsKey(occurrence) ? occurrence : null;
        updateFrustumVisibility();
    }

    /** Picks one rendered icon, preferring selection, then nearest depth, then stable identity. */
    Optional<CompositionOccurrenceId> pick(float horizontal, float vertical, int width, int height) {
        requireOpen();
        if (!Float.isFinite(horizontal) || !Float.isFinite(vertical)) {
            throw new IllegalArgumentException("pointer coordinates must be finite");
        }
        Comparator<SceneCameraIconState> priority = Comparator.comparing(SceneCameraIconState::selected)
                .reversed()
                .thenComparingDouble(SceneCameraIconState::depth)
                .thenComparing(state -> state.occurrence().toString());
        return renderState(width, height).stream()
                .filter(state -> state.bounds().contains(horizontal, vertical))
                .min(priority)
                .map(SceneCameraIconState::occurrence);
    }

    /** Builds current visible logical-pixel icon state without allocating renderer resources. */
    List<SceneCameraIconState> renderState(int width, int height) {
        requireOpen();
        Camera camera = editorCamera.get();
        List<SceneCameraIconState> state = new ArrayList<>();
        for (Map.Entry<CompositionOccurrenceId, Entry> item : entries.entrySet()) {
            Entry entry = item.getValue();
            if (!entry.visible()) {
                continue;
            }
            Optional<ProjectedPoint> projected = SceneCameraProjection.project(entry.object(), camera, width, height);
            if (projected.isEmpty()) {
                continue;
            }
            ProjectedPoint point = projected.orElseThrow();
            List<Camera3d> cameras = entry.cameras();
            boolean perspective = cameras.stream().anyMatch(PerspectiveCamera3d.class::isInstance);
            boolean orthographic = cameras.stream().anyMatch(OrthographicCamera3d.class::isInstance);
            state.add(new SceneCameraIconState(
                    item.getKey(),
                    point.point(),
                    point.depth(),
                    new Bounds(
                            point.point().x() - ICON_WIDTH * 0.5f,
                            point.point().y() - ICON_HEIGHT * 0.5f,
                            ICON_WIDTH,
                            ICON_HEIGHT),
                    cameras.size(),
                    perspective,
                    orthographic,
                    item.getKey().equals(selectedOccurrence)));
        }
        return List.copyOf(state);
    }

    /** Returns a small world-space framing bound for one enabled camera occurrence. */
    Optional<BoundingBox> focusBounds(CompositionOccurrenceId occurrence) {
        requireOpen();
        Entry entry = entries.get(Objects.requireNonNull(occurrence, "occurrence"));
        if (entry == null || !entry.visible()) {
            return Optional.empty();
        }
        Vector3f center = entry.object().worldPosition(new Vector3f());
        float halfExtent = sceneScale * 0.025f;
        return Optional.of(new BoundingBox(
                center.x() - halfExtent,
                center.y() - halfExtent,
                center.z() - halfExtent,
                center.x() + halfExtent,
                center.y() + halfExtent,
                center.z() + halfExtent));
    }

    /** Runs a bounds operation with editor frustums excluded, then restores selected-only state. */
    <T> T excludingFrustums(Supplier<T> operation) {
        requireOpen();
        entries.values().forEach(entry -> entry.setFrustumsVisible(false));
        try {
            return Objects.requireNonNull(operation, "operation").get();
        } finally {
            updateFrustumVisibility();
        }
    }

    /** Exposes current owned frustums for focused lifecycle tests. */
    List<SceneCameraFrustum> frustumsFor(CompositionOccurrenceId occurrence) {
        requireOpen();
        Entry entry = entries.get(Objects.requireNonNull(occurrence, "occurrence"));
        return entry == null ? List.of() : entry.frustums();
    }

    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        requireOpen();
        Objects.requireNonNull(canvas, "canvas");
        renderState(width, height).forEach(state -> iconRenderer.paint(canvas, state));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        entries.values().forEach(Entry::close);
        entries.clear();
        selectedOccurrence = null;
    }

    private void updateFrustumVisibility() {
        entries.forEach((occurrence, entry) ->
                entry.setFrustumsVisible(occurrence.equals(selectedOccurrence) && entry.visible()));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene camera visualizations are closed");
        }
    }

    private static void requireAspect(float aspectRatio) {
        if (!Float.isFinite(aspectRatio) || aspectRatio <= 0.0f) {
            throw new IllegalArgumentException("aspectRatio must be finite and positive");
        }
    }

    /** One occurrence's icon semantics and reusable camera-local frustum resources. */
    private final class Entry implements AutoCloseable {
        private Object3D object;
        private VisualOccurrence description;
        private List<SceneCameraFrustum> frustums = List.of();

        Entry(Object3D object, VisualOccurrence description, float aspectRatio) {
            this.object = object;
            this.description = description;
            reconcileFrustums(aspectRatio);
        }

        Object3D object() {
            return object;
        }

        List<Camera3d> cameras() {
            return description.cameras();
        }

        boolean visible() {
            if (!description.enabled()) {
                return false;
            }
            Object3D current = object;
            while (current != null) {
                if (!current.isVisible()) {
                    return false;
                }
                current = current.parent();
            }
            return true;
        }

        List<SceneCameraFrustum> frustums() {
            return frustums;
        }

        void replace(Object3D replacementObject, VisualOccurrence replacementDescription, float aspectRatio) {
            object = replacementObject;
            description = replacementDescription;
            reconcileFrustums(aspectRatio);
        }

        void resize(float aspectRatio) {
            reconcileFrustums(aspectRatio);
        }

        void setFrustumsVisible(boolean visible) {
            frustums.forEach(frustum -> frustum.setVisible(visible));
        }

        @Override
        public void close() {
            frustums.forEach(SceneCameraFrustum::close);
            frustums = List.of();
        }

        private void reconcileFrustums(float aspectRatio) {
            List<SceneCameraFrustum> previous = new ArrayList<>(frustums);
            List<SceneCameraFrustum> replacement = new ArrayList<>();
            List<Camera3d> cameras = description.cameras();
            for (int index = 0; index < cameras.size(); index++) {
                Camera3d camera = cameras.get(index);
                SceneCameraFrustum reusable = previous.stream()
                        .filter(candidate -> candidate.description().equals(camera))
                        .filter(candidate ->
                                !(camera instanceof PerspectiveCamera3d) || candidate.aspectRatio() == aspectRatio)
                        .findFirst()
                        .orElse(null);
                if (reusable != null) {
                    previous.remove(reusable);
                    reusable.attach(object);
                    replacement.add(reusable);
                } else {
                    int styleIndex = index;
                    SceneCameraFrustumGeometry.create(camera, aspectRatio, sceneScale)
                            .map(shape -> new SceneCameraFrustum(camera, aspectRatio, shape, styleIndex))
                            .ifPresent(frustum -> {
                                frustum.attach(object);
                                replacement.add(frustum);
                            });
                }
            }
            previous.forEach(SceneCameraFrustum::close);
            frustums = List.copyOf(replacement);
        }
    }
}
