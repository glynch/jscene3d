/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Code;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Diagnostic;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Status;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizedContent.RealizationFailure;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.helpers.BoxHelper;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.raycasting.Raycaster;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;

/** Renderer-side owner of one safe Scene View graph, editor camera, helpers, and resource cache. */
final class SceneViewRendererSession implements AutoCloseable {
    private final Scene scene = new Scene();
    private final SceneViewEditorCamera editorCamera = new SceneViewEditorCamera();
    private final GridHelper grid = new GridHelper(20.0f, 20);
    private final SceneOrientationGizmo orientationGizmo = new SceneOrientationGizmo(editorCamera::active);
    private final SceneTransformGizmo transformGizmo = new SceneTransformGizmo(editorCamera::active);
    private final SceneViewResourceCache resources;
    private final Raycaster raycaster = new Raycaster();

    private @Nullable SceneViewRealizedContent content;
    private @Nullable AssetId sceneId;
    private @Nullable CompositionOccurrenceId selectedOccurrence;
    private @Nullable BoxHelper selectionHelper;
    private boolean closed;

    SceneViewRendererSession(SceneViewResourceResolver resolver) {
        resources = new SceneViewResourceCache(Objects.requireNonNull(resolver, "resolver"));
        scene.add(grid);
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

        SceneViewRealizedContent replacement;
        try {
            replacement = SceneViewRealizedContent.realize(validSnapshot, resources);
        } catch (RealizationFailure failure) {
            return failure(failure.code(), message(failure), Optional.of(failure.resource()));
        } catch (RuntimeException failure) {
            return failure(Code.INVALID_SNAPSHOT, message(failure), Optional.empty());
        }

        SceneViewRealizedContent previous = content;
        scene.add(replacement.root());
        if (previous != null) {
            previous.root().detach();
        }
        content = replacement;
        sceneId = validSnapshot.scene();
        restoreSelection();
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

    void render(Renderer renderer, int viewportWidth, int viewportHeight) {
        requireOpen();
        updateViewportSize(viewportWidth, viewportHeight);
        Renderer validRenderer = Objects.requireNonNull(renderer, "renderer");
        validRenderer.render(scene, editorCamera.active());
        validRenderer.render(transformGizmo);
        validRenderer.render(orientationGizmo);
    }

    void updateViewportSize(int viewportWidth, int viewportHeight) {
        requireOpen();
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        editorCamera.resize(viewportWidth, viewportHeight);
    }

    Scene scene() {
        requireOpen();
        return scene;
    }

    Camera camera() {
        requireOpen();
        return editorCamera.active();
    }

    GridHelper grid() {
        requireOpen();
        return grid;
    }

    SceneOrientationGizmo orientationGizmo() {
        requireOpen();
        return orientationGizmo;
    }

    SceneTransformGizmo transformGizmo() {
        requireOpen();
        return transformGizmo;
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

    SceneViewSelectionResult pick(long expectedRevision, float horizontal, float vertical) {
        requireOpen();
        SceneViewRealizedContent current = content;
        if (current == null) {
            return SceneViewSelectionResult.stale(0L);
        }
        if (current.revision() != expectedRevision) {
            return SceneViewSelectionResult.stale(current.revision());
        }
        raycaster.setFromCamera(horizontal, vertical, editorCamera.active());
        Optional<CompositionOccurrenceId> picked = raycaster.intersect(current.root()).stream()
                .map(hit -> current.identities().get(hit.mesh()))
                .filter(Objects::nonNull)
                .map(SceneViewObjectIdentity::occurrence)
                .findFirst();
        select(expectedRevision, picked);
        return picked.map(occurrence -> SceneViewSelectionResult.selected(expectedRevision, occurrence))
                .orElseGet(() -> SceneViewSelectionResult.cleared(expectedRevision));
    }

    boolean select(long expectedRevision, Optional<CompositionOccurrenceId> occurrence) {
        requireOpen();
        Objects.requireNonNull(occurrence, "occurrence");
        SceneViewRealizedContent current = content;
        if (current == null || current.revision() != expectedRevision) {
            return false;
        }
        selectedOccurrence =
                occurrence.filter(current.occurrenceObjects()::containsKey).orElse(null);
        restoreSelection();
        return selectedOccurrence != null || occurrence.isEmpty();
    }

    Optional<SceneTransformEvent> synchronizeSelection(
            long expectedRevision, Optional<CompositionOccurrenceId> occurrence) {
        requireOpen();
        Objects.requireNonNull(occurrence, "occurrence");
        SceneViewRealizedContent current = content;
        if (current == null || current.revision() != expectedRevision) {
            return Optional.empty();
        }
        CompositionOccurrenceId replacement =
                occurrence.filter(current.occurrenceObjects()::containsKey).orElse(null);
        if (Objects.equals(selectedOccurrence, replacement)) {
            restoreSelection();
            return Optional.empty();
        }
        Optional<SceneTransformEvent> cancelled = cancelTransform();
        selectedOccurrence = replacement;
        restoreSelection();
        return cancelled;
    }

    void navigate(SceneViewNavigation navigation) {
        requireOpen();
        switch (Objects.requireNonNull(navigation, "navigation")) {
            case SceneViewNavigation.Orbit orbit ->
                editorCamera.orbit(orbit.horizontal(), orbit.vertical(), orbit.viewportWidth(), orbit.viewportHeight());
            case SceneViewNavigation.Pan pan ->
                editorCamera.pan(pan.horizontal(), pan.vertical(), pan.viewportWidth(), pan.viewportHeight());
            case SceneViewNavigation.Zoom zoom -> editorCamera.zoom(zoom.amount());
            case SceneViewNavigation.FocusSelected ignored -> focusSelected();
            case SceneViewNavigation.FrameAll ignored -> frameAll();
            case SceneViewNavigation.ToggleProjection ignored -> editorCamera.toggleProjection();
            case SceneViewNavigation.GizmoClick click ->
                orientationGizmo
                        .hitTest(click.horizontal(), click.vertical(), click.viewportWidth(), click.viewportHeight())
                        .ifPresent(this::applyGizmoHit);
        }
    }

    Optional<SceneTransformEvent> configureTransform(SceneTransformSettings settings) {
        requireOpen();
        Optional<SceneTransformEvent> cancelled = cancelTransform();
        transformGizmo.configure(settings);
        return cancelled;
    }

    SceneTransformPointerOutcome transform(
            SceneTransformGizmo.PointerPhase phase, float horizontal, float vertical, int width, int height) {
        requireOpen();
        return transformGizmo.pointer(phase, horizontal, vertical, width, height);
    }

    Optional<SceneTransformEvent> cancelTransform() {
        requireOpen();
        return transformGizmo
                .pointer(SceneTransformGizmo.PointerPhase.CANCEL, 0.0f, 0.0f, 1, 1)
                .event();
    }

    SceneViewCameraState cameraState() {
        requireOpen();
        return editorCamera.state();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException failure = null;
        clearSelectionHelper();
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
            transformGizmo.close();
        } catch (RuntimeException exception) {
            failure = retainFailure(failure, exception);
        }
        try {
            orientationGizmo.close();
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

    private void restoreSelection() {
        clearSelectionHelper();
        SceneViewRealizedContent current = content;
        CompositionOccurrenceId occurrence = selectedOccurrence;
        if (current == null || occurrence == null) {
            transformGizmo.select(null, null, 0L, false);
            return;
        }
        Object3D selected = current.occurrenceObjects().get(occurrence);
        VisualOccurrence description = current.occurrenceDescriptions().get(occurrence);
        if (selected == null) {
            selectedOccurrence = null;
            transformGizmo.select(null, null, 0L, false);
            return;
        }
        boolean editable = description != null
                && description
                        .transform()
                        .map(transform -> transform.identity().scope().definition())
                        .filter(occurrence.rootDefinition()::equals)
                        .isPresent();
        boolean presentGizmo = current.permitsTransformGizmo(occurrence);
        transformGizmo.refreshSelection(
                presentGizmo ? description : null, presentGizmo ? selected : null, current.revision(), editable);
        try {
            BoxHelper helper = new BoxHelper(selected);
            scene.add(helper);
            selectionHelper = helper;
        } catch (IllegalArgumentException ignored) {
            // Non-spatial Entities remain selected without a visual bounds helper.
        }
    }

    private void focusSelected() {
        SceneViewRealizedContent current = content;
        CompositionOccurrenceId occurrence = selectedOccurrence;
        if (current == null || occurrence == null) {
            return;
        }
        Object3D selected = current.occurrenceObjects().get(occurrence);
        if (selected != null) {
            BoxHelper.computeVisibleBounds(selected).ifPresent(editorCamera::frame);
        }
    }

    private void frameAll() {
        SceneViewRealizedContent current = content;
        if (current != null) {
            BoxHelper.computeVisibleBounds(current.root()).ifPresent(editorCamera::frame);
        }
    }

    private void applyGizmoHit(SceneOrientationGizmo.Hit hit) {
        switch (hit) {
            case SceneOrientationGizmo.Hit.Center ignored -> editorCamera.toggleProjection();
            case SceneOrientationGizmo.Hit.Axis axis -> editorCamera.align(axis.axis(), axis.direction());
        }
    }

    private void clearSelectionHelper() {
        BoxHelper helper = selectionHelper;
        selectionHelper = null;
        if (helper != null) {
            helper.detach();
            helper.close();
        }
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
}
