/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Renderer-owned screen-space transform overlay and continuous drag calculator. */
final class SceneTransformGizmo implements Overlay, AutoCloseable {
    private final SceneTransformGizmoLayout layout;
    private final Supplier<? extends Camera> camera;
    private final SceneTransformGizmoRenderer renderer = new SceneTransformGizmoRenderer();
    private SceneTransformSettings settings = SceneTransformSettings.defaults();
    private @Nullable SceneTransformSelection selection;
    private @Nullable SceneTransformGizmoState state;
    private @Nullable Handle hovered;
    private @Nullable SceneTransformDrag drag;
    private boolean closed;

    SceneTransformGizmo(Supplier<? extends Camera> camera) {
        this.camera = Objects.requireNonNull(camera, "camera");
        layout = new SceneTransformGizmoLayout(camera);
    }

    void configure(SceneTransformSettings replacement) {
        requireOpen();
        settings = Objects.requireNonNull(replacement, "replacement");
        hovered = null;
        if (settings.tool() == SceneTransformTool.SELECT) {
            drag = null;
        }
    }

    void select(@Nullable VisualOccurrence occurrence, @Nullable Object3D object, long revision, boolean editable) {
        requireOpen();
        drag = null;
        hovered = null;
        selection = createSelection(occurrence, object, revision, editable);
    }

    void refreshSelection(
            @Nullable VisualOccurrence occurrence, @Nullable Object3D object, long revision, boolean editable) {
        requireOpen();
        SceneTransformSelection replacement = createSelection(occurrence, object, revision, editable);
        if (replacement == null
                || selection == null
                || !replacement
                        .occurrence()
                        .occurrence()
                        .equals(selection.occurrence().occurrence())) {
            drag = null;
            hovered = null;
        }
        selection = replacement;
    }

    SceneTransformPointerOutcome pointer(PointerPhase phase, float horizontal, float vertical, int width, int height) {
        requireOpen();
        if (width <= 0 || height <= 0 || !Float.isFinite(horizontal) || !Float.isFinite(vertical)) {
            throw new IllegalArgumentException("transform pointer arguments are invalid");
        }
        SceneTransformGizmoState current = renderState(width, height).orElse(null);
        state = current;
        return switch (phase) {
            case HOVER -> hover(horizontal, vertical, current);
            case BEGIN -> begin(horizontal, vertical, width, height, current);
            case UPDATE -> update(horizontal, vertical);
            case COMMIT -> finish(SceneTransformEvent.Phase.COMMIT);
            case CANCEL -> finish(SceneTransformEvent.Phase.CANCEL);
        };
    }

    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        requireOpen();
        Objects.requireNonNull(canvas, "canvas");
        SceneTransformGizmoState current = renderState(width, height).orElse(null);
        state = current;
        if (current == null) {
            return;
        }
        SceneTransformSelection selected = Objects.requireNonNull(selection, "selection");
        renderer.paint(canvas, current, hovered, drag == null ? null : drag.handle(), selected.editable());
    }

    /** Builds immutable screen-space state for testing and renderer-owned hit testing. */
    Optional<SceneTransformGizmoState> renderState(int width, int height) {
        requireOpen();
        SceneTransformSelection selected = selection;
        return selected == null ? Optional.empty() : layout.create(selected.object(), settings, width, height);
    }

    boolean dragging() {
        return drag != null;
    }

    @Override
    public void close() {
        closed = true;
        selection = null;
        state = null;
        hovered = null;
        drag = null;
    }

    private SceneTransformPointerOutcome hover(float x, float y, @Nullable SceneTransformGizmoState current) {
        hovered = current == null ? null : current.hit(x, y).orElse(null);
        return SceneTransformPointerOutcome.unclaimed();
    }

    private SceneTransformPointerOutcome begin(
            float x, float y, int width, int height, @Nullable SceneTransformGizmoState current) {
        SceneTransformSelection selected = selection;
        if (selected == null || current == null || settings.tool() == SceneTransformTool.SELECT) {
            return SceneTransformPointerOutcome.unclaimed();
        }
        Optional<Handle> hit = current.hit(x, y);
        if (hit.isEmpty()) {
            return SceneTransformPointerOutcome.unclaimed();
        }
        Handle handle = hit.orElseThrow();
        hovered = handle;
        String editId = UUID.randomUUID().toString();
        Vector3f worldOrigin = selected.object().worldPosition(new Vector3f());
        SceneTransformPlaneDrag plane = handle.axes().size() == 2
                ? SceneTransformPlaneDrag.begin(
                                camera.get(),
                                current,
                                handle,
                                worldOrigin,
                                new SceneTransformGizmoState.Point(x, y),
                                new RendererSession.SurfaceSize(width, height))
                        .orElse(null)
                : null;
        if (handle.axes().size() == 2 && plane == null) {
            return SceneTransformPointerOutcome.unclaimed();
        }
        SceneTransformDrag started = new SceneTransformDrag(
                editId, selected, settings, handle, new SceneTransformGizmoState.Point(x, y), current, plane);
        if (selected.editable()) {
            drag = started;
            return SceneTransformPointerOutcome.claimed(
                    Optional.of(event(SceneTransformEvent.Phase.BEGIN, started, started.initialValue())));
        }
        return SceneTransformPointerOutcome.claimed(
                Optional.of(event(SceneTransformEvent.Phase.BLOCKED, started, started.initialValue())));
    }

    private static @Nullable SceneTransformSelection createSelection(
            @Nullable VisualOccurrence occurrence, @Nullable Object3D object, long revision, boolean editable) {
        if (occurrence == null || object == null) {
            return null;
        }
        Transform3d transform = occurrence.transform().orElse(null);
        return transform == null
                ? null
                : new SceneTransformSelection(occurrence, object, transform, revision, editable);
    }

    private SceneTransformPointerOutcome update(float x, float y) {
        SceneTransformDrag active = drag;
        if (active == null) {
            return SceneTransformPointerOutcome.unclaimed();
        }
        Optional<Vector3> value = active.update(x, y);
        if (value.isEmpty()) {
            return SceneTransformPointerOutcome.claimed(Optional.empty());
        }
        Vector3 replacement = value.orElseThrow();
        active.apply(replacement);
        return SceneTransformPointerOutcome.claimed(
                Optional.of(event(SceneTransformEvent.Phase.UPDATE, active, replacement)));
    }

    private SceneTransformPointerOutcome finish(SceneTransformEvent.Phase phase) {
        SceneTransformDrag active = drag;
        drag = null;
        if (active == null) {
            return SceneTransformPointerOutcome.unclaimed();
        }
        Vector3 value = phase == SceneTransformEvent.Phase.CANCEL ? active.initialValue() : active.currentValue();
        if (phase == SceneTransformEvent.Phase.CANCEL) {
            active.apply(value);
        }
        return SceneTransformPointerOutcome.claimed(Optional.of(event(phase, active, value)));
    }

    private static SceneTransformEvent event(
            SceneTransformEvent.Phase phase, SceneTransformDrag active, Vector3 value) {
        SceneTransformSelection selected = active.selection();
        return new SceneTransformEvent(
                phase, active.editId(), selected.transform().identity(), active.property(), value, selected.revision());
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene transform gizmo is closed");
        }
    }

    enum PointerPhase {
        HOVER,
        BEGIN,
        UPDATE,
        COMMIT,
        CANCEL
    }
}
